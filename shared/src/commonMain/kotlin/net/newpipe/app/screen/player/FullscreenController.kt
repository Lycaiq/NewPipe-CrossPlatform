/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.player

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Interfaz para abstraer el toggle de pantalla completa.
 * En Desktop se comunica con la Window, en Android ocultaría la status bar, etc.
 */
interface FullscreenController {
    val isFullscreen: Boolean
    fun toggleFullscreen()
}

/** 
 * Proveedor local para no tener que inyectar el controller por todo el árbol de Compose. 
 * El entry point de cada plataforma (Main.kt, MainActivity) lo provee.
 */
val LocalFullscreenController = staticCompositionLocalOf<FullscreenController> {
    object : FullscreenController {
        override val isFullscreen: Boolean = false
        override fun toggleFullscreen() {}
    }
}
