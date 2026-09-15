/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

enum class DownloadState {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, ERROR, CANCELLED
}

data class DownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val progress: Float = 0f, // 0.0 a 1.0
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val state: DownloadState = DownloadState.QUEUED,
    val priority: Int = 0,
    val errorMessage: String? = null
)
