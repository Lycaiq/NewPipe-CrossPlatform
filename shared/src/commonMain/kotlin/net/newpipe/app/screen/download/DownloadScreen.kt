/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.download.DownloadState
import net.newpipe.app.download.DownloadTask
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.viewmodel.download.DownloadViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadScreen(
    destination: Destination.Downloads,
    navigator: Navigator = koinInject(),
    viewModel: DownloadViewModel = koinViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Descargas",
                onNavigateUp = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (downloads.isEmpty()) {
                item {
                    Text(
                        "No hay descargas activas.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            items(downloads, key = { it.id }) { task ->
                DownloadItem(
                    task = task,
                    onPause = { viewModel.pauseDownload(task.id) },
                    onResume = { viewModel.resumeDownload(task.id) },
                    onCancel = { viewModel.cancelDownload(task.id) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DownloadItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            
            val statusText = when (task.state) {
                DownloadState.QUEUED -> "En cola"
                DownloadState.DOWNLOADING -> "Descargando ${(task.progress * 100).toInt()}%"
                DownloadState.PAUSED -> "Pausado"
                DownloadState.COMPLETED -> "Completado"
                DownloadState.ERROR -> "Error: ${task.errorMessage}"
                DownloadState.CANCELLED -> "Cancelado"
            }
            Text(statusText, style = MaterialTheme.typography.bodySmall)

            if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PAUSED) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            Row(modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                if (task.state == DownloadState.DOWNLOADING) {
                    Button(onClick = onPause) { Text("Pausar") }
                } else if (task.state == DownloadState.PAUSED) {
                    Button(onClick = onResume) { Text("Reanudar") }
                }
                
                if (task.state == DownloadState.QUEUED || task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PAUSED) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onCancel) { Text("Cancelar") }
                }
            }
        }
    }
}
