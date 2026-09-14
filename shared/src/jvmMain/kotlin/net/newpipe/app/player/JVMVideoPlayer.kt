/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.awt.Canvas
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

import java.awt.Color
import java.awt.Component
import java.awt.event.HierarchyEvent
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

/**
 * Implementación de VideoPlayer para JVM usando VLCj.
 */
@Factory(binds = [VideoPlayer::class])
class JVMVideoPlayer : VideoPlayer {

    private var factory: MediaPlayerFactory? = null
    private var mediaPlayer: EmbeddedMediaPlayer? = null
    private var videoSurfaceCanvas: Canvas? = null
    private val player: MediaPlayer? get() = mediaPlayer

    private val _state = MutableStateFlow(VideoPlayerState())
    override val state: StateFlow<VideoPlayerState> get() = _state

    private var pendingUrl: String? = null
    private var isSurfaceAttached = false

    init {
        val found = NativeDiscovery().discover()
        if (!found) {
            Logger.e("JVMVideoPlayer") { "VLC no encontrado en el sistema. El reproductor no funcionará." }
            _state.update { it.copy(errorMessage = "VLC no instalado o no encontrado en PATH") }
        } else {
            factory = MediaPlayerFactory()
            mediaPlayer = factory!!.mediaPlayers().newEmbeddedMediaPlayer()
            
            videoSurfaceCanvas = Canvas().apply {
                background = Color.BLACK
                // FIX WINDOWS: VLCj lanza "The video surface component must be displayable"
                addHierarchyListener { e ->
                    if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L) {
                        if (isShowing && !isSurfaceAttached) {
                            mediaPlayer?.videoSurface()?.set(factory!!.videoSurfaces().newVideoSurface(this))
                            isSurfaceAttached = true
                            pendingUrl?.let { url ->
                                pendingUrl = null
                                mediaPlayer?.media()?.play(url)
                            }
                        }
                    }
                }
            }
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
        if (isSurfaceAttached) {
            p.media().play(url)
        } else {
            pendingUrl = url
        }
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
        mediaPlayer?.release()
        factory?.release()
        mediaPlayer = null
        factory = null
        _state.update { it.copy(playbackStatus = PlaybackStatus.IDLE) }
    }

    /** Devuelve el componente AWT/Swing para embeber en SwingPanel de Compose */
    fun getComponent(): Component? = videoSurfaceCanvas

    private fun logNoPlayer(action: String) =
        Logger.w("JVMVideoPlayer") { "Acción '$action' ignorada: player no inicializado (¿VLC instalado?)" }
}
