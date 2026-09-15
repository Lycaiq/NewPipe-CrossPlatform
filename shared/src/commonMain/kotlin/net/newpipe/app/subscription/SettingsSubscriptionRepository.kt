/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.subscription

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

@Singleton(binds = [SubscriptionRepository::class])
class SettingsSubscriptionRepository(
    private val settings: Settings
) : SubscriptionRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val SUBSCRIPTIONS_KEY = "subscriptions_v1"

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    override val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    init {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        val storedJson = settings.getStringOrNull(SUBSCRIPTIONS_KEY)
        if (storedJson != null) {
            try {
                val list = json.decodeFromString<List<SubscriptionItem>>(storedJson)
                _subscriptions.value = list
            } catch (e: Exception) {
                _subscriptions.value = emptyList()
            }
        }
    }

    private fun persist() {
        val list = _subscriptions.value
        settings[SUBSCRIPTIONS_KEY] = json.encodeToString(list)
    }

    override fun addSubscription(item: SubscriptionItem) {
        if (!isSubscribed(item.url)) {
            _subscriptions.update { it + item }
            persist()
        }
    }

    override fun removeSubscription(url: String) {
        _subscriptions.update { current ->
            current.filterNot { it.url == url }
        }
        persist()
    }

    override fun isSubscribed(url: String): Boolean {
        return _subscriptions.value.any { it.url == url }
    }
}
