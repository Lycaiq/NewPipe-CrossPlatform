/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.subscription

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionItem(
    val url: String,
    val name: String,
    val avatarUrl: String? = null
)

interface SubscriptionRepository {
    val subscriptions: StateFlow<List<SubscriptionItem>>
    fun addSubscription(item: SubscriptionItem)
    fun removeSubscription(url: String)
    fun isSubscribed(url: String): Boolean
    fun reload()
}
