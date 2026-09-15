/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.history

import androidx.lifecycle.ViewModel
import net.newpipe.app.history.HistoryRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val history = historyRepository.history

    fun clearHistory() {
        historyRepository.clearHistory()
    }
}
