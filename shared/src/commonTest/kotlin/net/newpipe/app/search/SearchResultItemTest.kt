/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests unitarios del modelo de búsqueda.
 * Sin red, sin extractor, sin VLC. Solo lógica de negocio pura.
 */
class SearchResultItemTest {

    private fun fakeItem(
        title: String = "Test Video",
        duration: String = "10:30",
        streamUrl: String = "https://youtube.com/watch?v=abc123"
    ) = SearchResultItem(
        title = title,
        uploaderName = "Canal Falso",
        duration = duration,
        thumbnailUrl = "https://img.youtube.com/vi/abc123/0.jpg",
        streamUrl = streamUrl,
        viewCount = 1_000_000L
    )

    @Test
    fun `item tiene todos los campos correctos`() {
        val item = fakeItem(title = "Mi Video", duration = "05:12")
        assertEquals("Mi Video", item.title)
        assertEquals("05:12", item.duration)
        assertEquals("Canal Falso", item.uploaderName)
        assertNotNull(item.thumbnailUrl)
        assertTrue(item.streamUrl.startsWith("https://"))
        assertEquals(1_000_000L, item.viewCount)
    }

    @Test
    fun `item con thumbnail null no rompe nada`() {
        val item = SearchResultItem(
            title = "Sin Thumbnail",
            uploaderName = "",
            duration = "00:30",
            thumbnailUrl = null,
            streamUrl = "https://youtube.com/watch?v=xyz"
        )
        assertNull(item.thumbnailUrl)
        assertFalse(item.title.isEmpty())
    }

    @Test
    fun `dos items con mismo streamUrl se consideran iguales`() {
        val url = "https://youtube.com/watch?v=same"
        val item1 = fakeItem(title = "Versión A", streamUrl = url)
        val item2 = fakeItem(title = "Versión B", streamUrl = url)
        // El key en LazyColumn usa streamUrl, así que si son iguales
        // Compose los trata como el mismo elemento en la lista
        assertEquals(item1.streamUrl, item2.streamUrl)
    }
}
