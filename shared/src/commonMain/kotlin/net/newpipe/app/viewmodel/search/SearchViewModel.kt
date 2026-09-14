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
import org.koin.core.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

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
            .filter { it.length >= 2 }  // mínimo 2 caracteres para no spamear el extractor
            .onEach { performSearch(it) }
            .launchIn(viewModelScope)
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
                val streamUrl = repo.resolveStreamUrl(item.streamUrl)
                if (streamUrl != null) {
                    onResolved(streamUrl)
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
}
