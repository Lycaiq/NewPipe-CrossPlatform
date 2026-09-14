/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests del estado del reproductor — no requieren VLC instalado porque
 * testeamos la lógica de estado pura, sin tocar el player nativo.
 * Los tests de integración que necesitan VLC están marcados con @Ignore
 * para que no exploten en CI donde no hay VLC instalado.
 */
class VideoPlayerStateTest {

    @Test
    fun `estado inicial es IDLE con ceros`() {
        val state = VideoPlayerState()
        assertEquals(PlaybackStatus.IDLE, state.playbackStatus)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertEquals(100, state.volume)
        assertEquals(false, state.isBuffering)
        assertNull(state.errorMessage)
    }

    @Test
    fun `progress es 0 cuando no hay duracion`() {
        val state = VideoPlayerState(durationMs = 0L, currentPositionMs = 500L)
        assertEquals(0f, state.progress)
    }

    @Test
    fun `progress calcula correctamente a la mitad`() {
        val state = VideoPlayerState(durationMs = 10_000L, currentPositionMs = 5_000L)
        assertEquals(0.5f, state.progress, absoluteTolerance = 0.001f)
    }

    @Test
    fun `progress es 1 al final del video`() {
        val state = VideoPlayerState(durationMs = 10_000L, currentPositionMs = 10_000L)
        assertEquals(1f, state.progress, absoluteTolerance = 0.001f)
    }

    @Test
    fun `snapshot de MutableVideoPlayerState refleja cambios`() {
        val mutable = MutableVideoPlayerState()

        mutable.playbackStatus = PlaybackStatus.PLAYING
        mutable.currentPositionMs = 3_000L
        mutable.durationMs = 60_000L
        mutable.isBuffering = true
        mutable.errorMessage = null

        val snapshot = mutable.snapshot()

        assertEquals(PlaybackStatus.PLAYING, snapshot.playbackStatus)
        assertEquals(3_000L, snapshot.currentPositionMs)
        assertEquals(60_000L, snapshot.durationMs)
        assertEquals(true, snapshot.isBuffering)
        assertNull(snapshot.errorMessage)
    }

    @Test
    fun `snapshot con error tiene status ERROR y mensaje`() {
        val mutable = MutableVideoPlayerState()
        mutable.playbackStatus = PlaybackStatus.ERROR
        mutable.errorMessage = "timeout al conectar"

        val snapshot = mutable.snapshot()

        assertEquals(PlaybackStatus.ERROR, snapshot.playbackStatus)
        assertNotNull(snapshot.errorMessage)
    }
}
