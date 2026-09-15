/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.screen.home.VideoGridItem
import net.newpipe.app.subscription.SubscriptionRepository
import net.newpipe.app.viewmodel.feed.FeedViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    navigator: Navigator = koinInject(),
    repository: SubscriptionRepository = koinInject(),
    feedViewModel: FeedViewModel = koinViewModel()
) {
    val subscriptions by repository.subscriptions.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Suscripciones",
                onNavigateUp = { navigator.navigateUp() }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Novedades") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Canales") }
                )
            }

            if (selectedTab == 0) {
                FeedTab(navigator, feedViewModel)
            } else {
                ChannelsTab(navigator, subscriptions)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FeedTab(navigator: Navigator, viewModel: FeedViewModel) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
        } else if (videos.isEmpty()) {
            Text(
                text = "No hay novedades.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        onDownload = {},
                        onUploaderClick = {
                            if (video.uploaderUrl != null) {
                                navigator.navigateTo(Destination.Channel(video.uploaderUrl, video.uploaderName))
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ChannelsTab(navigator: Navigator, subscriptions: List<net.newpipe.app.subscription.SubscriptionItem>) {
    if (subscriptions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tienes suscripciones aún.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(subscriptions, key = { it.url }) { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator.navigateTo(Destination.Channel(sub.url, sub.name))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
