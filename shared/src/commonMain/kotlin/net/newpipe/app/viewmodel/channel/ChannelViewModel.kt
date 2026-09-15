/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.newpipe.app.search.SearchRepository
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.subscription.SubscriptionItem
import net.newpipe.app.subscription.SubscriptionRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ChannelViewModel(
    private val repo: SearchRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val videos: StateFlow<List<SearchResultItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentChannelUrl: String = ""
    private var currentChannelName: String = ""

    val isSubscribed = subscriptionRepository.subscriptions.map { list ->
        list.any { it.url == currentChannelUrl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadChannel(url: String, name: String) {
        if (currentChannelUrl == url) return
        currentChannelUrl = url
        currentChannelName = name

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _videos.value = repo.getChannelVideos(url)
            } catch (e: Exception) {
                _error.value = "Error al cargar canal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSubscription() {
        if (currentChannelUrl.isBlank()) return
        if (isSubscribed.value) {
            subscriptionRepository.removeSubscription(currentChannelUrl)
        } else {
            subscriptionRepository.addSubscription(
                SubscriptionItem(
                    url = currentChannelUrl,
                    name = currentChannelName,
                    avatarUrl = null // No lo extraemos por ahora
                )
            )
        }
    }
}
