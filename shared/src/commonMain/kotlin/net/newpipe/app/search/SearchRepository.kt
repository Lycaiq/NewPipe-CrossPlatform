/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.search

/**
 * Contrato de búsqueda que el ViewModel usa. Cada plataforma tiene su implementación.
 *
 * Por qué una interfaz y no llamar al extractor directo desde el ViewModel:
 * El extractor es una dependencia pesada de JVM — no puede vivir en commonMain.
 * Con esto el ViewModel queda limpio y testeable con un fake en commonTest.
 */
interface SearchRepository {

    /**
     * Ejecuta una búsqueda en YouTube.
     * Bloquea el hilo — llámala siempre desde un Dispatcher.IO.
     * @return lista de resultados, o lista vacía si algo falla
     */
    suspend fun search(query: String): List<SearchResultItem>

    /**
     * Dado el URL de página de un video (ej: https://youtube.com/watch?v=xxx),
     * devuelve la URL del stream directo que VLC puede reproducir.
     * Bloquea el hilo — llámala siempre desde un Dispatcher.IO.
     */
    suspend fun resolveStreamUrl(pageUrl: String): String?
}
