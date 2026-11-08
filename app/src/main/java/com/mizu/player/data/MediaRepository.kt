package com.mizu.player.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton repository holding media library state in memory.
 * Provides instant initial display (<30ms) and background subtitle badge enrichment.
 */
object MediaRepository {
    var folders by mutableStateOf<List<FolderItem>>(emptyList())
    var allVideos by mutableStateOf<List<VideoItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isInitialized by mutableStateOf(false)

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    suspend fun loadIfNeeded(context: Context, force: Boolean = false) {
        if (isInitialized && !force) return
        isLoading = true
        val (f, v) = MediaScanner.scanMedia(context)
        folders = f
        allVideos = v
        isLoading = false
        isInitialized = true

        // Background non-blocking subtitle enrichment for MKV containers
        enrichSubtitlesInBackground()
    }

    suspend fun refresh(context: Context) {
        loadIfNeeded(context, force = true)
    }

    private fun enrichSubtitlesInBackground() {
        backgroundScope.launch {
            var anyChanged = false
            val updatedVideos = allVideos.map { video ->
                if (video.subtitleType == null && video.path.endsWith(".mkv", ignoreCase = true)) {
                    val embeddedSub = MediaScanner.detectEmbeddedSubtitle(video.path)
                    if (embeddedSub != null) {
                        anyChanged = true
                        video.copy(subtitleType = embeddedSub)
                    } else {
                        video
                    }
                } else {
                    video
                }
            }

            if (anyChanged) {
                allVideos = updatedVideos
                val videoMap = updatedVideos.associateBy { it.id }
                folders = folders.map { folder ->
                    folder.copy(videos = folder.videos.map { v ->
                        videoMap[v.id] ?: v
                    })
                }
            }
        }
    }
}
