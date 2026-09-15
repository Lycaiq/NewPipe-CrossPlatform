/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.newpipe.app.bookmark.BookmarkRepository
import net.newpipe.app.bookmark.SavedVideo
import net.newpipe.app.history.HistoryItem
import net.newpipe.app.history.HistoryRepository
import net.newpipe.app.player.PlaybackStatus
import net.newpipe.app.player.VideoPlayer
import net.newpipe.app.search.SearchRepository
import net.newpipe.app.search.SearchResultItem
import net.newpipe.app.utils.currentTimeMillis
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel que conecta la UI del reproductor con la implementación nativa (VideoPlayer).
 * Agrupa toda la lógica de control para que la UI solo dibuje y reaccione.
 */
@KoinViewModel
class PlayerViewModel(
    private val player: VideoPlayer,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    val playerState = player.state

    // Guardar los metadatos del video actual
    private var currentSavedVideo: SavedVideo? = null
    private var currentPageUrl: String = ""
    
    // Cola de reproducción (relacionados)
    private val _playQueue = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val playQueue: StateFlow<List<SearchResultItem>> = _playQueue.asStateFlow()

    init {
        viewModelScope.launch {
            player.state.collect { currentState ->
                if (currentState.playbackStatus == PlaybackStatus.FINISHED) {
                    playNextInQueue()
                }
            }
        }
    }

    val isBookmarked = bookmarkRepository.bookmarks.map { list ->
        list.any { it.streamUrl == currentPageUrl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun initializeVideo(pageUrl: String, title: String, uploader: String, duration: String, thumbnailUrl: String, viewCount: Long) {
        currentPageUrl = pageUrl
        currentSavedVideo = SavedVideo(
            title = title,
            uploaderName = uploader,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            streamUrl = pageUrl,
            viewCount = viewCount,
            savedAtMs = 0L
        )

        historyRepository.addToHistory(HistoryItem(
            streamUrl = pageUrl,
            title = title,
            uploaderName = uploader,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            viewCount = viewCount,
            lastAccessTimeMs = currentTimeMillis()
        ))
        
        resolveAndPlay(pageUrl)
    }

    private fun resolveAndPlay(pageUrl: String) {
        viewModelScope.launch {
            try {
                val details = searchRepository.resolveStreamDetails(pageUrl)
                if (details != null) {
                    _playQueue.value = details.relatedItems
                    player.play(details.directUrl)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    /** Alterna el estado de guardado (Bookmark) */
    fun toggleBookmark() {
        currentSavedVideo?.let {
            bookmarkRepository.toggleBookmark(it)
        }
    }


    fun playNextInQueue() {
        val nextItem = _playQueue.value.firstOrNull()
        if (nextItem != null) {
            initializeVideo(
                pageUrl = nextItem.streamUrl,
                title = nextItem.title,
                uploader = nextItem.uploaderName,
                duration = nextItem.duration,
                thumbnailUrl = nextItem.thumbnailUrl ?: "",
                viewCount = nextItem.viewCount
            )
        }
    }

    /** Alterna entre play y pausa */
    fun togglePlayPause() {
        when (playerState.value.playbackStatus) {
            PlaybackStatus.PLAYING -> player.pause()
            PlaybackStatus.PAUSED -> player.resume()
            else -> {} // Si está cargando o en error no hacemos nada
        }
    }

    /** Adelanta o retrocede N segundos */
    fun seekRelative(seconds: Int) {
        val currentMs = playerState.value.currentPositionMs
        val targetMs = currentMs + (seconds * 1000)
        player.seekTo(targetMs)
    }

    /** Salta a un porcentaje específico del video (0.0 a 1.0) */
    fun seekToPercentage(percentage: Float) {
        val duration = playerState.value.durationMs
        if (duration > 0) {
            player.seekTo((duration * percentage).toLong())
        }
    }

    /** Ajusta el volumen relativo */
    fun adjustVolume(delta: Int) {
        val current = playerState.value.volume
        player.setVolume(current + delta)
    }
    
    /** Detiene el video al salir de la pantalla */
    fun stop() {
        player.stop()
    }

    override fun onCleared() {
        super.onCleared()
        player.release() // Destruir el reproductor nativo y la ventana
    }
}
