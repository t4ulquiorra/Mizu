package com.mizu.player.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem

/**
 * Singleton repository holding media library state in memory.
 * Preserves scanned folders and videos so returning from a playing video or tab is instantaneous (0ms).
 */
object MediaRepository {
    var folders by mutableStateOf<List<FolderItem>>(emptyList())
    var allVideos by mutableStateOf<List<VideoItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isInitialized by mutableStateOf(false)

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
}
