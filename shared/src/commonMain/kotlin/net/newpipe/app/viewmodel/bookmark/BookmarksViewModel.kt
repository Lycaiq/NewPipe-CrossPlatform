/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.bookmark

import androidx.lifecycle.ViewModel
import net.newpipe.app.bookmark.BookmarkRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class BookmarksViewModel(
    bookmarkRepository: BookmarkRepository
) : ViewModel() {
    val bookmarks = bookmarkRepository.bookmarks
}
