/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.navigation

import androidx.compose.runtime.mutableStateListOf
import co.touchlab.kermit.Logger
import net.newpipe.app.screen.home.HomeScreen
import net.newpipe.app.screen.player.PlayerScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.plugin.module.dsl.single

/**
 * Navigation module to make navigation easier with nav3
 *
 * There is currently no annotation to handle this so we are using DSL API of Koin
 */
@OptIn(KoinExperimentalAPI::class)
fun navModule() = module {
    single<Navigator>()

    navigation<Destination.Home> {
        HomeScreen()
    }

    navigation<Destination.About> {
        net.newpipe.app.screen.about.AboutScreen()
    }

    navigation<Destination.Settings> {
        net.newpipe.app.screen.settings.SettingsHomeScreen()
    }

    // Player necesita acceso a la destination para sacar la URL y el título
    navigation<Destination.Player> { destination ->
        PlayerScreen(destination = destination)
    }

    navigation<Destination.Downloads> { destination ->
        net.newpipe.app.screen.download.DownloadScreen(destination = destination)
    }

    navigation<Destination.Bookmarks> {
        net.newpipe.app.screen.bookmark.BookmarksScreen()
    }
}

/**
 * Helper to navigate up and to different destinations in compose
 */
@Singleton
class Navigator(
    @Provided
    private val startDestination: Destination,

    @Provided
    private val onCloseRequest: () -> Unit
) {

    /**
     * Navigation backstack in compose
     */
    val backstack = mutableStateListOf(startDestination)

    /**
     * Navigates to the given destination
     */
    fun navigateTo(destination: Destination) = backstack.add(destination)

    /**
     * Navigates to the previous entry in the backstack
     */
    fun navigateUp() = when {
        backstack.size > 1 -> backstack.removeLastOrNull()

        else -> {
            // Si ya estamos en la última vista del stack, hacer "back" significa que el user 
            // quiere salir de la app, así que disparamos el onCloseRequest.
            Logger.i(messageString = "Cannot remove the only entry in backstack!")
            onCloseRequest()
        }
    }
}
