/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.newpipe.app.search.SearchRepository
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.history.HistoryRepository
import net.newpipe.app.subscription.SubscriptionRepository
import org.koin.core.annotation.KoinViewModel

import net.newpipe.app.download.DownloadManager

@OptIn(FlowPreview::class)
@KoinViewModel
class SearchViewModel(
    private val repo: SearchRepository,
    private val historyRepo: HistoryRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val query: MutableStateFlow<String> = MutableStateFlow("")
    val results: StateFlow<List<SearchResultItem>> get() = _results
    val isLoading: StateFlow<Boolean> get() = _isLoading
    val error: StateFlow<String?> get() = _error

    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    init {
        // Debounce de 500ms para no lanzar una búsqueda por cada keystroke.
        // distinctUntilChanged evita relanzar si el user borra y re-escribe lo mismo.
        query
            .debounce(500)
            .distinctUntilChanged()
            .onEach { q ->
                if (q.trim().length >= 2) {
                    performSearch(q)
                } else if (q.isEmpty()) {
                    loadFeed()
                } else {
                    _results.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
            
        // Cargar el feed inicial
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            if (_isLoading.value) return@launch
            _isLoading.value = true
            _error.value = null
            try {
                // 1. Priorizar videos de un canal suscrito aleatorio (si hay)
                val subs = subscriptionRepo.subscriptions.value
                if (subs.isNotEmpty()) {
                    val randomSub = subs.random()
                    val channelVideos = repo.getChannelVideos(randomSub.url)
                    if (channelVideos.isNotEmpty()) {
                        _results.value = channelVideos
                        return@launch
                    }
                }

                // 2. Si no hay suscripciones o fallan, intentar con relacionados del historial
                val history = historyRepo.history.value
                if (history.isNotEmpty()) {
                    val lastViewedUrl = history.first().streamUrl
                    val details = repo.resolveStreamDetails(lastViewedUrl)
                    val related = details?.relatedItems ?: emptyList()
                    if (related.isNotEmpty()) {
                        _results.value = related
                        return@launch
                    }
                }

                // 3. Fallback a tendencias generales
                _results.value = repo.getTrending()
                if (_results.value.isEmpty()) {
                    _error.value = "No se pudieron cargar recomendaciones."
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", e) { "Error al cargar el feed" }
                _error.value = "Error al cargar sugerencias"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun performSearch(q: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _results.value = repo.search(q)
                if (_results.value.isEmpty()) {
                    _error.value = "Sin resultados para \"$q\""
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", e) { "Error buscando: $q" }
                _error.value = "Error al buscar. ¿Hay conexión?"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Resuelve la URL de stream de un item y se la pasa de vuelta al caller */
    fun resolveAndPlay(item: SearchResultItem, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val details = repo.resolveStreamDetails(item.streamUrl)
                if (details != null) {
                    onResolved(details.directUrl)
                } else {
                    _error.value = "No se encontró stream reproducible para \"${item.title}\""
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", e) { "Error resolviendo stream: ${item.streamUrl}" }
                _error.value = "No se pudo obtener el stream"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Encola una descarga resolviendo primero la URL del stream real */
    fun enqueueDownload(item: SearchResultItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val details = repo.resolveStreamDetails(item.streamUrl)
                if (details != null) {
                    downloadManager.enqueue(details.directUrl, item.title)
                } else {
                    _error.value = "No se encontró stream descargable para \"${item.title}\""
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", e) { "Error iniciando descarga: ${item.streamUrl}" }
                _error.value = "Error al iniciar descarga"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
