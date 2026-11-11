package com.mizu.player.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mizu.player.data.preferences.UserPreferencesManager
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.FolderInfo
import com.mizu.player.domain.model.Playlist
import com.mizu.player.domain.model.PlaylistItem
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.domain.model.ViewMode
import com.mizu.player.domain.repository.MediaRepository
import com.mizu.player.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val sortOrder: StateFlow<SortOrder> = preferencesManager.videoSortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOrder.DATE_DESC)

    val viewMode: StateFlow<ViewMode> = preferencesManager.viewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.GRID)

    val videos: StateFlow<List<VideoItem>> = sortOrder.flatMapLatest { order ->
        mediaRepository.getVideos(order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<VideoItem>> = mediaRepository.getContinueWatching()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderInfo>> = mediaRepository.getFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioTracks: StateFlow<List<AudioItem>> = mediaRepository.getAudioTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection mode state
    private val _selectedVideoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVideoIds: StateFlow<Set<Long>> = _selectedVideoIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Video Info sheet state
    private val _inspectedVideo = MutableStateFlow<VideoItem?>(null)
    val inspectedVideo: StateFlow<VideoItem?> = _inspectedVideo.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesManager.setVideoSortOrder(order)
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch {
            preferencesManager.setViewMode(mode)
        }
    }

    fun toggleVideoSelection(videoId: Long) {
        val current = _selectedVideoIds.value.toMutableSet()
        if (current.contains(videoId)) {
            current.remove(videoId)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(videoId)
            _isSelectionMode.value = true
        }
        _selectedVideoIds.value = current
    }

    fun enterSelectionMode(initialVideoId: Long) {
        _isSelectionMode.value = true
        _selectedVideoIds.value = setOf(initialVideoId)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedVideoIds.value = emptySet()
    }

    fun selectAllVideos() {
        _selectedVideoIds.value = videos.value.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun inspectVideo(video: VideoItem?) {
        _inspectedVideo.value = video
    }

    fun refreshMedia() {
        viewModelScope.launch {
            mediaRepository.scanMedia()
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val selected = videos.value.filter { _selectedVideoIds.value.contains(it.id) }
            selected.forEachIndexed { index, video ->
                playlistRepository.addToPlaylist(
                    playlistId,
                    PlaylistItem(
                        playlistId = playlistId,
                        mediaUri = video.uri.toString(),
                        title = video.title,
                        duration = video.duration,
                        position = index,
                        isVideo = true
                    )
                )
            }
            exitSelectionMode()
        }
    }
}
