/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import co.touchlab.kermit.Logger
import org.koin.core.annotation.Singleton
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

/**
 * Implementación de VideoPlayer para JVM usando VLCj.
 *
 * Por qué EmbeddedMediaPlayerComponent y no CallbackMediaPlayer:
 * EmbeddedMediaPlayerComponent usa el pipeline de renderizado nativo de VLC,
 * que es sustancialmente más eficiente en CPU que decodificar frame a frame
 * en Kotlin y copiarlos a un ImageBitmap para Compose. Para streams de alta
 * resolución la diferencia es brutal (~30% CPU menos).
 */
@Singleton(binds = [VideoPlayer::class])
class JVMVideoPlayer : VideoPlayer {

    private var mediaPlayerComponent: EmbeddedMediaPlayerComponent? = null
    private val player: MediaPlayer? get() = mediaPlayerComponent?.mediaPlayer()

    private val _state = MutableVideoPlayerState()
    override val state: VideoPlayerState get() = _state.snapshot()

    init {
        // NativeDiscovery busca libvlc en los paths estándar del SO.
        // Si falla aquí, VLC no está instalado o no está en PATH.
        val found = NativeDiscovery().discover()
        if (!found) {
            Logger.e("JVMVideoPlayer") { "VLC no encontrado en el sistema. El reproductor no funcionará." }
            _state.errorMessage = "VLC no instalado o no encontrado en PATH"
        } else {
            mediaPlayerComponent = EmbeddedMediaPlayerComponent()
            setupEventListeners()
        }
    }

    private fun setupEventListeners() {
        player?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

            override fun playing(mediaPlayer: MediaPlayer) {
                _state.playbackStatus = PlaybackStatus.PLAYING
                _state.isBuffering = false
                _state.errorMessage = null
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _state.playbackStatus = PlaybackStatus.PAUSED
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _state.playbackStatus = PlaybackStatus.STOPPED
                _state.currentPositionMs = 0L
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                // Solo marcamos buffering si no está al 100% aún
                _state.isBuffering = newCache < 100f
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                _state.currentPositionMs = newTime
                // Aprovechamos el tick de tiempo para actualizar la duración
                // sin necesidad de un timer separado
                _state.durationMs = mediaPlayer.status().length()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                Logger.e("JVMVideoPlayer") { "Error de reproducción en: ${mediaPlayer.media()?.info()?.mrl()}" }
                _state.playbackStatus = PlaybackStatus.ERROR
                _state.errorMessage = "Error al reproducir el medio"
            }
        })
    }

    override fun play(url: String) {
        val p = player ?: return logNoPlayer("play")
        _state.playbackStatus = PlaybackStatus.LOADING
        _state.isBuffering = true
        p.media().play(url)
    }

    override fun pause() {
        val p = player ?: return logNoPlayer("pause")
        if (p.status().isPlaying) p.controls().pause()
    }

    override fun resume() {
        val p = player ?: return logNoPlayer("resume")
        if (!p.status().isPlaying) p.controls().play()
    }

    override fun stop() {
        val p = player ?: return logNoPlayer("stop")
        p.controls().stop()
        _state.playbackStatus = PlaybackStatus.STOPPED
    }

    override fun seekTo(positionMs: Long) {
        val p = player ?: return logNoPlayer("seekTo")
        val duration = p.status().length()
        // Clampear para no mandar posiciones fuera de rango que hacen explotar VLC
        val clamped = positionMs.coerceIn(0L, duration)
        p.controls().setTime(clamped)
    }

    override fun setVolume(volume: Int) {
        val p = player ?: return logNoPlayer("setVolume")
        val clamped = volume.coerceIn(0, 100)
        p.audio().setVolume(clamped)
        _state.volume = clamped
    }

    override fun release() {
        mediaPlayerComponent?.release()
        mediaPlayerComponent = null
        _state.playbackStatus = PlaybackStatus.IDLE
    }

    /** Devuelve el componente AWT/Swing para embeber en SwingPanel de Compose */
    fun getComponent(): EmbeddedMediaPlayerComponent? = mediaPlayerComponent

    private fun logNoPlayer(action: String) =
        Logger.w("JVMVideoPlayer") { "Acción '$action' ignorada: player no inicializado (¿VLC instalado?)" }
}

/**
 * Holder mutable del estado interno del player. Solo JVMVideoPlayer lo toca.
 * La UI solo ve VideoPlayerState (inmutable) via snapshot().
 */
internal class MutableVideoPlayerState {
    var playbackStatus: PlaybackStatus = PlaybackStatus.IDLE
    var currentPositionMs: Long = 0L
    var durationMs: Long = 0L
    var volume: Int = 100
    var isBuffering: Boolean = false
    var errorMessage: String? = null

    fun snapshot() = VideoPlayerState(
        playbackStatus = playbackStatus,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        volume = volume,
        isBuffering = isBuffering,
        errorMessage = errorMessage
    )
}
