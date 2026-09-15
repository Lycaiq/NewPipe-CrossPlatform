/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.search

/**
 * Resultado individual de búsqueda que la UI consume.
 * Solo guardamos lo que realmente vamos a pintar — nada más.
 * El thumbnail es una URL porque Coil (o lo que usemos) lo descarga lazy.
 */
data class SearchResultItem(
    val title: String,
    val uploaderName: String,
    val uploaderUrl: String? = null,
    val uploadDateMs: Long? = null,
    val duration: String,          // ya formateado "HH:MM:SS" o "LIVE"
    val thumbnailUrl: String?,
    val streamUrl: String,          // URL de la página del video, para pasarla al extractor
    val viewCount: Long = 0L
)

/**
 * Contiene la URL directa para reproducir y la lista de videos relacionados.
 */
data class StreamDetails(
    val directUrl: String,
    val relatedItems: List<SearchResultItem>
)
