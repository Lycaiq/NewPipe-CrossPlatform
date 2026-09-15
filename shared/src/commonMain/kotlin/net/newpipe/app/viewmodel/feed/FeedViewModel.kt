/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.newpipe.app.search.SearchRepository
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.subscription.SubscriptionRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FeedViewModel(
    private val repo: SearchRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val videos: StateFlow<List<SearchResultItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadFeed() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val subs = subscriptionRepository.subscriptions.value
                if (subs.isEmpty()) {
                    _videos.value = emptyList()
                    return@launch
                }

                // Cargar canales en paralelo
                val allVideos = subs.map { sub ->
                    async {
                        try {
                            repo.getChannelVideos(sub.url)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()

                // Ordenar por fecha descendente (los más nuevos primero)
                // Si no tienen fecha, los dejamos al final.
                val sorted = allVideos.sortedByDescending { it.uploadDateMs ?: 0L }
                
                // Mostrar solo los últimos 50 para no reventar la memoria
                _videos.value = sorted.take(50)

            } catch (e: Exception) {
                _error.value = "Error al cargar novedades"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
