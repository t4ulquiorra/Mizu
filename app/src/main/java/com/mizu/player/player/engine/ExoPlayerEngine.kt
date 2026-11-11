package com.mizu.player.player.engine

import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.mizu.player.domain.model.AudioTrackInfo
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SubtitleTrack
import com.mizu.player.util.PlaybackLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(UnstableApi::class)
class ExoPlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerEngineInterface {

    override val engineType: PlayerEngine = PlayerEngine.EXOPLAYER

    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    override val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentSurfaceView: SurfaceView? = null
    private var currentSubtitleView: SubtitleView? = null
    private var currentMediaUri: Uri? = null

    // Track selection mappings
    private val audioTrackMap = mutableListOf<Pair<Tracks.Group, Int>>()
    private val subtitleTrackMap = mutableListOf<Pair<Tracks.Group, Int>>()

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            PlaybackLog.logSurfaceEvent("surfaceCreated (holder valid=${holder.surface.isValid})")
            player?.setVideoSurfaceHolder(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            PlaybackLog.logSurfaceEvent("surfaceChanged", width, height)
            player?.setVideoSurfaceHolder(holder)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            PlaybackLog.logSurfaceEvent("surfaceDestroyed")
            player?.clearVideoSurfaceHolder(holder)
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            PlaybackLog.logDecoderInit("VIDEO", decoderName, null)
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            PlaybackLog.logDecoderInit("AUDIO", decoderName, null)
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            if (droppedFrames > 10) {
                PlaybackLog.log("ExoPlayer", "Dropped $droppedFrames frames in ${elapsedMs}ms")
            }
        }

        override fun onVideoCodecError(
            eventTime: AnalyticsListener.EventTime,
            videoCodecError: Exception
        ) {
            PlaybackLog.logDecoderError("VIDEO", "Video codec exception: ${videoCodecError.message}", videoCodecError)
        }

        override fun onAudioCodecError(
            eventTime: AnalyticsListener.EventTime,
            audioCodecError: Exception
        ) {
            PlaybackLog.logDecoderError("AUDIO", "Audio codec exception: ${audioCodecError.message}", audioCodecError)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    PlaybackLog.log("ExoPlayer", "STATE_BUFFERING")
                    _playbackState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    val dur = player?.duration?.coerceAtLeast(0L) ?: 0L
                    PlaybackLog.log("ExoPlayer", "STATE_READY, duration=$dur")
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            duration = dur
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    PlaybackLog.log("ExoPlayer", "STATE_ENDED")
                    _playbackState.update { it.copy(isEnded = true, isPlaying = false) }
                }
                Player.STATE_IDLE -> {
                    PlaybackLog.log("ExoPlayer", "STATE_IDLE")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            PlaybackLog.log("ExoPlayer", "onIsPlayingChanged: $isPlaying")
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressTracker() else stopProgressTracker()
        }

        override fun onRenderedFirstFrame() {
            PlaybackLog.log("ExoPlayer", "First video frame rendered!")
            _playbackState.update { it.copy(firstFrameRendered = true) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val msg = error.message ?: "ExoPlayer playback error"
            PlaybackLog.log("ExoPlayer", "PlaybackException (code=${error.errorCode}): $msg", error)
            _playbackState.update { it.copy(error = msg, isBuffering = false) }
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            PlaybackLog.log("ExoPlayer", "VideoSizeChanged: ${videoSize.width}x${videoSize.height}")
            _playbackState.update {
                it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
            }
        }

        override fun onCues(cueGroup: CueGroup) {
            currentSubtitleView?.setCues(cueGroup.cues)
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTracksInternal(tracks)
        }
    }

    private fun updateTracksInternal(tracks: Tracks) {
        val audioTracks = mutableListOf<AudioTrackInfo>()
        val subtitleTracks = mutableListOf<SubtitleTrack>()
        audioTrackMap.clear()
        subtitleTrackMap.clear()

        var selectedAudioIdx = -1
        var selectedSubIdx = -1

        for (group in tracks.groups) {
            val type = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)

                if (type == C.TRACK_TYPE_AUDIO) {
                    val lang = format.language?.let {
                        try { Locale(it).displayLanguage } catch (_: Exception) { it }
                    } ?: "Audio"
                    val channels = when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1"
                        8 -> "7.1"
                        else -> if (format.channelCount > 0) "${format.channelCount}ch" else ""
                    }
                    val codec = format.sampleMimeType?.substringAfterLast('/')?.uppercase() ?: ""
                    val label = format.label ?: listOf(lang, codec, channels).filter { it.isNotEmpty() }.joinToString(" - ")

                    val trackInfo = AudioTrackInfo(
                        index = audioTracks.size,
                        label = label.ifEmpty { "Audio Track ${audioTracks.size + 1}" },
                        language = format.language,
                        channelCount = format.channelCount,
                        sampleRate = format.sampleRate,
                        codec = codec,
                        isSelected = isSelected
                    )
                    if (isSelected) {
                        selectedAudioIdx = audioTracks.size
                    }
                    audioTrackMap.add(Pair(group, i))
                    audioTracks.add(trackInfo)
                } else if (type == C.TRACK_TYPE_TEXT) {
                    val lang = format.language?.let {
                        try { Locale(it).displayLanguage } catch (_: Exception) { it }
                    } ?: "Subtitle"
                    val label = format.label ?: "$lang ${subtitleTracks.size + 1}"

                    val trackInfo = SubtitleTrack(
                        index = subtitleTracks.size,
                        label = label,
                        language = format.language,
                        mimeType = format.sampleMimeType,
                        isEmbedded = true,
                        isSelected = isSelected
                    )
                    if (isSelected) {
                        selectedSubIdx = subtitleTracks.size
                    }
                    subtitleTrackMap.add(Pair(group, i))
                    subtitleTracks.add(trackInfo)
                }
            }
        }

        PlaybackLog.log("ExoPlayer", "Tracks updated: ${audioTracks.size} audio, ${subtitleTracks.size} text (selAudio=$selectedAudioIdx, selSub=$selectedSubIdx)")

        _playbackState.update {
            it.copy(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                selectedAudioTrack = selectedAudioIdx,
                selectedSubtitleTrack = selectedSubIdx
            )
        }
    }

    private fun initPlayer() {
        if (player == null) {
            PlaybackLog.log("ExoPlayer", "Initializing ExoPlayer with NextRenderersFactory")
            val renderersFactory = NextRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true)

            player = ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .build().apply {
                    addListener(playerListener)
                    addAnalyticsListener(analyticsListener)
                }

            // Immediately attach surface view if available
            currentSurfaceView?.let { sv ->
                PlaybackLog.log("ExoPlayer", "Attaching pending SurfaceView to new player")
                player?.setVideoSurfaceView(sv)
                sv.holder.removeCallback(surfaceCallback)
                sv.holder.addCallback(surfaceCallback)
            }
        }
    }

    override fun prepare(uri: Uri, title: String, startPositionMs: Long) {
        currentMediaUri = uri
        initPlayer()
        PlaybackLog.logEngineChoice("ExoPlayer", uri, "Preparing media: $title, startPos=$startPositionMs")

        _playbackState.update {
            it.copy(
                uri = uri,
                title = title,
                error = null,
                isEnded = false,
                currentPosition = startPositionMs,
                firstFrameRendered = false
            )
        }

        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
            playWhenReady = true
        }
    }

    override fun play() {
        PlaybackLog.log("ExoPlayer", "play() requested")
        player?.play()
    }

    override fun pause() {
        PlaybackLog.log("ExoPlayer", "pause() requested")
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        PlaybackLog.log("ExoPlayer", "seekTo: $positionMs")
        player?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        PlaybackLog.log("ExoPlayer", "setPlaybackSpeed: $speed")
        player?.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume
        _playbackState.update { it.copy(volume = volume) }
    }

    override fun selectAudioTrack(index: Int) {
        val p = player ?: return
        if (index in audioTrackMap.indices) {
            val (group, trackIdx) = audioTrackMap[index]
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackIdx)
            PlaybackLog.log("ExoPlayer", "Selecting audio track $index (group=${group.mediaTrackGroup.id}, idx=$trackIdx)")
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(override)
                .build()
        }
    }

    override fun selectSubtitleTrack(index: Int) {
        val p = player ?: return
        if (index == -1) {
            // Disable subtitles
            PlaybackLog.log("ExoPlayer", "Disabling subtitles")
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
            currentSubtitleView?.setCues(emptyList())
        } else if (index in subtitleTrackMap.indices) {
            val (group, trackIdx) = subtitleTrackMap[index]
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackIdx)
            PlaybackLog.log("ExoPlayer", "Selecting subtitle track $index (group=${group.mediaTrackGroup.id}, idx=$trackIdx)")
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
    }

    override fun addSidecarSubtitle(uri: Uri, mimeType: String, language: String?, label: String?) {
        val currentUri = currentMediaUri ?: return
        val pos = player?.currentPosition ?: 0L
        PlaybackLog.log("ExoPlayer", "Adding sidecar subtitle: $uri (mime=$mimeType, lang=$language, label=$label)")

        val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage(language)
            .setLabel(label ?: "External Subtitle")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(currentUri)
            .setSubtitleConfigurations(listOf(subConfig))
            .build()

        player?.apply {
            setMediaItem(mediaItem, pos)
            prepare()
            play()
        }
    }

    override fun setAudioDelay(delayMs: Long) {
        PlaybackLog.log("ExoPlayer", "setAudioDelay: $delayMs")
        _playbackState.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        PlaybackLog.log("ExoPlayer", "setSubtitleDelay: $delayMs")
        _playbackState.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        PlaybackLog.logSurfaceEvent("attachSurfaceView called")
        currentSurfaceView = surfaceView
        surfaceView.holder.removeCallback(surfaceCallback)
        surfaceView.holder.addCallback(surfaceCallback)

        player?.let { p ->
            PlaybackLog.logSurfaceEvent("Attaching SurfaceView to active ExoPlayer instance")
            p.setVideoSurfaceView(surfaceView)
        }
    }

    override fun detachSurfaceView() {
        PlaybackLog.logSurfaceEvent("detachSurfaceView called")
        currentSurfaceView?.holder?.removeCallback(surfaceCallback)
        player?.clearVideoSurfaceView(currentSurfaceView)
        currentSurfaceView = null
    }

    override fun attachSubtitleView(subtitleView: SubtitleView) {
        currentSubtitleView = subtitleView
        subtitleView.setUserDefaultStyle()
        subtitleView.setUserDefaultTextSize()
        subtitleView.setApplyEmbeddedStyles(true)
        subtitleView.setApplyEmbeddedFontSizes(true)
        subtitleView.setStyle(
            CaptionStyleCompat(
                android.graphics.Color.WHITE,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                android.graphics.Color.BLACK,
                null
            )
        )
    }

    override fun detachSubtitleView() {
        currentSubtitleView?.setCues(emptyList())
        currentSubtitleView = null
    }

    override fun release() {
        PlaybackLog.log("ExoPlayer", "Releasing ExoPlayerEngine")
        stopProgressTracker()
        player?.removeListener(playerListener)
        player?.removeAnalyticsListener(analyticsListener)
        detachSurfaceView()
        detachSubtitleView()
        player?.release()
        player = null
        currentMediaUri = null
        audioTrackMap.clear()
        subtitleTrackMap.clear()
        _playbackState.value = PlayerPlaybackState()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                player?.let { p ->
                    _playbackState.update {
                        it.copy(
                            currentPosition = p.currentPosition,
                            duration = p.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
