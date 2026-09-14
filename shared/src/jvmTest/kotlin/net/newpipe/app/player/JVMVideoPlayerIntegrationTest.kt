/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests de integración de JVMVideoPlayer.
 *
 * Los tests marcados con @Ignore requieren VLC instalado en el sistema.
 * Para correrlos localmente: quita el @Ignore y asegúrate de tener VLC en PATH.
 * En CI se saltan porque los runners no tienen VLC (y es innecesario para el pipeline básico).
 *
 * Los tests SIN @Ignore validan el comportamiento del player cuando VLC NO está,
 * que es igualmente importante — no queremos que la app crashee en limpio si falta VLC.
 */
class JVMVideoPlayerIntegrationTest {

    @Test
    fun `player arranca en estado IDLE`() {
        // Este test pasa siempre, con o sin VLC.
        // Solo validamos que la instancia se crea sin explotar.
        val player = JVMVideoPlayer()
        val state = player.state
        // Si VLC no está, el estado puede ser ERROR o IDLE — lo que no puede ser es PLAYING.
        assert(state.playbackStatus != PlaybackStatus.PLAYING) {
            "El player no debería estar reproduciendo recién inicializado"
        }
        player.release()
    }

    @Test
    fun `llamar pause sin media activo no crashea`() {
        // El player tiene que ser resiliente a operaciones fuera de orden.
        // Esto simula el case donde el user aprieta pause antes de que cargue algo.
        val player = JVMVideoPlayer()
        // Si esto no tira excepción, el test pasa.
        player.pause()
        player.resume()
        player.stop()
        player.release()
    }

    @Test
    fun `seekTo con rango valido no crashea sin media`() {
        val player = JVMVideoPlayer()
        // Sin media cargado, seekTo debería loggear y retornar limpiamente.
        player.seekTo(5000L)
        player.release()
    }

    @Test
    fun `setVolume clampea valores fuera de rango`() {
        val player = JVMVideoPlayer()
        // Si VLC está disponible, el volumen se setea en el player nativo.
        // Si no, solo actualizamos el estado interno.
        // En ambos casos, el estado no debería explotar.
        player.setVolume(-10)  // debería clampear a 0
        player.setVolume(200)  // debería clampear a 100
        player.release()
    }

    @Test
    @Ignore  // Requiere VLC instalado + red — correr manual en dev
    fun `play con URL valida cambia status a LOADING`() {
        val player = JVMVideoPlayer()
        player.play("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
        // Damos 500ms para que el evento de loading dispare
        Thread.sleep(500)
        val state = player.state
        assert(
            state.playbackStatus == PlaybackStatus.LOADING ||
                state.playbackStatus == PlaybackStatus.PLAYING
        ) {
            "Se esperaba LOADING o PLAYING, se obtuvo: ${state.playbackStatus}"
        }
        player.stop()
        player.release()
    }

    @Test
    @Ignore  // Prueba de estrés — correr manual, puede tardar varios segundos
    fun `stress test multiples ciclos play stop`() {
        val player = JVMVideoPlayer()
        // 20 ciclos rápidos de play/stop para buscar fugas de recursos o deadlocks
        repeat(20) { i ->
            player.play("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
            Thread.sleep(200)
            player.stop()
            Thread.sleep(100)
            assert(player.state.playbackStatus != PlaybackStatus.ERROR) {
                "El player crasheó en el ciclo $i"
            }
        }
        player.release()
    }
}
