package com.mizu.player.ui.screens.player

import android.net.Uri
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.SubtitleView
import com.mizu.player.data.preferences.UserPreferencesManager
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.repository.MediaRepository
import com.mizu.player.player.engine.ExoPlayerEngine
import com.mizu.player.player.engine.PlayerEngineInterface
import com.mizu.player.player.engine.PlayerPlaybackState
import com.mizu.player.player.engine.VlcPlayerEngine
import com.mizu.player.util.PlaybackLog
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

    val seekDurationSeconds: StateFlow<Int> = preferencesManager.seekDuration
        .stateIn(viewModelScope, SharingStarted.Eagerly, 10)

    private var currentMediaUri: Uri? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var attachedSubtitleView: SubtitleView? = null
    private var currentVideoId: Long = 0L
    private var stateJob: Job? = null

    private var lastSavedTime = 0L

    private fun observeActiveEngine() {
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            activeEngine.playbackState.collect { state ->
                _playbackState.value = state

                // Update position in database periodically (every 5 seconds)
                val now = System.currentTimeMillis()
                if (currentVideoId != 0L && state.currentPosition > 0 && now - lastSavedTime >= 5000L) {
                    lastSavedTime = now
                    mediaRepository.updatePlaybackPosition(currentVideoId, state.currentPosition)
                }

                // Automatic fallback to VLC if ExoPlayer fails
                if (state.error != null && activeEngine.engineType == PlayerEngine.EXOPLAYER) {
                    PlaybackLog.log("PlayerViewModel", "ExoPlayer failed with error: '${state.error}'. Auto-falling back to VLC at position ${state.currentPosition}ms")
                    fallbackToVlc()
                }
            }
        }
    }

    private fun fallbackToVlc() {
        val uri = currentMediaUri ?: return
        val pos = _playbackState.value.currentPosition
        val title = _playbackState.value.title
        val speed = _playbackState.value.playbackSpeed

        activeEngine.release()
        activeEngine = vlcEngine
        _currentEngine.value = PlayerEngine.VLC

        attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
        attachedSubtitleView?.let { activeEngine.attachSubtitleView(it) }
        activeEngine.prepare(uri, title, pos)
        if (speed != 1.0f) {
            activeEngine.setPlaybackSpeed(speed)
        }
        observeActiveEngine()
    }

    fun initialize(uri: Uri, isVideo: Boolean, title: String = "") {
        currentMediaUri = uri
        currentVideoId = uri.hashCode().toLong()

        viewModelScope.launch {
            val matching = mediaRepository.getVideoByUri(uri).first()
            if (matching != null) {
                currentVideoId = matching.id
            }

            val preferredEngine = preferencesManager.playerEngine.first()
            val selectedEngine = resolveEngine(uri, preferredEngine)

            PlaybackLog.log("PlayerViewModel", "Initializing playback for URI: $uri | isVideo: $isVideo | Preferred: $preferredEngine | Selected: $selectedEngine")

            _currentEngine.value = selectedEngine
            activeEngine = if (selectedEngine == PlayerEngine.VLC) vlcEngine else exoEngine

            attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
            attachedSubtitleView?.let { activeEngine.attachSubtitleView(it) }

            val resumePos = if (matching != null && matching.lastPlayedPosition > 3000L && matching.lastPlayedPosition < (matching.duration - 5000L)) {
                matching.lastPlayedPosition
            } else 0L

            val defaultSpd = preferencesManager.defaultSpeed.first()
            activeEngine.prepare(uri, title, resumePos)
            if (defaultSpd != 1.0f) {
                activeEngine.setPlaybackSpeed(defaultSpd)
            }

            observeActiveEngine()
        }
    }

    private fun resolveEngine(uri: Uri, preference: PlayerEngine): PlayerEngine {
        if (preference == PlayerEngine.EXOPLAYER) return PlayerEngine.EXOPLAYER
        if (preference == PlayerEngine.VLC) return PlayerEngine.VLC

        // AUTO Engine Selection:
        // Use VLC for exotic/legacy formats known to cause problems on hardware/ExoPlayer
        val path = uri.path ?: uri.toString()
        val ext = path.substringAfterLast('.', "").lowercase()

        val vlcFormats = setOf(
            "avi", "rm", "rmvb", "wmv", "asf", "flv", "f4v",
            "vob", "mpg", "mpeg", "ps", "ts", "m2ts", "mts", "iso", "divx", "ogv"
        )

        return if (ext in vlcFormats) {
            PlaybackLog.log("EngineSelector", "Auto-selected VLC for extension '.$ext'")
            PlayerEngine.VLC
        } else {
            PlaybackLog.log("EngineSelector", "Auto-selected ExoPlayer for extension '.$ext'")
            PlayerEngine.EXOPLAYER
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        PlaybackLog.log("PlayerViewModel", "attachSurface: $surfaceView")
        attachedSurfaceView = surfaceView
        activeEngine.attachSurfaceView(surfaceView)
    }

    fun detachSurface() {
        PlaybackLog.log("PlayerViewModel", "detachSurface")
        activeEngine.detachSurfaceView()
        attachedSurfaceView = null
    }

    fun attachSubtitleView(subtitleView: SubtitleView) {
        PlaybackLog.log("PlayerViewModel", "attachSubtitleView: $subtitleView")
        attachedSubtitleView = subtitleView
        activeEngine.attachSubtitleView(subtitleView)
    }

    fun detachSubtitleView() {
        PlaybackLog.log("PlayerViewModel", "detachSubtitleView")
        activeEngine.detachSubtitleView()
        attachedSubtitleView = null
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

    fun seekForward() {
        val deltaMs = seekDurationSeconds.value * 1000L
        val newPos = (_playbackState.value.currentPosition + deltaMs).coerceIn(0L, _playbackState.value.duration)
        activeEngine.seekTo(newPos)
    }

    fun seekBackward() {
        val deltaMs = seekDurationSeconds.value * 1000L
        val newPos = (_playbackState.value.currentPosition - deltaMs).coerceIn(0L, _playbackState.value.duration)
        activeEngine.seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        activeEngine.setPlaybackSpeed(speed)
        viewModelScope.launch {
            if (preferencesManager.rememberSpeed.first()) {
                preferencesManager.setDefaultSpeed(speed)
            }
        }
    }

    fun setVolume(volume: Float) {
        activeEngine.setVolume(volume)
    }

    fun selectSubtitle(index: Int) {
        activeEngine.selectSubtitleTrack(index)
    }

    fun selectAudioTrack(index: Int) {
        activeEngine.selectAudioTrack(index)
    }

    fun addSidecarSubtitle(uri: Uri) {
        activeEngine.addSidecarSubtitle(uri)
    }

    fun setAudioDelay(delayMs: Long) {
        activeEngine.setAudioDelay(delayMs)
    }

    fun setSubtitleDelay(delayMs: Long) {
        activeEngine.setSubtitleDelay(delayMs)
    }

    fun switchEngine(targetEngine: PlayerEngine) {
        if (_currentEngine.value == targetEngine) return
        val uri = currentMediaUri ?: return
        val pos = _playbackState.value.currentPosition
        val title = _playbackState.value.title
        val speed = _playbackState.value.playbackSpeed

        PlaybackLog.log("PlayerViewModel", "Switching engine from ${_currentEngine.value} to $targetEngine at position ${pos}ms")

        activeEngine.release()
        activeEngine = if (targetEngine == PlayerEngine.VLC) vlcEngine else exoEngine
        _currentEngine.value = targetEngine

        attachedSurfaceView?.let { activeEngine.attachSurfaceView(it) }
        attachedSubtitleView?.let { activeEngine.attachSubtitleView(it) }
        activeEngine.prepare(uri, title, pos)
        if (speed != 1.0f) {
            activeEngine.setPlaybackSpeed(speed)
        }
        observeActiveEngine()
    }

    fun toggleOtherEngine() {
        val next = if (_currentEngine.value == PlayerEngine.EXOPLAYER) PlayerEngine.VLC else PlayerEngine.EXOPLAYER
        switchEngine(next)
    }

    override fun onCleared() {
        super.onCleared()
        stateJob?.cancel()
        val pos = _playbackState.value.currentPosition
        if (currentVideoId != 0L && pos > 0) {
            viewModelScope.launch {
                mediaRepository.updatePlaybackPosition(currentVideoId, pos)
            }
        }
        activeEngine.release()
    }
}
