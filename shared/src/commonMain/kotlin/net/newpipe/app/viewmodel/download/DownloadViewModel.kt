/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.download

import androidx.lifecycle.ViewModel
import net.newpipe.app.download.DownloadManager
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DownloadViewModel(
    private val downloadManager: DownloadManager
) : ViewModel() {

    val downloads = downloadManager.downloads

    fun pauseDownload(id: String) {
        downloadManager.pause(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resume(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancel(id)
    }
}
