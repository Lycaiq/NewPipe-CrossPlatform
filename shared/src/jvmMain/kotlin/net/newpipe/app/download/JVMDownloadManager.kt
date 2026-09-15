/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Singleton
import java.io.File
import java.util.UUID

@Singleton(binds = [DownloadManager::class])
class JVMDownloadManager(
    private val client: OkHttpClient = OkHttpClient()
) : DownloadManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    override val downloads: StateFlow<List<DownloadTask>> = _downloads

    private val activeJobs = mutableMapOf<String, Job>()

    // Concurrencia máxima
    private val maxConcurrentDownloads = 3

    init {
        // Dispatcher loop para procesar la cola por prioridad
        scope.launch {
            while (isActive) {
                var current = _downloads.value
                var downloadingCount = current.count { it.state == DownloadState.DOWNLOADING }
                
                while (downloadingCount < maxConcurrentDownloads) {
                    val nextToDownload = current
                        .filter { it.state == DownloadState.QUEUED }
                        .maxByOrNull { it.priority }
                    
                    if (nextToDownload != null) {
                        startDownload(nextToDownload)
                        // Refrescar el estado para la siguiente iteración del while
                        current = _downloads.value
                        downloadingCount = current.count { it.state == DownloadState.DOWNLOADING }
                    } else {
                        break
                    }
                }
                delay(100) // Polling ligero
            }
        }
    }

    override fun enqueue(url: String, title: String, priority: Int): String {
        val id = UUID.randomUUID().toString()
        val task = DownloadTask(id = id, title = title, url = url, priority = priority)
        _downloads.update { it + task }
        return id
    }

    override fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        updateTaskState(id, DownloadState.CANCELLED)
    }

    override fun pause(id: String) {
        // Pausar = cancelar por ahora. Soporte para Range-Header se haría aquí.
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        updateTaskState(id, DownloadState.PAUSED)
    }

    override fun resume(id: String) {
        val task = _downloads.value.find { it.id == id } ?: return
        if (task.state == DownloadState.PAUSED || task.state == DownloadState.ERROR) {
            updateTaskState(id, DownloadState.QUEUED)
        }
    }

    private fun updateTaskState(id: String, newState: DownloadState, errorMessage: String? = null) {
        _downloads.update { list ->
            list.map {
                if (it.id == id) it.copy(state = newState, errorMessage = errorMessage) else it
            }
        }
    }

    private fun updateProgress(id: String, bytesDownloaded: Long, totalBytes: Long) {
        _downloads.update { list ->
            list.map {
                if (it.id == id) {
                    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
                    it.copy(progress = progress, bytesDownloaded = bytesDownloaded, totalBytes = totalBytes)
                } else it
            }
        }
    }

    private fun startDownload(task: DownloadTask) {
        updateTaskState(task.id, DownloadState.DOWNLOADING)
        
        val job = scope.launch {
            try {
                // Carpeta de descargas simulada (relativa al directorio de ejecución)
                val downloadDir = File("downloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                
                val safeTitle = task.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val targetFile = File(downloadDir, "$safeTitle.mp4")
                
                val request = Request.Builder().url(task.url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Error HTTP ${response.code}")
                    
                    val body = response.body ?: throw Exception("Empty body")
                    val totalBytes = body.contentLength()
                    
                    var downloadedBytes = 0L
                    body.byteStream().use { input ->
                        targetFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (!isActive) {
                                    throw kotlinx.coroutines.CancellationException("Descarga cancelada")
                                }
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                
                                // Para no saturar el StateFlow, actualizamos con throttle natural (bloque 8KB)
                                // En producción real se hace un throttle por tiempo (ej. cada 200ms)
                                updateProgress(task.id, downloadedBytes, totalBytes)
                            }
                        }
                    }
                }
                updateTaskState(task.id, DownloadState.COMPLETED)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // No hacemos nada, el cancel() o pause() ya actualizó el estado
                targetFileForTask(task)?.delete()
            } catch (e: Exception) {
                Logger.e("JVMDownloadManager", e) { "Fallo en descarga ${task.id}" }
                updateTaskState(task.id, DownloadState.ERROR, e.message)
            } finally {
                activeJobs.remove(task.id)
            }
        }
        activeJobs[task.id] = job
    }
    
    private fun targetFileForTask(task: DownloadTask): File? {
        val downloadDir = File("downloads")
        val safeTitle = task.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        return File(downloadDir, "$safeTitle.mp4")
    }
}
