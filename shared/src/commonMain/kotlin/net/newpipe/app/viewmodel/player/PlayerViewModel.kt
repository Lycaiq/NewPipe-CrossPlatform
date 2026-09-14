/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.player

import androidx.lifecycle.ViewModel
import net.newpipe.app.player.PlaybackStatus
import net.newpipe.app.player.VideoPlayer
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel que conecta la UI del reproductor con la implementación nativa (VideoPlayer).
 * Agrupa toda la lógica de control para que la UI solo dibuje y reaccione.
 */
@KoinViewModel
class PlayerViewModel(private val player: VideoPlayer) : ViewModel() {

    val playerState = player.state

    /** Inicia la reproducción */
    fun play(url: String) {
        player.play(url)
    }

    /** Alterna entre play y pausa */
    fun togglePlayPause() {
        when (playerState.value.playbackStatus) {
            PlaybackStatus.PLAYING -> player.pause()
            PlaybackStatus.PAUSED -> player.resume()
            else -> {} // Si está cargando o en error no hacemos nada
        }
    }

    /** Adelanta o retrocede N segundos */
    fun seekRelative(seconds: Int) {
        val currentMs = playerState.value.currentPositionMs
        val targetMs = currentMs + (seconds * 1000)
        player.seekTo(targetMs)
    }

    /** Salta a un porcentaje específico del video (0.0 a 1.0) */
    fun seekToPercentage(percentage: Float) {
        val duration = playerState.value.durationMs
        if (duration > 0) {
            player.seekTo((duration * percentage).toLong())
        }
    }

    /** Ajusta el volumen relativo */
    fun adjustVolume(delta: Int) {
        val current = playerState.value.volume
        player.setVolume(current + delta)
    }
    
    /** Detiene el video al salir de la pantalla */
    fun stop() {
        player.stop()
    }

    override fun onCleared() {
        super.onCleared()
        player.stop() // Asegurar que paramos si el ViewModel muere
    }
}
