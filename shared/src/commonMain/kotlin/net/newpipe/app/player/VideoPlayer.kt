/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

/**
 * Contrato que tiene que cumplir cualquier reproductor en cualquier plataforma.
 * Cada target (JVM, Android, iOS) provee su propia implementación.
 */
interface VideoPlayer {

    /** Estado observable del reproductor */
    val state: VideoPlayerState

    /**
     * Carga y arranca la reproducción de una URL.
     * Acepta cualquier cosa que VLC/ExoPlayer entienda: HLS, DASH, MP4 directo, etc.
     */
    fun play(url: String)

    /** Pausa sin soltar el recurso — listo para continuar desde donde quedó */
    fun pause()

    /** Reanuda desde la posición actual */
    fun resume()

    /** Para y libera todos los recursos del media. Después de esto hay que llamar play() de nuevo */
    fun stop()

    /**
     * Salta a una posición específica.
     * @param positionMs posición en milisegundos. Se clampea entre 0 y duración total.
     */
    fun seekTo(positionMs: Long)

    /** Ajusta el volumen entre 0 (mute) y 100 (máximo nativo) */
    fun setVolume(volume: Int)

    /** Libera el recurso nativo. No se puede usar el player después de esto */
    fun release()
}
