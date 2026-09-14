/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import net.newpipe.app.extractor.JVMDownloader
import net.newpipe.app.screen.player.FullscreenController
import net.newpipe.app.screen.player.LocalFullscreenController
import org.schabi.newpipe.extractor.NewPipe

/**
 * Entry point para Desktop (JVM).
 * Inicializamos el extractor antes de levantar la ventana — si esto falla
 * no tiene sentido mostrar la UI, así que dejamos que explote limpiamente.
 */
fun main() = application {
    // El extractor necesita un downloader inicializado antes del primer uso.
    // Lo hacemos aquí y no en un ViewModel para que sea síncrono y garantizado.
    NewPipe.init(JVMDownloader.getInstance())

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { JVMDownloader.getInstance().client }))
            }
            .build()
    }

    val windowState = rememberWindowState()
    
    val fullscreenController = remember {
        object : FullscreenController {
            override val isFullscreen: Boolean 
                get() = windowState.placement == WindowPlacement.Fullscreen
            
            override fun toggleFullscreen() {
                windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Fullscreen
                }
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication, 
        title = "NewPipe",
        state = windowState
    ) {
        CompositionLocalProvider(LocalFullscreenController provides fullscreenController) {
            App(onCloseRequest = ::exitApplication)
        }
    }
}
