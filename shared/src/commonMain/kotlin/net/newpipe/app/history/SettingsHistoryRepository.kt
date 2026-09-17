/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.history

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

@Singleton(binds = [HistoryRepository::class])
class SettingsHistoryRepository(
    private val settings: Settings
) : HistoryRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val HISTORY_KEY = "history_videos_v1"

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    override val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val storedJson = settings.getStringOrNull(HISTORY_KEY)
        if (storedJson != null) {
            try {
                val list = json.decodeFromString<List<HistoryItem>>(storedJson)
                _history.value = list.sortedByDescending { it.lastAccessTimeMs }
            } catch (e: Exception) {
                // Evitamos un crash si el esquema cambia
                _history.value = emptyList()
            }
        }
    }

    private fun persist() {
        val list = _history.value
        settings[HISTORY_KEY] = json.encodeToString(list)
    }

    override fun addToHistory(item: HistoryItem) {
        _history.update { current ->
            // Filtramos si ya existe, lo quitamos para volver a meterlo al inicio (bump)
            val filtered = current.filterNot { it.streamUrl == item.streamUrl }
            // Mantener un límite razonable en memoria/settings (ej. 100 items)
            val updatedList = (listOf(item) + filtered).take(100)
            updatedList
        }
        persist()
    }

    override fun clearHistory() {
        _history.value = emptyList()
        persist()
    }

    override fun removeHistoryItem(streamUrl: String) {
        _history.update { current ->
            current.filterNot { it.streamUrl == streamUrl }
        }
        persist()
    }

    override fun reload() {
        loadHistory()
    }
}
