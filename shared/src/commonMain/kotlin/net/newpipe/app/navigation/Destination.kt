/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import net.newpipe.app.model.License

/**
 * Destinations for navigation in compose
 */
@Serializable
sealed interface Destination : NavKey {

    @Serializable
    data object Home : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object About : Destination

    /**
     * Player — lleva la URL del stream ya resuelta, lista para pasársela a VLC.
     * Usamos un data class y no data object porque necesitamos pasar la URL.
     */
    @Serializable
    data class Player(val streamUrl: String, val title: String = "") : Destination

    @Serializable
    data object Downloads : Destination
}
