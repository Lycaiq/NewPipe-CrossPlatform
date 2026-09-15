/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.bookmark

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SavedVideo(
    val title: String,
    val uploaderName: String,
    val duration: String,
    val thumbnailUrl: String?,
    val streamUrl: String,
    val viewCount: Long = 0L,
    val savedAtMs: Long
)

interface BookmarkRepository {
    val bookmarks: StateFlow<List<SavedVideo>>
    
    fun isBookmarked(streamUrl: String): Boolean
    fun toggleBookmark(video: SavedVideo)
}
