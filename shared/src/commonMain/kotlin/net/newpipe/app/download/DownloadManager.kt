/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

import kotlinx.coroutines.flow.StateFlow

/**
 * Gestor multiplataforma de descargas.
 */
interface DownloadManager {
    /** Flujo reactivo con todas las descargas (activas y terminadas) */
    val downloads: StateFlow<List<DownloadTask>>

    /**
     * Encola una descarga.
     * @param priority Mayor valor = mayor prioridad.
     * @return El ID asignado a la tarea.
     */
    fun enqueue(url: String, title: String, priority: Int = 0): String

    fun cancel(id: String)
    
    fun pause(id: String)
    
    fun resume(id: String)
}
