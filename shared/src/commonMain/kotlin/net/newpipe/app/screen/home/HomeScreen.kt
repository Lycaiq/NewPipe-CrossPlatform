/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.viewmodel.search.SearchViewModel
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.ic_favorite
import newpipe.shared.generated.resources.ic_history

import newpipe.shared.generated.resources.ic_cloud_download
import newpipe.shared.generated.resources.ic_file_download
import newpipe.shared.generated.resources.ic_search
import newpipe.shared.generated.resources.search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),
    viewModel: SearchViewModel = koinViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    HomeScreenContent(
        query = query,
        onQueryChange = { viewModel.query.value = it },
        results = results,
        isLoading = isLoading,
        error = error,
        onItemClick = { item ->
            viewModel.resolveAndPlay(item) { streamUrl ->
                navigator.navigateTo(Destination.Player(
                    streamUrl = streamUrl, 
                    title = item.title, 
                    uploaderName = item.uploaderName,
                    duration = item.duration,
                    thumbnailUrl = item.thumbnailUrl ?: "",
                    viewCount = item.viewCount
                ))
            }
        },
        onDownloadsClick = {
            navigator.navigateTo(Destination.Downloads)
        },
        onBookmarksClick = {
            navigator.navigateTo(Destination.Bookmarks)
        },
        onHistoryClick = {
            navigator.navigateTo(Destination.History)
        },
        onDownloadClick = { item ->
            viewModel.enqueueDownload(item)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    results: List<SearchResultItem> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onItemClick: (SearchResultItem) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onDownloadClick: (SearchResultItem) -> Unit = {}
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    modifier = Modifier.weight(1f),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSearch = onQueryChange,
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text(stringResource(Res.string.search)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_search),
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = {}
                ) {}
                
                Spacer(Modifier.width(8.dp))
                
                androidx.compose.material3.IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_history),
                        contentDescription = "Historial"
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = onBookmarksClick,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_favorite),
                        contentDescription = "Favoritos"
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = onDownloadsClick,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_file_download),
                        contentDescription = "Descargas"
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    isLoading && results.isEmpty() -> CircularProgressIndicator(modifier = Modifier.padding(top = 48.dp))
                    error != null && results.isEmpty() -> Text(
                        text = error,
                        modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> ResultsGrid(
                        results = results, 
                        onItemClick = onItemClick,
                        onDownloadClick = onDownloadClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsGrid(
    results: List<SearchResultItem>,
    onItemClick: (SearchResultItem) -> Unit,
    onDownloadClick: (SearchResultItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = results, key = { it.streamUrl }) { item ->
            VideoGridItem(
                item = item, 
                onClick = { onItemClick(item) },
                onDownload = { onDownloadClick(item) }
            )
        }
    }
}

@Composable
fun VideoGridItem(
    item: SearchResultItem,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = "Thumbnail for ${item.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.uploaderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                androidx.compose.material3.IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cloud_download),
                        contentDescription = "Descargar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
