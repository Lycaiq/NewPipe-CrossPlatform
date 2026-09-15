/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.search

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.channel.ChannelInfo


/**
 * Implementación del repo de búsqueda para JVM.
 *
 * NewPipe Extractor es completamente bloqueante (sin coroutines ni rx dentro),
 * así que todo va en Dispatchers.IO. No hay de otra.
 *
 * Por qué no guardamos caché aquí: la caché va en el ViewModel con StateFlow.
 * Este repo es stateless a propósito para que sea fácil de testear y de reemplazar.
 */
@Singleton(binds = [SearchRepository::class])
class JVMSearchRepository : SearchRepository {

    override suspend fun search(query: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            try {
                val searchHandler = ServiceList.YouTube
                    .getSearchQHFactory()
                    .fromQuery(
                        query,
                        listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                        ""
                    )

                val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, searchHandler)

                searchInfo.relatedItems.mapNotNull { item ->
                    // El extractor puede devolver channels o playlists mezclados con videos.
                    // Solo nos interesan los streams de video.
                    if (item !is org.schabi.newpipe.extractor.stream.StreamInfoItem) return@mapNotNull null

                    SearchResultItem(
                        title = item.name ?: return@mapNotNull null,
                        uploaderName = item.uploaderName ?: "",
                        uploaderUrl = item.uploaderUrl,
                        duration = item.duration.formatDuration(),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        streamUrl = item.url ?: return@mapNotNull null,
                        viewCount = item.viewCount
                    )
                }
            } catch (e: Exception) {
                Logger.e("JVMSearchRepository", e) { "Búsqueda falló para query: '$query'" }
                emptyList()
            }
        }

    override suspend fun resolveStreamDetails(pageUrl: String): StreamDetails? =
        withContext(Dispatchers.IO) {
            try {
                val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, pageUrl)

                // Extraer videos relacionados
                val relatedItems = streamInfo.relatedItems.mapNotNull { item ->
                    if (item !is org.schabi.newpipe.extractor.stream.StreamInfoItem) return@mapNotNull null
                    SearchResultItem(
                        title = item.name ?: return@mapNotNull null,
                        uploaderName = item.uploaderName ?: "",
                        uploaderUrl = item.uploaderUrl,
                        duration = item.duration.formatDuration(),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        streamUrl = item.url ?: return@mapNotNull null,
                        viewCount = item.viewCount
                    )
                }

                // Preferimos el stream con mejor calidad que VLC pueda manejar directamente.
                // HLS/DASH los skippeamos aquí — si el stream es LIVE usamos el HLS sí o sí.
                val directUrl = if (streamInfo.streamType == StreamType.LIVE_STREAM ||
                    streamInfo.streamType == StreamType.AUDIO_LIVE_STREAM
                ) {
                    streamInfo.hlsUrl.ifBlank { null }
                } else {
                    // Para videos normales preferimos el stream de video+audio combinado (progressive).
                    streamInfo.videoStreams
                        .filter { it.isVideoOnly.not() }  // solo streams con audio
                        .maxByOrNull { it.height }
                        ?.content
                        ?: streamInfo.videoStreams.maxByOrNull { it.height }?.content
                        ?: streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
                }

                directUrl?.let { StreamDetails(it, relatedItems) }
            } catch (e: Exception) {
                Logger.e("JVMSearchRepository", e) { "No se pudo resolver URL para: $pageUrl" }
                null
            }
        }

    override suspend fun getChannelVideos(channelUrl: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            try {
                val channelInfo = org.schabi.newpipe.extractor.channel.ChannelInfo.getInfo(ServiceList.YouTube, channelUrl)
                val tab = channelInfo.tabs.firstOrNull() ?: return@withContext emptyList()
                val tabInfo = org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo.getInfo(ServiceList.YouTube, tab)
                
                tabInfo.relatedItems.mapNotNull { item ->
                    if (item !is org.schabi.newpipe.extractor.stream.StreamInfoItem) return@mapNotNull null

                    SearchResultItem(
                        title = item.name ?: return@mapNotNull null,
                        uploaderName = item.uploaderName ?: "",
                        uploaderUrl = item.uploaderUrl,
                        duration = item.duration.formatDuration(),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        streamUrl = item.url ?: return@mapNotNull null,
                        viewCount = item.viewCount
                    )
                }
            } catch (e: Exception) {
                Logger.e("JVMSearchRepository", e) { "Error al obtener videos del canal: $channelUrl" }
                emptyList()
            }
        }

    /** Convierte duración en segundos a "H:MM:SS" o "MM:SS". -1 = LIVE */
    private fun Long.formatDuration(): String {
        if (this < 0) return "EN VIVO"
        val h = this / 3600
        val m = (this % 3600) / 60
        val s = this % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
