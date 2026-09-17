/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.bookmark

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

@Singleton(binds = [BookmarkRepository::class])
class SettingsBookmarkRepository(
    private val settings: Settings
) : BookmarkRepository {

    private val KEY_BOOKMARKS = "saved_videos_list"
    
    // JSON laxo para evitar problemas si añadimos propiedades en el futuro
    private val json = Json { ignoreUnknownKeys = true }

    private val _bookmarks = MutableStateFlow<List<SavedVideo>>(emptyList())
    override val bookmarks: StateFlow<List<SavedVideo>> = _bookmarks

    init {
        loadFromSettings()
    }

    private fun loadFromSettings() {
        val jsonString = settings.getStringOrNull(KEY_BOOKMARKS)
        if (jsonString != null) {
            try {
                val list = json.decodeFromString<List<SavedVideo>>(jsonString)
                // Ordenar del más reciente al más antiguo
                _bookmarks.value = list.sortedByDescending { it.savedAtMs }
            } catch (e: Exception) {
                // Si la data está corrupta, arrancamos limpios
                _bookmarks.value = emptyList()
            }
        }
    }

    private fun saveToSettings(list: List<SavedVideo>) {
        try {
            val jsonString = json.encodeToString(list)
            settings.putString(KEY_BOOKMARKS, jsonString)
        } catch (e: Exception) {
            // Log error
        }
    }

    override fun isBookmarked(streamUrl: String): Boolean {
        return _bookmarks.value.any { it.streamUrl == streamUrl }
    }

    override fun toggleBookmark(video: SavedVideo) {
        _bookmarks.update { currentList ->
            val exists = currentList.any { it.streamUrl == video.streamUrl }
            val newList = if (exists) {
                currentList.filter { it.streamUrl != video.streamUrl }
            } else {
                currentList + video.copy(savedAtMs = net.newpipe.app.utils.currentTimeMillis())
            }
            saveToSettings(newList)
            newList.sortedByDescending { it.savedAtMs }
        }
    }

    override fun reload() {
        loadFromSettings()
    }
}
