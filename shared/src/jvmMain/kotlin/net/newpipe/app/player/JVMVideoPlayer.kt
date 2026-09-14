/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    private val _state = MutableStateFlow(VideoPlayerState())
    override val state: StateFlow<VideoPlayerState> get() = _state

    init {
        // NativeDiscovery busca libvlc en los paths estándar del SO.
        // Si falla aquí, VLC no está instalado o no está en PATH.
        val found = NativeDiscovery().discover()
        if (!found) {
            Logger.e("JVMVideoPlayer") { "VLC no encontrado en el sistema. El reproductor no funcionará." }
            _state.update { it.copy(errorMessage = "VLC no instalado o no encontrado en PATH") }
        } else {
            mediaPlayerComponent = EmbeddedMediaPlayerComponent()
            setupEventListeners()
        }
    }

    private fun setupEventListeners() {
        player?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

            override fun playing(mediaPlayer: MediaPlayer) {
                _state.update { 
                    it.copy(
                        playbackStatus = PlaybackStatus.PLAYING,
                        isBuffering = false,
                        errorMessage = null
                    )
                }
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _state.update { it.copy(playbackStatus = PlaybackStatus.PAUSED) }
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _state.update { 
                    it.copy(
                        playbackStatus = PlaybackStatus.STOPPED,
                        currentPositionMs = 0L
                    )
                }
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                // Solo marcamos buffering si no está al 100% aún
                _state.update { it.copy(isBuffering = newCache < 100f) }
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                val length = mediaPlayer.status().length()
                _state.update { 
                    it.copy(
                        currentPositionMs = newTime,
                        durationMs = length
                    )
                }
            }

            override fun error(mediaPlayer: MediaPlayer) {
                Logger.e("JVMVideoPlayer") { "Error de reproducción en: ${mediaPlayer.media()?.info()?.mrl()}" }
                _state.update { 
                    it.copy(
                        playbackStatus = PlaybackStatus.ERROR,
                        errorMessage = "Error al reproducir el medio"
                    )
                }
            }
        })
    }

    override fun play(url: String) {
        val p = player ?: return logNoPlayer("play")
        _state.update { 
            it.copy(
                playbackStatus = PlaybackStatus.LOADING,
                isBuffering = true
            )
        }
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
        _state.update { it.copy(playbackStatus = PlaybackStatus.STOPPED) }
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
        _state.update { it.copy(volume = clamped) }
    }

    override fun release() {
        mediaPlayerComponent?.release()
        mediaPlayerComponent = null
        _state.update { it.copy(playbackStatus = PlaybackStatus.IDLE) }
    }

    /** Devuelve el componente AWT/Swing para embeber en SwingPanel de Compose */
    fun getComponent(): EmbeddedMediaPlayerComponent? = mediaPlayerComponent

    private fun logNoPlayer(action: String) =
        Logger.w("JVMVideoPlayer") { "Acción '$action' ignorada: player no inicializado (¿VLC instalado?)" }
}
