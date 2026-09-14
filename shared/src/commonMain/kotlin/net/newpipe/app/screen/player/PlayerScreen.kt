/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import org.koin.compose.koinInject

/**
 * Pantalla del reproductor.
 * La lógica de controles (Fase 3) va a vivir aquí también. Por ahora solo tenemos
 * la superficie de video y el botón de back — suficiente para validar el flujo completo.
 */
@Composable
fun PlayerScreen(
    destination: Destination.Player,
    navigator: Navigator = koinInject()
) {
    PlayerScreenContent(
        streamUrl = destination.streamUrl,
        title = destination.title,
        onNavigateUp = { navigator.navigateUp() }
    )
}

@Composable
fun PlayerScreenContent(
    streamUrl: String,
    title: String = "",
    onNavigateUp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title.ifBlank { "Reproduciendo" },
                onNavigateUp = onNavigateUp
            )
        }
    ) { _ ->
        // VideoSurface solo existe en desktopApp (necesita SwingPanel).
        // El expect/actual o la inyección de Koin se resuelven en Fase 3.
        // Por ahora usamos un placeholder que confirma que la navegación funciona.
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerSurface(
                streamUrl = streamUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Expect/actual para el surface del player.
 * En JVM/Desktop: embebe VideoSurface (VLCj).
 * En otras plataformas: stub por implementar en fases posteriores.
 */
@Composable
expect fun PlayerSurface(streamUrl: String, modifier: Modifier = Modifier)
