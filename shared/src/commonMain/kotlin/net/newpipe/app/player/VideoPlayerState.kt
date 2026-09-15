/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.runtime.Stable

/**
 * Snapshot del estado del reproductor que la UI consume directamente.
 * Es un data class puro — sin lógica, solo datos para pintar.
 * El ViewModel es quien lo actualiza.
 */
@Stable
data class VideoPlayerState(
    val playbackStatus: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Int = 100,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
) {
    /** Porcentaje de progreso entre 0.0 y 1.0 — listo para pasar a un Slider */
    val progress: Float
        get() = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
}

enum class PlaybackStatus {
    IDLE,       // sin nada cargado
    LOADING,    // cargando/bufferizando el primer frame
    PLAYING,
    PAUSED,
    STOPPED,
    FINISHED,   // Cuando terminó de reproducir
    ERROR
}
