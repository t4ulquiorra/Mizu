package com.mizu.player.player.engine

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import com.mizu.player.domain.model.AudioTrackInfo
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SubtitleTrack
import dagger.hilt.android.qualifiers.ApplicationContext
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
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import javax.inject.Inject

class VlcPlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerEngineInterface {

    override val engineType: PlayerEngine = PlayerEngine.VLC

    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    override val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurfaceView: SurfaceView? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val eventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                _playbackState.update { it.copy(isPlaying = true, isBuffering = false) }
                startProgressTracker()
            }
            MediaPlayer.Event.Paused -> {
                _playbackState.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            }
            MediaPlayer.Event.Stopped -> {
                _playbackState.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            }
            MediaPlayer.Event.EndReached -> {
                _playbackState.update { it.copy(isPlaying = false, isEnded = true) }
                stopProgressTracker()
            }
            MediaPlayer.Event.Buffering -> {
                _playbackState.update { it.copy(isBuffering = event.buffering < 100f) }
            }
            MediaPlayer.Event.LengthChanged -> {
                _playbackState.update { it.copy(duration = event.lengthChanged) }
            }
            MediaPlayer.Event.EncounteredError -> {
                _playbackState.update { it.copy(error = "VLC playback error", isBuffering = false) }
            }
            MediaPlayer.Event.Vout -> {
                updateTracks()
            }
        }
    }

    private fun initVlc() {
        if (libVLC == null) {
            val options = arrayListOf(
                "--no-sub-autodetect-file",
                "--swscale-mode=0",
                "--network-caching=1500"
            )
            libVLC = LibVLC(context, options)
            mediaPlayer = MediaPlayer(libVLC).apply {
                setEventListener(eventListener)
            }
            currentSurfaceView?.let { attachSurfaceView(it) }
        }
    }

    override fun prepare(uri: Uri, title: String, startPositionMs: Long) {
        initVlc()
        _playbackState.update {
            it.copy(
                uri = uri,
                title = title,
                error = null,
                isEnded = false,
                currentPosition = startPositionMs
            )
        }
        val vlc = libVLC ?: return
        val media = Media(vlc, uri).apply {
            setHWDecoderEnabled(true, false)
        }
        mediaPlayer?.media = media
        media.release()
        mediaPlayer?.play()

        if (startPositionMs > 0) {
            mediaPlayer?.time = startPositionMs
        }
    }

    override fun play() {
        mediaPlayer?.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer?.time = positionMs
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.rate = speed
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.volume = (volume * 100).toInt().coerceIn(0, 100)
        _playbackState.update { it.copy(volume = volume) }
    }

    override fun selectAudioTrack(index: Int) {
        val tracks = mediaPlayer?.audioTracks ?: return
        if (index in tracks.indices) {
            mediaPlayer?.setAudioTrack(tracks[index].id)
            _playbackState.update { it.copy(selectedAudioTrack = index) }
        }
    }

    override fun selectSubtitleTrack(index: Int) {
        val tracks = mediaPlayer?.spuTracks ?: return
        if (index in tracks.indices) {
            mediaPlayer?.setSpuTrack(tracks[index].id)
            _playbackState.update { it.copy(selectedSubtitleTrack = index) }
        }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        currentSurfaceView = surfaceView
        mediaPlayer?.vlcVout?.apply {
            setVideoView(surfaceView)
            attachViews()
        }
    }

    override fun detachSurfaceView() {
        mediaPlayer?.vlcVout?.detachViews()
        currentSurfaceView = null
    }

    override fun release() {
        stopProgressTracker()
        mediaPlayer?.vlcVout?.detachViews()
        mediaPlayer?.setEventListener(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
        _playbackState.value = PlayerPlaybackState()
    }

    private fun updateTracks() {
        val mp = mediaPlayer ?: return

        val audioList = mp.audioTracks?.mapIndexed { index, track ->
            AudioTrackInfo(
                index = index,
                label = track.name ?: "Audio $index",
                language = null,
                channelCount = 2,
                sampleRate = 44100
            )
        } ?: emptyList()

        val spuList = mp.spuTracks?.mapIndexed { index, track ->
            SubtitleTrack(
                index = index,
                label = track.name ?: "Subtitle $index",
                language = null,
                mimeType = null,
                isEmbedded = true
            )
        } ?: emptyList()

        _playbackState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = spuList
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    _playbackState.update {
                        it.copy(
                            currentPosition = mp.time,
                            duration = mp.length.coerceAtLeast(0L)
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
