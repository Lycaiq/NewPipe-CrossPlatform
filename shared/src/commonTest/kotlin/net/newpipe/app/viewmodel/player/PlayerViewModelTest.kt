/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.newpipe.app.player.PlaybackStatus
import net.newpipe.app.player.VideoPlayer
import net.newpipe.app.player.VideoPlayerState
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerViewModelTest {

    @Test
    fun `togglePlayPause alterna correctamente el estado`() {
        val fakePlayer = FakeVideoPlayer()
        val viewModel = PlayerViewModel(fakePlayer)

        // Simular que el player está reproduciendo
        fakePlayer.mutableState.update { it.copy(playbackStatus = PlaybackStatus.PLAYING) }
        viewModel.togglePlayPause()
        assertEquals(PlaybackStatus.PAUSED, fakePlayer.mutableState.value.playbackStatus)

        // Simular que está pausado
        viewModel.togglePlayPause()
        assertEquals(PlaybackStatus.PLAYING, fakePlayer.mutableState.value.playbackStatus)
    }

    @Test
    fun `seekRelative calcula bien el desplazamiento`() {
        val fakePlayer = FakeVideoPlayer()
        val viewModel = PlayerViewModel(fakePlayer)

        fakePlayer.mutableState.update { it.copy(currentPositionMs = 15000L, durationMs = 60000L) }
        
        // Avanzar 10 segundos
        viewModel.seekRelative(10)
        assertEquals(25000L, fakePlayer.lastSeekTarget)

        // Retroceder 5 segundos
        viewModel.seekRelative(-5)
        assertEquals(10000L, fakePlayer.lastSeekTarget)
    }

    @Test
    fun `seekToPercentage calcula bien la posicion absoluta`() {
        val fakePlayer = FakeVideoPlayer()
        val viewModel = PlayerViewModel(fakePlayer)

        fakePlayer.mutableState.update { it.copy(durationMs = 100_000L) }
        
        viewModel.seekToPercentage(0.25f)
        assertEquals(25_000L, fakePlayer.lastSeekTarget)
    }

    @Test
    fun `stress test de play y stop rapidos`() {
        val fakePlayer = FakeVideoPlayer()
        val viewModel = PlayerViewModel(fakePlayer)

        for (i in 1..100) {
            viewModel.play("http://fake.com/$i")
            viewModel.stop()
        }
        
        assertEquals(PlaybackStatus.STOPPED, fakePlayer.mutableState.value.playbackStatus)
    }

    @Test
    fun `stress test de seek agresivo`() {
        val fakePlayer = FakeVideoPlayer()
        val viewModel = PlayerViewModel(fakePlayer)

        fakePlayer.mutableState.update { it.copy(currentPositionMs = 5000L, durationMs = 100_000L) }
        
        for (i in 1..1000) {
            viewModel.seekRelative(1) // Spam de +1s
            fakePlayer.mutableState.update { it.copy(currentPositionMs = fakePlayer.lastSeekTarget) }
        }
        
        assertEquals(1005000L, fakePlayer.lastSeekTarget) // 5000 + 1000*1000
    }
}

class FakeVideoPlayer : VideoPlayer {
    val mutableState = MutableStateFlow(VideoPlayerState())
    override val state: StateFlow<VideoPlayerState> = mutableState

    var lastSeekTarget = 0L

    override fun play(url: String) {
        mutableState.update { it.copy(playbackStatus = PlaybackStatus.PLAYING) }
    }

    override fun pause() {
        mutableState.update { it.copy(playbackStatus = PlaybackStatus.PAUSED) }
    }

    override fun resume() {
        mutableState.update { it.copy(playbackStatus = PlaybackStatus.PLAYING) }
    }

    override fun stop() {
        mutableState.update { it.copy(playbackStatus = PlaybackStatus.STOPPED) }
    }

    override fun seekTo(positionMs: Long) {
        lastSeekTarget = positionMs
    }

    override fun setVolume(volume: Int) {
        mutableState.update { it.copy(volume = volume) }
    }

    override fun release() {}
}
