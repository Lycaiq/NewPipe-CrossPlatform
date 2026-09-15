/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.history

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val streamUrl: String,
    val title: String,
    val uploaderName: String,
    val duration: String,
    val thumbnailUrl: String,
    val viewCount: Long,
    val lastAccessTimeMs: Long
)

interface HistoryRepository {
    val history: StateFlow<List<HistoryItem>>
    fun addToHistory(item: HistoryItem)
    fun clearHistory()
    fun removeHistoryItem(streamUrl: String)
}
