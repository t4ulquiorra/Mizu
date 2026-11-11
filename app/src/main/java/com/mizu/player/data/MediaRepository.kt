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

    // Reactive playback positions map for immediate UI updates & resume
    val playbackPositions = androidx.compose.runtime.mutableStateMapOf<String, Long>()

    private var preferencesLoaded = false

    fun initPreferences(context: Context) {
        if (preferencesLoaded) return
        isGridView = UserPreferences.isGridView(context)
        sortOption = UserPreferences.getSortOption(context)
        selectedTab = UserPreferences.getSelectedTab(context)
        playbackPositions.clear()
        playbackPositions.putAll(UserPreferences.getAllPlaybackPositions(context))
        preferencesLoaded = true
    }

    fun getSavedPosition(video: VideoItem): Long {
        val key = UserPreferences.getVideoKey(video)
        return playbackPositions[key] ?: 0L
    }

    fun savePlaybackPosition(context: Context, video: VideoItem, positionMs: Long, durationMs: Long = 0L) {
        val key = UserPreferences.getVideoKey(video)
        val totalDuration = if (durationMs > 0) durationMs else video.durationMs
        val effectivePos = when {
            totalDuration > 0 && positionMs >= totalDuration - 5000 -> 0L
            totalDuration > 0 && (positionMs.toFloat() / totalDuration) >= 0.95f -> 0L
            positionMs < 3000L -> 0L
            else -> positionMs
        }

        if (effectivePos > 0) {
            playbackPositions[key] = effectivePos
        } else {
            playbackPositions.remove(key)
        }
        UserPreferences.setPlaybackPosition(context, key, effectivePos)
    }

    fun getProgressFraction(video: VideoItem): Float {
        val key = UserPreferences.getVideoKey(video)
        val pos = playbackPositions[key] ?: return 0f
        return if (video.durationMs > 0) {
            (pos.toFloat() / video.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    fun updateGridView(context: Context, isGrid: Boolean) {
        isGridView = isGrid
        UserPreferences.setGridView(context, isGrid)
    }

    fun updateSortOption(context: Context, option: SortOption) {
        sortOption = option
        UserPreferences.setSortOption(context, option)
    }

    fun updateSelectedTab(context: Context, tab: Int) {
        selectedTab = tab
        UserPreferences.setSelectedTab(context, tab)
    }

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    suspend fun loadIfNeeded(context: Context, force: Boolean = false) {
        initPreferences(context)
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
