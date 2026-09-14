/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.koin.compose.koinInject

/**
 * Composable que pega el reproductor nativo de VLC dentro de la ventana de Compose.
 *
 * SwingPanel es el puente oficial entre el mundo AWT/Swing de VLC y el mundo
 * de Compose Desktop. Internamente Compose crea una ventana AWT superpuesta
 * sobre la región que ocupa este composable — por eso no podés dibujar nada
 * de Compose encima de esta área (limitación conocida de Compose Desktop).
 * Los controles del player tienen que ir FUERA de este composable.
 *
 * @param url URL del stream a reproducir. Si cambia, hace stop + play automático.
 * @param modifier Modifier estándar de Compose para tamaño/posición.
 * @param player El player inyectado por Koin, ya inicializado.
 */
@Composable
fun VideoSurface(
    url: String,
    modifier: Modifier = Modifier,
    player: JVMVideoPlayer = koinInject()
) {
    val component = remember { player.getComponent() }

    DisposableEffect(url) {
        // Cada vez que url cambia, paramos lo anterior y arrancamos lo nuevo.
        // Si url es vacío, solo paramos (por ejemplo al navegar fuera del player).
        if (url.isNotBlank()) {
            player.play(url)
        } else {
            player.stop()
        }

        onDispose {
            // Cuando el composable sale del árbol (ej: navegar back), paramos limpiamente.
            // NO llamamos release() aquí porque el player vive en el scope de Koin
            // y puede ser reutilizado. release() solo se llama al cerrar la app.
            player.stop()
        }
    }

    if (component != null) {
        SwingPanel(
            modifier = modifier,
            factory = { component }
        )
    }
}
