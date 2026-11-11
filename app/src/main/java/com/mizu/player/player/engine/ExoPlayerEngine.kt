package com.mizu.player.player.engine

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.mizu.player.domain.model.AudioTrackInfo
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SubtitleTrack
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

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> _playbackState.update { it.copy(isBuffering = true) }
                Player.STATE_READY -> _playbackState.update {
                    it.copy(
                        isBuffering = false,
                        duration = player?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
                Player.STATE_ENDED -> _playbackState.update { it.copy(isEnded = true, isPlaying = false) }
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressTracker() else stopProgressTracker()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update { it.copy(error = error.message ?: "Playback error", isBuffering = false) }
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            _playbackState.update {
                it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val audioTracks = mutableListOf<AudioTrackInfo>()
            val subtitleTracks = mutableListOf<SubtitleTrack>()

            for (group in tracks.groups) {
                val type = group.type
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (type == C.TRACK_TYPE_AUDIO) {
                        audioTracks.add(
                            AudioTrackInfo(
                                index = audioTracks.size,
                                label = format.label ?: "Audio ${audioTracks.size + 1}",
                                language = format.language,
                                channelCount = format.channelCount,
                                sampleRate = format.sampleRate
                            )
                        )
                    } else if (type == C.TRACK_TYPE_TEXT) {
                        subtitleTracks.add(
                            SubtitleTrack(
                                index = subtitleTracks.size,
                                label = format.label ?: "Subtitle ${subtitleTracks.size + 1}",
                                language = format.language,
                                mimeType = format.sampleMimeType,
                                isEmbedded = true
                            )
                        )
                    }
                }
            }

            _playbackState.update {
                it.copy(
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks
                )
            }
        }
    }

    private fun initPlayer() {
        if (player == null) {
            val renderersFactory = NextRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

            player = ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .build().apply {
                    addListener(playerListener)
                }
        }
    }

    override fun prepare(uri: Uri, title: String, startPositionMs: Long) {
        initPlayer()
        _playbackState.update {
            it.copy(
                uri = uri,
                title = title,
                error = null,
                isEnded = false,
                currentPosition = startPositionMs
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
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume
        _playbackState.update { it.copy(volume = volume) }
    }

    override fun selectAudioTrack(index: Int) {
        // Track selection
        _playbackState.update { it.copy(selectedAudioTrack = index) }
    }

    override fun selectSubtitleTrack(index: Int) {
        _playbackState.update { it.copy(selectedSubtitleTrack = index) }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        player?.setVideoSurfaceView(surfaceView)
    }

    override fun detachSurfaceView() {
        player?.clearVideoSurface()
    }

    override fun release() {
        stopProgressTracker()
        player?.removeListener(playerListener)
        player?.release()
        player = null
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
