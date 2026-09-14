/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.viewmodel.search.SearchViewModel
import newpipe.shared.generated.resources.Res
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
                navigator.navigateTo(Destination.Player(streamUrl = streamUrl, title = item.title))
            }
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
    onItemClick: (SearchResultItem) -> Unit = {}
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.padding(top = 48.dp))
                    error != null && results.isEmpty() -> Text(
                        text = error,
                        modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> ResultsList(results = results, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: List<SearchResultItem>,
    onItemClick: (SearchResultItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = results, key = { it.streamUrl }) { item ->
            ResultItem(item = item, onClick = { onItemClick(item) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ResultItem(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Duración badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = item.duration,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = item.uploaderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
