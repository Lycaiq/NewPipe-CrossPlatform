/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.player.PlaybackStatus
import net.newpipe.app.player.VideoPlayerState
import net.newpipe.app.viewmodel.player.PlayerViewModel
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.ic_fullscreen
import newpipe.shared.generated.resources.ic_pause
import newpipe.shared.generated.resources.ic_play
import newpipe.shared.generated.resources.ic_volume_up
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Pantalla principal del reproductor.
 * Contiene la superficie de video (dependiente de plataforma) y los controles.
 */
@Composable
fun PlayerScreen(
    destination: Destination.Player,
    navigator: Navigator = koinInject(),
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val fullscreenController = LocalFullscreenController.current
    val isFullscreen = fullscreenController.isFullscreen

    LaunchedEffect(destination.streamUrl) {
        viewModel.play(destination.streamUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
            // Si salimos y estaba en fullscreen, volvemos a la normalidad
            if (fullscreenController.isFullscreen) {
                fullscreenController.toggleFullscreen()
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = destination.title.ifBlank { "Reproduciendo" },
                    onNavigateUp = { navigator.navigateUp() }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Spacebar -> { viewModel.togglePlayPause(); true }
                        Key.F -> { fullscreenController.toggleFullscreen(); true }
                        Key.DirectionLeft -> { viewModel.seekRelative(-10); true }
                        Key.DirectionRight -> { viewModel.seekRelative(10); true }
                        Key.DirectionUp -> { viewModel.adjustVolume(10); true }
                        Key.DirectionDown -> { viewModel.adjustVolume(-10); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            PlayerSurface(
                streamUrl = destination.streamUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Controles. Si estamos en Fullscreen sin blending, igual los dibujamos abajo.
            // Para mejorar la inmersión le ponemos un fondo negro.
            PlayerControls(
                state = state,
                isFullscreen = isFullscreen,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekPercentage = viewModel::seekToPercentage,
                onToggleFullscreen = fullscreenController::toggleFullscreen,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest)
            )
        }
    }
}

@Composable
private fun PlayerControls(
    state: VideoPlayerState,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekPercentage: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Timeline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTime(state.currentPositionMs),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = state.progress,
                onValueChange = { onSeekPercentage(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelMedium
            )
        }

        // Botonera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder a la izquierda para centrar el Play
            Row(modifier = Modifier.weight(1f)) {}

            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(64.dp)) {
                val icon = if (state.playbackStatus == PlaybackStatus.PLAYING) {
                    Res.drawable.ic_pause
                } else {
                    Res.drawable.ic_play
                }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(48.dp)
                )
            }

            // Controles de volumen y fullscreen a la derecha
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_volume_up),
                    contentDescription = "Volume",
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
                Text(
                    text = "${state.volume}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_fullscreen),
                        contentDescription = "Fullscreen"
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val mStr = m.toString().padStart(2, '0')
    val sStr = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mStr:$sStr" else "$mStr:$sStr"
}

/**
 * Expect/actual para el surface del player.
 * En JVM/Desktop: embebe VideoSurface (VLCj).
 * En otras plataformas: stub por implementar en fases posteriores.
 */
@Composable
expect fun PlayerSurface(streamUrl: String, modifier: Modifier = Modifier)
