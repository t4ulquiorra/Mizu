package com.mizu.player.player.engine

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.ui.SubtitleView
import com.mizu.player.domain.model.AudioTrackInfo
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SubtitleTrack
import com.mizu.player.util.PlaybackLog
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
    private var currentPfd: ParcelFileDescriptor? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Maps UI track index to VLC track ID
    private val audioTrackIds = mutableListOf<Int>()
    private val subtitleTrackIds = mutableListOf<Int>()

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            PlaybackLog.logSurfaceEvent("VLC surfaceCreated")
            attachViewsInternal()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            PlaybackLog.logSurfaceEvent("VLC surfaceChanged", width, height)
            mediaPlayer?.vlcVout?.setWindowSize(width, height)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            PlaybackLog.logSurfaceEvent("VLC surfaceDestroyed")
            mediaPlayer?.vlcVout?.detachViews()
        }
    }

    private val eventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                PlaybackLog.logVlcEvent("Opening")
            }
            MediaPlayer.Event.Buffering -> {
                PlaybackLog.logVlcEvent("Buffering", "${event.buffering}%")
                _playbackState.update { it.copy(isBuffering = event.buffering < 100f) }
            }
            MediaPlayer.Event.Playing -> {
                PlaybackLog.logVlcEvent("Playing")
                _playbackState.update {
                    it.copy(
                        isPlaying = true,
                        isBuffering = false,
                        firstFrameRendered = true
                    )
                }
                startProgressTracker()
                updateTracks()
            }
            MediaPlayer.Event.Paused -> {
                PlaybackLog.logVlcEvent("Paused")
                _playbackState.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            }
            MediaPlayer.Event.Stopped -> {
                PlaybackLog.logVlcEvent("Stopped")
                _playbackState.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            }
            MediaPlayer.Event.EndReached -> {
                PlaybackLog.logVlcEvent("EndReached")
                _playbackState.update { it.copy(isPlaying = false, isEnded = true) }
                stopProgressTracker()
            }
            MediaPlayer.Event.LengthChanged -> {
                PlaybackLog.logVlcEvent("LengthChanged", "${event.lengthChanged}ms")
                _playbackState.update { it.copy(duration = event.lengthChanged) }
            }
            MediaPlayer.Event.TimeChanged -> {
                _playbackState.update { it.copy(currentPosition = event.timeChanged) }
            }
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.ESSelected -> {
                PlaybackLog.logVlcEvent("TrackEvent (${event.type})")
                updateTracks()
            }
            MediaPlayer.Event.EncounteredError -> {
                PlaybackLog.logVlcEvent("EncounteredError")
                _playbackState.update { it.copy(error = "VLC playback error", isBuffering = false) }
            }
            MediaPlayer.Event.Vout -> {
                PlaybackLog.logVlcEvent("Vout (${event.voutCount})")
                updateTracks()
            }
        }
    }

    private fun initVlc() {
        if (libVLC == null) {
            PlaybackLog.log("VLC", "Initializing LibVLC instance")
            val options = arrayListOf(
                "--no-sub-autodetect-file",
                "--swscale-mode=0",
                "--network-caching=1500",
                "-vvv" // verbose diagnostic logging
            )
            libVLC = LibVLC(context, options)
            mediaPlayer = MediaPlayer(libVLC).apply {
                setEventListener(eventListener)
            }
            attachViewsInternal()
        }
    }

    private fun attachViewsInternal() {
        val sv = currentSurfaceView ?: return
        val mp = mediaPlayer ?: return
        val vout = mp.vlcVout
        if (!vout.areViewsAttached()) {
            PlaybackLog.logSurfaceEvent("Attaching SurfaceView to VLC vlcVout")
            vout.setVideoView(sv)
            vout.attachViews()
        }
    }

    override fun prepare(uri: Uri, title: String, startPositionMs: Long) {
        initVlc()
        PlaybackLog.logEngineChoice("VLC", uri, "Preparing media: $title, startPos=$startPositionMs")

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

        val vlc = libVLC ?: return
        try {
            // Close any previous descriptor
            currentPfd?.close()
            currentPfd = null

            val media = if (uri.scheme == "content") {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                currentPfd = pfd
                if (pfd != null) {
                    PlaybackLog.log("VLC", "Opened ParcelFileDescriptor for content URI: $uri")
                    Media(vlc, pfd.fileDescriptor).apply {
                        setHWDecoderEnabled(true, false)
                    }
                } else {
                    PlaybackLog.log("VLC", "Failed to open PFD, falling back to direct URI: $uri")
                    Media(vlc, uri).apply {
                        setHWDecoderEnabled(true, false)
                    }
                }
            } else {
                val path = uri.path ?: uri.toString()
                PlaybackLog.log("VLC", "Opening direct path: $path")
                Media(vlc, path).apply {
                    setHWDecoderEnabled(true, false)
                }
            }

            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()

            if (startPositionMs > 0) {
                mediaPlayer?.time = startPositionMs
            }
        } catch (e: Exception) {
            PlaybackLog.log("VLC", "Error preparing media: ${e.message}", e)
            _playbackState.update { it.copy(error = e.message ?: "Failed to open media with VLC") }
        }
    }

    override fun play() {
        PlaybackLog.logVlcEvent("play()")
        mediaPlayer?.play()
    }

    override fun pause() {
        PlaybackLog.logVlcEvent("pause()")
        mediaPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        PlaybackLog.logVlcEvent("seekTo($positionMs)")
        mediaPlayer?.time = positionMs
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        PlaybackLog.logVlcEvent("setPlaybackSpeed($speed)")
        mediaPlayer?.rate = speed
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        val volInt = (volume * 100).toInt().coerceIn(0, 100)
        PlaybackLog.logVlcEvent("setVolume($volInt)")
        mediaPlayer?.volume = volInt
        _playbackState.update { it.copy(volume = volume) }
    }

    override fun selectAudioTrack(index: Int) {
        val mp = mediaPlayer ?: return
        if (index in audioTrackIds.indices) {
            val trackId = audioTrackIds[index]
            PlaybackLog.logVlcEvent("selectAudioTrack", "index=$index, id=$trackId")
            mp.setAudioTrack(trackId)
            _playbackState.update { it.copy(selectedAudioTrack = index) }
        }
    }

    override fun selectSubtitleTrack(index: Int) {
        val mp = mediaPlayer ?: return
        if (index == -1) {
            PlaybackLog.logVlcEvent("selectSubtitleTrack", "disable (-1)")
            mp.setSpuTrack(-1)
            _playbackState.update { it.copy(selectedSubtitleTrack = -1) }
        } else if (index in subtitleTrackIds.indices) {
            val trackId = subtitleTrackIds[index]
            PlaybackLog.logVlcEvent("selectSubtitleTrack", "index=$index, id=$trackId")
            mp.setSpuTrack(trackId)
            _playbackState.update { it.copy(selectedSubtitleTrack = index) }
        }
    }

    override fun addSidecarSubtitle(uri: Uri, mimeType: String, language: String?, label: String?) {
        val mp = mediaPlayer ?: return
        val path = uri.path ?: uri.toString()
        PlaybackLog.logVlcEvent("addSidecarSubtitle", "path=$path")
        // 1 = IMedia.Slave.Type.Subtitle in LibVLC
        mp.addSlave(1, path, true)
        updateTracks()
    }

    override fun setAudioDelay(delayMs: Long) {
        PlaybackLog.logVlcEvent("setAudioDelay", "${delayMs}ms")
        mediaPlayer?.setAudioDelay(delayMs * 1000) // microseconds
        _playbackState.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        PlaybackLog.logVlcEvent("setSubtitleDelay", "${delayMs}ms")
        mediaPlayer?.setSpuDelay(delayMs * 1000) // microseconds
        _playbackState.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        PlaybackLog.logSurfaceEvent("VLC attachSurfaceView")
        currentSurfaceView = surfaceView
        surfaceView.holder.removeCallback(surfaceCallback)
        surfaceView.holder.addCallback(surfaceCallback)
        attachViewsInternal()
    }

    override fun detachSurfaceView() {
        PlaybackLog.logSurfaceEvent("VLC detachSurfaceView")
        currentSurfaceView?.holder?.removeCallback(surfaceCallback)
        mediaPlayer?.vlcVout?.detachViews()
        currentSurfaceView = null
    }

    override fun attachSubtitleView(subtitleView: SubtitleView) {
        // VLC renders styled subtitles directly onto the video surface using libass!
    }

    override fun detachSubtitleView() {
        // No-op for VLC
    }

    override fun release() {
        PlaybackLog.log("VLC", "Releasing VlcPlayerEngine")
        stopProgressTracker()
        detachSurfaceView()
        mediaPlayer?.setEventListener(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentPfd?.close()
        currentPfd = null
        libVLC?.release()
        libVLC = null
        audioTrackIds.clear()
        subtitleTrackIds.clear()
        _playbackState.value = PlayerPlaybackState()
    }

    private fun updateTracks() {
        val mp = mediaPlayer ?: return

        audioTrackIds.clear()
        subtitleTrackIds.clear()

        val currentAudioId = mp.audioTrack
        val currentSpuId = mp.spuTrack

        var selectedAudioIdx = -1
        var selectedSubIdx = -1

        val audioList = mp.audioTracks?.filter { it.id != -1 }?.mapIndexed { index, track ->
            audioTrackIds.add(track.id)
            val isSelected = track.id == currentAudioId
            if (isSelected) {
                selectedAudioIdx = index
            }
            AudioTrackInfo(
                index = index,
                label = track.name ?: "Audio Track ${index + 1}",
                language = null,
                channelCount = 2,
                sampleRate = 44100,
                isSelected = isSelected
            )
        } ?: emptyList()

        val spuList = mp.spuTracks?.filter { it.id != -1 }?.mapIndexed { index, track ->
            subtitleTrackIds.add(track.id)
            val isSelected = track.id == currentSpuId
            if (isSelected) {
                selectedSubIdx = index
            }
            SubtitleTrack(
                index = index,
                label = track.name ?: "Subtitle ${index + 1}",
                language = null,
                mimeType = null,
                isEmbedded = true,
                isSelected = isSelected
            )
        } ?: emptyList()

        PlaybackLog.logVlcEvent("updateTracks", "audioCount=${audioList.size}, subCount=${spuList.size}, selAudio=$selectedAudioIdx, selSub=$selectedSubIdx")

        _playbackState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = spuList,
                selectedAudioTrack = selectedAudioIdx,
                selectedSubtitleTrack = selectedSubIdx
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
