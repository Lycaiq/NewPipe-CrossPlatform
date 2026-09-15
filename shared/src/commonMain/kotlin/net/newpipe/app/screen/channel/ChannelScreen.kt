/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.channel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.screen.home.VideoGridItem
import net.newpipe.app.viewmodel.channel.ChannelViewModel
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.ic_favorite
import newpipe.shared.generated.resources.ic_favorite_border
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChannelScreen(
    destination: Destination.Channel,
    navigator: Navigator = koinInject(),
    viewModel: ChannelViewModel = koinViewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()

    LaunchedEffect(destination.url) {
        viewModel.loadChannel(destination.url, destination.name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = destination.name,
                onNavigateUp = { navigator.navigateUp() },
                actions = {
                    IconButton(onClick = { viewModel.toggleSubscription() }) {
                        val favIcon = if (isSubscribed) Res.drawable.ic_favorite else Res.drawable.ic_favorite_border
                        Icon(
                            painter = painterResource(favIcon),
                            contentDescription = if (isSubscribed) "Desuscribirse" else "Suscribirse",
                            tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            } else if (videos.isEmpty()) {
                Text("No hay videos en este canal")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(videos, key = { it.streamUrl }) { video ->
                        VideoGridItem(
                            item = video,
                            onClick = {
                                navigator.navigateTo(
                                    Destination.Player(
                                        streamUrl = video.streamUrl,
                                        title = video.title,
                                        uploaderName = video.uploaderName,
                                        duration = video.duration,
                                        thumbnailUrl = video.thumbnailUrl ?: "",
                                        viewCount = video.viewCount
                                    )
                                )
                            },
                            onDownload = {}
                        )
                    }
                }
            }
        }
    }
}
