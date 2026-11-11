package com.mizu.player.engine

import android.net.Uri
import com.mizu.player.model.VideoItem
import org.videolan.libvlc.util.VLCVideoLayout

data class AudioTrackInfo(
    val id: String,
    val index: Int,
    val name: String,
    val language: String,
    val codec: String,
    val channels: String,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val id: String,
    val index: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean,
    val isExternal: Boolean = false
)

enum class AspectRatioMode(val label: String) {
    FIT("Fit screen"),
    FILL("Fill / Crop"),
    ZOOM("Zoom 100%"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3")
}

interface PlayerEngineListener {
    fun onPlaybackStateChanged(state: Int) // Matches Player.STATE_IDLE, BUFFERING, READY, ENDED
    fun onIsPlayingChanged(isPlaying: Boolean)
    fun onTimeChanged(currentTimeMs: Long, durationMs: Long)
    fun onTracksChanged(audioTracks: List<AudioTrackInfo>, subtitleTracks: List<SubtitleTrackInfo>)
    fun onMediaItemTransition(video: VideoItem, index: Int)
    fun onError(errorMessage: String, canFallback: Boolean)
    fun onNotice(notice: String)
}

interface PlayerEngine {
    val isPlaying: Boolean
    val currentPositionMs: Long
    val durationMs: Long
    val currentVideo: VideoItem?
    val currentPlaybackSpeed: Float
    val currentAspectRatio: AspectRatioMode
    val currentAudioDelayMs: Long
    val currentSubtitleDelayMs: Long
    val currentVolumePercent: Int
    val playlist: List<VideoItem>
    val currentIndex: Int

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setAspectRatio(mode: AspectRatioMode)
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)
    fun setVolumeBoost(volumePercent: Int)

    fun getAudioTracks(): List<AudioTrackInfo>
    fun getSubtitleTracks(): List<SubtitleTrackInfo>
    fun selectAudioTrack(trackId: String)
    fun selectSubtitleTrack(trackId: String?) // null or "-1" for Off
    fun addExternalSubtitle(uri: Uri, selectImmediately: Boolean = true): Boolean

    fun loadQueue(videos: List<VideoItem>, startIndex: Int, fallbackStep: Int = 0)
    fun next()
    fun previous()
    fun hasNext(): Boolean
    fun hasPrevious(): Boolean

    fun attachVideoLayout(layout: VLCVideoLayout)
    fun notifySurfaceReady(layout: VLCVideoLayout, width: Int, height: Int)
    fun detachVideoLayout()

    fun addListener(listener: PlayerEngineListener)
    fun removeListener(listener: PlayerEngineListener)
    fun release()
}
