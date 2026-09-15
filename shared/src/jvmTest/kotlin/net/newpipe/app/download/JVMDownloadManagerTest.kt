/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JVMDownloadManagerTest {

    private val downloadsDir = File("downloads")

    @AfterTest
    fun tearDown() {
        if (downloadsDir.exists()) {
            downloadsDir.deleteRecursively()
        }
    }

    @Test
    fun `test 10 descargas concurrentes`() = runBlocking {
        // Interceptor que simula una respuesta instantanea de 100KB
        val mockClient = OkHttpClient.Builder().addInterceptor { chain ->
            val content = ByteArray(100 * 1024) { 1 } // 100KB
            val body = ResponseBody.create("video/mp4".toMediaType(), content)
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build()
        }.build()

        val manager = JVMDownloadManager(mockClient)
        val ids = (1..10).map { i ->
            manager.enqueue("http://fake.com/video$i", "Video$i")
        }

        // Wait until all are completed
        withTimeout(5000) {
            while (true) {
                val state = manager.downloads.first()
                if (state.count { it.state == DownloadState.COMPLETED } == 10) {
                    break
                }
                delay(100)
            }
        }

        val finalState = manager.downloads.value
        assertEquals(10, finalState.size)
        assertTrue(finalState.all { it.state == DownloadState.COMPLETED })
        
        // Verificar que los archivos existan
        for (i in 1..10) {
            assertTrue(File(downloadsDir, "Video$i.mp4").exists())
        }
    }

    @Test
    fun `test cancelacion a mitad de descarga`() = runBlocking {
        // Interceptor que simula una respuesta lenta para poder cancelar a medias
        val mockClient = OkHttpClient.Builder().addInterceptor { chain ->
            val source = object : Source {
                var bytesSent = 0L
                val totalBytes = 100_000L
                
                override fun close() {}
                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (bytesSent >= totalBytes) return -1
                    // Escribimos lento
                    Thread.sleep(100) 
                    val toWrite = minOf(byteCount, 10_000L)
                    val dummy = ByteArray(toWrite.toInt())
                    sink.write(dummy)
                    bytesSent += toWrite
                    return toWrite
                }
                override fun timeout(): Timeout = Timeout.NONE
            }
            
            val body = object : ResponseBody() {
                override fun contentLength() = 100_000L
                override fun contentType() = "video/mp4".toMediaType()
                override fun source(): BufferedSource = source.buffer()
            }
            
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build()
        }.build()

        val manager = JVMDownloadManager(mockClient)
        val id = manager.enqueue("http://fake.com/slow", "SlowVideo")

        // Esperar a que pase a DOWNLOADING
        withTimeout(5000) {
            while (true) {
                val task = manager.downloads.first().find { it.id == id }
                if (task?.state == DownloadState.DOWNLOADING && task.progress > 0) {
                    break
                }
                delay(50)
            }
        }

        manager.cancel(id)

        // Esperar a que el estado sea cancelado
        delay(300)
        
        val task = manager.downloads.value.find { it.id == id }
        assertEquals(DownloadState.CANCELLED, task?.state)
        
        // Verificar que el archivo fue borrado
        val file = File(downloadsDir, "SlowVideo.mp4")
        assertTrue(!file.exists(), "El archivo cancelado deberia eliminarse")
    }
}
