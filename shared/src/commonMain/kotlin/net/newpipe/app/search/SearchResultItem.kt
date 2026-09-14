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
    val duration: String,          // ya formateado "HH:MM:SS" o "LIVE"
    val thumbnailUrl: String?,
    val streamUrl: String,          // URL de la página del video, para pasarla al extractor
    val viewCount: Long = 0L
)
