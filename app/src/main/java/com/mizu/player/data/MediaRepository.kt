package com.mizu.player.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mizu.player.model.FolderItem
import com.mizu.player.model.SortOption
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton repository holding media library state in memory.
 * Preserves active folder, tab, and view preferences across navigation and playback.
 */
object MediaRepository {
    var folders by mutableStateOf<List<FolderItem>>(emptyList())
    var allVideos by mutableStateOf<List<VideoItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isInitialized by mutableStateOf(false)

    // Preserved active folder, tab, and view preferences across navigation and playback
    var selectedFolder by mutableStateOf<FolderItem?>(null)
    var selectedTab by mutableIntStateOf(0)
    var isGridView by mutableStateOf(true)
    var sortOption by mutableStateOf(SortOption.DATE_DESC)

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    suspend fun loadIfNeeded(context: Context, force: Boolean = false) {
        if (isInitialized && !force) return
        isLoading = true
        val (f, v) = MediaScanner.scanMedia(context)
        folders = f
        allVideos = v
        isLoading = false
        isInitialized = true
    }

    suspend fun refresh(context: Context) {
        loadIfNeeded(context, force = true)
    }

    /**
     * Enriches embedded MKV subtitles on-demand only for the folder currently opened.
     * Keeps home screen completely idle, snappy, and free from background disk I/O thrashing.
     */
    fun enrichFolderSubtitles(folder: FolderItem) {
        backgroundScope.launch {
            var anyChanged = false
            val updatedVideos = folder.videos.map { video ->
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
                val videoMap = updatedVideos.associateBy { it.id }
                allVideos = allVideos.map { videoMap[it.id] ?: it }
                folders = folders.map { f ->
                    if (f.path == folder.path) f.copy(videos = updatedVideos) else f
                }
                if (selectedFolder?.path == folder.path) {
                    selectedFolder = selectedFolder?.copy(videos = updatedVideos)
                }
            }
        }
    }
}
