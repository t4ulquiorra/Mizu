package com.mizu.player.player.engine

import android.net.Uri
import android.view.SurfaceView
import androidx.media3.ui.SubtitleView
import com.mizu.player.domain.model.AudioTrackInfo
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SubtitleTrack
import kotlinx.coroutines.flow.StateFlow

data class PlayerPlaybackState(
    val uri: Uri? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val firstFrameRendered: Boolean = false,
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudioTrack: Int = -1,
    val selectedSubtitleTrack: Int = -1,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    val error: String? = null
)

interface PlayerEngineInterface {
    val engineType: PlayerEngine
    val playbackState: StateFlow<PlayerPlaybackState>

    fun prepare(uri: Uri, title: String = "", startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun selectAudioTrack(index: Int)
    fun selectSubtitleTrack(index: Int)
    fun addSidecarSubtitle(uri: Uri, mimeType: String = "application/x-subrip", language: String? = "und", label: String? = "External Subtitle")
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)
    fun attachSurfaceView(surfaceView: SurfaceView)
    fun detachSurfaceView()
    fun attachSubtitleView(subtitleView: SubtitleView)
    fun detachSubtitleView()
    fun release()
}
