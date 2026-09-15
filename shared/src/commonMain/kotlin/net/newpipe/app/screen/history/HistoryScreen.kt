/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.screen.home.VideoGridItem
import net.newpipe.app.viewmodel.history.HistoryViewModel
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.ic_delete
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    navigator: Navigator = koinInject(),
    viewModel: HistoryViewModel = koinViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Historial",
                onNavigateUp = { navigator.navigateUp() },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(painter = painterResource(Res.drawable.ic_delete), contentDescription = "Limpiar Historial")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No has visto ningún video.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(history, key = { it.streamUrl }) { video ->
                    val mappedItem = SearchResultItem(
                        title = video.title,
                        uploaderName = video.uploaderName,
                        duration = video.duration,
                        thumbnailUrl = video.thumbnailUrl,
                        streamUrl = video.streamUrl,
                        viewCount = video.viewCount
                    )
                    
                    VideoGridItem(
                        item = mappedItem,
                        onClick = {
                            navigator.navigateTo(
                                Destination.Player(
                                    streamUrl = video.streamUrl,
                                    title = video.title,
                                    uploaderName = video.uploaderName,
                                    duration = video.duration,
                                    thumbnailUrl = video.thumbnailUrl,
                                    viewCount = video.viewCount
                                )
                            )
                        },
                        onDownload = {
                            // En fase posterior: conectar con JVMDownloadManager
                        }
                    )
                }
            }
        }
    }
}
