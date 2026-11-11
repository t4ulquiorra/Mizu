package com.mizu.player.ui.screens.player

import android.net.Uri
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mizu.player.data.preferences.UserPreferencesManager
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.repository.MediaRepository
import com.mizu.player.player.engine.ExoPlayerEngine
import com.mizu.player.player.engine.PlayerEngineInterface
import com.mizu.player.player.engine.PlayerPlaybackState
import com.mizu.player.player.engine.VlcPlayerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoEngine: ExoPlayerEngine,
    private val vlcEngine: VlcPlayerEngine,
    private val preferencesManager: UserPreferencesManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private var activeEngine: PlayerEngineInterface = exoEngine
    private val _currentEngine = MutableStateFlow(PlayerEngine.EXOPLAYER)
    val currentEngine: StateFlow<PlayerEngine> = _currentEngine.asStateFlow()

    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    private var currentMediaUri: Uri? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var currentVideoId: Long = 0L

    private var stateJob: Job? = null

    init {
        viewModelScope.launch {
            val preferredEngine = preferencesManager.playerEngine.first()
            _currentEngine.value = preferredEngine
            activeEngine = if (preferredEngine == PlayerEngine.VLC) vlcEngine else exoEngine

            observeActiveEngine()
        }
    }

    private fun observeActiveEngine() {
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            activeEngine.playbackState.collect { state ->
                _playbackState.value = state

                // Update position in repository periodically
                if (currentVideoId != 0L && state.currentPosition > 0) {
                    mediaRepository.updatePlaybackPosition(currentVideoId, state.currentPosition)
                }

                // Auto-fallback to VLC if ExoPlayer fails
                if (state.error != null && activeEngine.engineType == PlayerEngine.EXOPLAYER) {
                    fallbackToVlc()
                }
            }
        }
    }

    private fun fallbackToVlc() {
        val uri = currentMediaUri ?: return
        val pos = _playbackState.value.currentPosition
        val title = _playbackState.value.title

        activeEngine.release()
        activeEngine = vlcEngine
        _currentEngine.value = PlayerEngine.VLC

        attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
        activeEngine.prepare(uri, title, pos)
        observeActiveEngine()
    }

    fun initialize(uri: Uri, isVideo: Boolean, title: String = "") {
        currentMediaUri = uri
        currentVideoId = uri.hashCode().toLong()

        viewModelScope.launch {
            val resume = preferencesManager.resumePlayback.first()
            val defaultSpd = preferencesManager.defaultSpeed.first()

            attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
            activeEngine.prepare(uri, title, 0L)
            if (defaultSpd != 1.0f) {
                activeEngine.setPlaybackSpeed(defaultSpd)
            }
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        activeEngine.attachSurfaceView(surfaceView)
    }

    fun detachSurface() {
        activeEngine.detachSurfaceView()
        attachedSurfaceView = null
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            activeEngine.pause()
        } else {
            activeEngine.play()
        }
    }

    fun seekTo(positionMs: Long) {
        activeEngine.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (_playbackState.value.currentPosition + deltaMs).coerceIn(0L, _playbackState.value.duration)
        activeEngine.seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        activeEngine.setPlaybackSpeed(speed)
    }

    fun selectSubtitle(index: Int) {
        activeEngine.selectSubtitleTrack(index)
    }

    fun selectAudioTrack(index: Int) {
        activeEngine.selectAudioTrack(index)
    }

    fun switchEngine(engine: PlayerEngine) {
        if (_currentEngine.value == engine) return
        val uri = currentMediaUri ?: return
        val pos = _playbackState.value.currentPosition
        val title = _playbackState.value.title

        activeEngine.release()
        activeEngine = if (engine == PlayerEngine.VLC) vlcEngine else exoEngine
        _currentEngine.value = engine

        attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
        activeEngine.prepare(uri, title, pos)
        observeActiveEngine()
    }

    override fun onCleared() {
        super.onCleared()
        stateJob?.cancel()
        activeEngine.release()
    }
}
