package com.mizu.player.engine

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.media3.common.Player
import com.mizu.player.data.MediaRepository
import com.mizu.player.data.UserPreferences
import com.mizu.player.model.VideoItem
import com.mizu.player.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class VlcPlayerEngine(private val context: Context) : PlayerEngine {

    private val engineScope = CoroutineScope(Dispatchers.Main + Job())
    private val listeners = CopyOnWriteArrayList<PlayerEngineListener>()

    private val libVLC: LibVLC
    private val mediaPlayer: MediaPlayer

    override var isPlaying: Boolean = false
        private set
    override var currentPositionMs: Long = 0L
        private set
    override var durationMs: Long = 0L
        private set
    override var currentVideo: VideoItem? = null
        private set
    override var currentPlaybackSpeed: Float = 1.0f
        private set
    override var currentAspectRatio: AspectRatioMode = AspectRatioMode.FIT
        private set
    override var currentAudioDelayMs: Long = 0L
        private set
    override var currentSubtitleDelayMs: Long = 0L
        private set
    override var currentVolumePercent: Int = 100
        private set
    override var playlist: List<VideoItem> = emptyList()
        private set
    override var currentIndex: Int = 0
        private set

    private var activeFallbackStep: Int = 0
    private var attachedLayout: VLCVideoLayout? = null
    private var isBuffering: Boolean = false

    companion object {
        fun createLibVlcInstance(context: Context): LibVLC {
            // Minimal known-good option set from official LibVLC Android samples and VLC for Android.
            // Verified against options in libvlc.so (VLC 3.x line used by libvlc-all).
            val primaryOptions = arrayListOf(
                "--audio-time-stretch",
                "--sub-autodetect-file"
            )
            val minimalOptions = arrayListOf(
                "--audio-time-stretch"
            )
            val bareOptions = arrayListOf<String>()

            // 1. Primary Attempt: Known-good audio time stretch and subtitle autodetection
            try {
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "Attempting LibVLC init with primary options: $primaryOptions")
                val lib = LibVLC(context, primaryOptions)
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "LibVLC initialized successfully (primary options: $primaryOptions, fallback: NONE)")
                return lib
            } catch (t: Throwable) {
                DiagnosticLogger.log(
                    context,
                    "VlcPlayerEngine.Startup",
                    "Primary options failed (${t.message}) with options: $primaryOptions. Retrying with minimal options: $minimalOptions"
                )
            }

            // 2. Fallback Attempt: Minimal options (audio time stretch only)
            try {
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "Attempting LibVLC init with minimal options: $minimalOptions")
                val lib = LibVLC(context, minimalOptions)
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "LibVLC initialized successfully (fallback: MINIMAL_OPTIONS, options: $minimalOptions)")
                return lib
            } catch (t: Throwable) {
                DiagnosticLogger.log(
                    context,
                    "VlcPlayerEngine.Startup",
                    "Minimal options failed (${t.message}) with options: $minimalOptions. Retrying with bare options: []"
                )
            }

            // 3. Fallback Attempt: Bare options (empty list)
            try {
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "Attempting LibVLC init with bare options: []")
                val lib = LibVLC(context, bareOptions)
                DiagnosticLogger.log(context, "VlcPlayerEngine.Startup", "LibVLC initialized successfully (fallback: BARE_OPTIONS, options: [])")
                return lib
            } catch (t: Throwable) {
                DiagnosticLogger.log(
                    context,
                    "VlcPlayerEngine.Startup",
                    "ALL LibVLC options failed. Tested: [primary: $primaryOptions, minimal: $minimalOptions, bare: []]. Error: ${t.message}"
                )
                throw t
            }
        }
    }

    init {
        libVLC = createLibVlcInstance(context)
        mediaPlayer = MediaPlayer(libVLC)
        setupEventListener()
    }

    private fun setupEventListener() {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    isPlaying = true
                    isBuffering = false
                    durationMs = mediaPlayer.length.coerceAtLeast(0L)
                    notifyPlaybackState(Player.STATE_READY)
                    notifyIsPlaying(true)
                    if (activeFallbackStep > 0) {
                        DiagnosticLogger.logFailsafeEvent(
                            context = context,
                            error = "Recovered",
                            fallbackStep = activeFallbackStep,
                            fixedByStep = activeFallbackStep
                        )
                        activeFallbackStep = 0
                    }
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Event", "Playing, durationMs=$durationMs")
                }
                MediaPlayer.Event.Paused -> {
                    isPlaying = false
                    notifyIsPlaying(false)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Event", "Paused at $currentPositionMs")
                }
                MediaPlayer.Event.Stopped -> {
                    isPlaying = false
                    notifyIsPlaying(false)
                    notifyPlaybackState(Player.STATE_IDLE)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Event", "Stopped")
                }
                MediaPlayer.Event.EndReached -> {
                    isPlaying = false
                    notifyIsPlaying(false)
                    notifyPlaybackState(Player.STATE_ENDED)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Event", "EndReached")
                    handleAutoplayNext()
                }
                MediaPlayer.Event.TimeChanged -> {
                    currentPositionMs = event.timeChanged
                    if (durationMs <= 0L) {
                        durationMs = mediaPlayer.length.coerceAtLeast(0L)
                    }
                    listeners.forEach { it.onTimeChanged(currentPositionMs, durationMs) }
                }
                MediaPlayer.Event.PositionChanged -> {
                    // Update duration if dynamically resolved
                    if (durationMs <= 0L && mediaPlayer.length > 0L) {
                        durationMs = mediaPlayer.length
                    }
                }
                MediaPlayer.Event.Buffering -> {
                    isBuffering = event.buffering < 100f
                    if (isBuffering) {
                        notifyPlaybackState(Player.STATE_BUFFERING)
                    } else if (isPlaying) {
                        notifyPlaybackState(Player.STATE_READY)
                    }
                }
                MediaPlayer.Event.ESAdded,
                MediaPlayer.Event.ESDeleted,
                MediaPlayer.Event.ESSelected -> {
                    engineScope.launch {
                        delay(100)
                        notifyTracksChanged()
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    isPlaying = false
                    notifyIsPlaying(false)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Event", "EncounteredError at pos=$currentPositionMs, step=$activeFallbackStep")
                    handlePlaybackFailure()
                }
            }
        }
    }

    private fun notifyPlaybackState(state: Int) {
        listeners.forEach { it.onPlaybackStateChanged(state) }
    }

    private fun notifyIsPlaying(playing: Boolean) {
        listeners.forEach { it.onIsPlayingChanged(playing) }
    }

    private fun notifyTracksChanged() {
        val audio = getAudioTracks()
        val subs = getSubtitleTracks()
        listeners.forEach { it.onTracksChanged(audio, subs) }
    }

    override fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        currentPositionMs = safePos
        mediaPlayer.time = safePos
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed.coerceIn(0.25f, 4.0f)
        mediaPlayer.rate = currentPlaybackSpeed
    }

    override fun setAspectRatio(mode: AspectRatioMode) {
        currentAspectRatio = mode
        when (mode) {
            AspectRatioMode.FIT -> {
                mediaPlayer.aspectRatio = null
                mediaPlayer.scale = 0f
            }
            AspectRatioMode.FILL -> {
                mediaPlayer.aspectRatio = null
                mediaPlayer.scale = 1.35f
            }
            AspectRatioMode.ZOOM -> {
                mediaPlayer.aspectRatio = null
                mediaPlayer.scale = 1.0f
            }
            AspectRatioMode.SIXTEEN_NINE -> {
                mediaPlayer.aspectRatio = "16:9"
            }
            AspectRatioMode.FOUR_THREE -> {
                mediaPlayer.aspectRatio = "4:3"
            }
        }
    }

    override fun setAudioDelay(delayMs: Long) {
        currentAudioDelayMs = delayMs
        mediaPlayer.setAudioDelay(delayMs * 1000L) // libVLC uses microseconds
    }

    override fun setSubtitleDelay(delayMs: Long) {
        currentSubtitleDelayMs = delayMs
        mediaPlayer.setSpuDelay(delayMs * 1000L) // libVLC uses microseconds
    }

    override fun setVolumeBoost(volumePercent: Int) {
        currentVolumePercent = volumePercent.coerceIn(0, 200)
        mediaPlayer.volume = currentVolumePercent
    }

    override fun getAudioTracks(): List<AudioTrackInfo> {
        val currentSelectedId = mediaPlayer.audioTrack
        val tracks = mediaPlayer.audioTracks ?: return emptyList()
        return tracks.mapIndexed { idx, track ->
            val lang = parseLanguageCode(track.name)
            AudioTrackInfo(
                id = track.id.toString(),
                index = idx,
                name = track.name ?: "Audio Track ${idx + 1}",
                language = lang,
                codec = "",
                channels = "",
                isSelected = (track.id == currentSelectedId)
            )
        }
    }

    override fun getSubtitleTracks(): List<SubtitleTrackInfo> {
        val currentSelectedId = mediaPlayer.spuTrack
        val tracks = mediaPlayer.spuTracks ?: return emptyList()
        return tracks.mapIndexed { idx, track ->
            val lang = parseLanguageCode(track.name)
            SubtitleTrackInfo(
                id = track.id.toString(),
                index = idx,
                name = if (track.id == -1) "Off" else (track.name ?: "Subtitle ${idx + 1}"),
                language = lang,
                isSelected = (track.id == currentSelectedId),
                isExternal = false
            )
        }
    }

    override fun selectAudioTrack(trackId: String) {
        val id = trackId.toIntOrNull() ?: return
        mediaPlayer.setAudioTrack(id)
        currentVideo?.let { v ->
            val key = UserPreferences.getVideoKey(v)
            UserPreferences.setSavedAudioTrack(context, key, trackId)
        }
        notifyTracksChanged()
    }

    override fun selectSubtitleTrack(trackId: String?) {
        val id = if (trackId == null || trackId == "-1") -1 else trackId.toIntOrNull() ?: -1
        mediaPlayer.setSpuTrack(id)
        currentVideo?.let { v ->
            val key = UserPreferences.getVideoKey(v)
            UserPreferences.setSavedSubtitleTrack(
                context, key,
                if (id == -1) UserPreferences.SUBTITLE_OFF else trackId ?: UserPreferences.SUBTITLE_OFF
            )
        }
        notifyTracksChanged()
    }

    override fun addExternalSubtitle(uri: Uri, selectImmediately: Boolean): Boolean {
        return try {
            val success = mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, uri, selectImmediately)
            if (success) {
                engineScope.launch {
                    delay(200)
                    notifyTracksChanged()
                }
            }
            DiagnosticLogger.log(context, "VlcPlayerEngine.addExternalSubtitle", "uri=$uri, success=$success")
            success
        } catch (e: Exception) {
            DiagnosticLogger.log(context, "VlcPlayerEngine.addExternalSubtitle_error", e.message ?: "error")
            false
        }
    }

    override fun loadQueue(videos: List<VideoItem>, startIndex: Int, fallbackStep: Int) {
        if (videos.isEmpty()) return
        playlist = videos
        currentIndex = startIndex.coerceIn(0, videos.size - 1)
        activeFallbackStep = fallbackStep

        val video = playlist[currentIndex]
        currentVideo = video
        listeners.forEach { it.onMediaItemTransition(video, currentIndex) }

        playVideoInternal(video, seekPos = 0L, fallbackStep = fallbackStep)
    }

    private fun playVideoInternal(video: VideoItem, seekPos: Long, fallbackStep: Int) {
        engineScope.launch(Dispatchers.IO) {
            val isMissing = video.path.isNotEmpty() && !File(video.path).exists()
            if (isMissing) {
                withContext(Dispatchers.Main) {
                    MediaRepository.removeVideo(context, video)
                    listeners.forEach { it.onError("This file no longer exists", false) }
                }
                return@launch
            }

            // Save resume position if seekPos is 0 and saved position exists
            val savedPos = if (seekPos > 0L) {
                seekPos
            } else {
                val s = MediaRepository.getSavedPosition(video)
                if (s > 3000L && (video.durationMs <= 0 || s < video.durationMs - 5000L)) s else 0L
            }

            try {
                val media = Media(libVLC, video.uri)

                // Requirement: Use libVLC's real hardware decoder API: setHWDecoderEnabled
                // Step 4 of failsafe: hardware decoding disabled (software fallback)
                if (fallbackStep == 4) {
                    media.setHWDecoderEnabled(false, false)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Decoder", "Using SOFTWARE decoder (fallbackStep=4)")
                } else {
                    media.setHWDecoderEnabled(true, false)
                    DiagnosticLogger.log(context, "VlcPlayerEngine.Decoder", "Using HARDWARE decoder")
                }

                media.addOption(":file-caching=1000")
                media.addOption(":network-caching=1500")

                withContext(Dispatchers.Main) {
                    mediaPlayer.media = media
                    media.release()

                    // Reattach video layout if previously detached
                    attachedLayout?.let {
                        if (!mediaPlayer.vlcVout.areViewsAttached()) {
                            mediaPlayer.attachViews(it, null, false, false)
                        }
                    }

                    // Auto-load companion subtitle files (.srt, .ass, .ssa, .vtt, .sub, .idx, .smi)
                    // unless fallbackStep >= 2 (step 2 disables external companion subtitles)
                    if (fallbackStep < 2 && video.path.isNotEmpty()) {
                        val companionSubs = findCompanionSubtitleFiles(File(video.path))
                        for (subFile in companionSubs) {
                            val subUri = Uri.fromFile(subFile)
                            mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, subUri, false)
                        }
                    }

                    // Apply track choices & priorities
                    applyTrackPreferences(video, fallbackStep)

                    // Seek to initial position if resuming
                    if (savedPos > 0L) {
                        currentPositionMs = savedPos
                        mediaPlayer.time = savedPos
                    }

                    mediaPlayer.play()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    DiagnosticLogger.log(context, "VlcPlayerEngine.playError", e.message ?: "error")
                    handlePlaybackFailure()
                }
            }
        }
    }

    private fun applyTrackPreferences(video: VideoItem, fallbackStep: Int) {
        val key = UserPreferences.getVideoKey(video)

        // 1. Audio track (Step 3 disables saved audio track and falls back to default)
        if (fallbackStep < 3) {
            val savedAudio = UserPreferences.getSavedAudioTrack(context, key)
            if (savedAudio != null) {
                val id = savedAudio.toIntOrNull()
                if (id != null) mediaPlayer.setAudioTrack(id)
            } else {
                // Apply language priority preferences
                val preferredLangs = UserPreferences.getPreferredAudioLanguages(context)
                mediaPlayer.audioTracks?.let { tracks ->
                    for (pref in preferredLangs) {
                        val matched = tracks.find { matchLanguage(it.name, pref) }
                        if (matched != null) {
                            mediaPlayer.setAudioTrack(matched.id)
                            break
                        }
                    }
                }
            }
        }

        // 2. Subtitle track (Step 1 disables all subtitles)
        if (fallbackStep >= 1) {
            mediaPlayer.setSpuTrack(-1)
        } else {
            val savedSub = UserPreferences.getSavedSubtitleTrack(context, key)
            if (savedSub == UserPreferences.SUBTITLE_OFF) {
                mediaPlayer.setSpuTrack(-1)
            } else if (savedSub != null) {
                val id = savedSub.toIntOrNull()
                if (id != null) mediaPlayer.setSpuTrack(id)
            } else {
                // Apply language priority preferences
                val preferredLangs = UserPreferences.getPreferredSubtitleLanguages(context)
                mediaPlayer.spuTracks?.let { tracks ->
                    for (pref in preferredLangs) {
                        val matched = tracks.find { it.id != -1 && matchLanguage(it.name, pref) }
                        if (matched != null) {
                            mediaPlayer.setSpuTrack(matched.id)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun findCompanionSubtitleFiles(videoFile: File): List<File> {
        val parent = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension.lowercase()
        val extensions = setOf("srt", "ass", "ssa", "vtt", "sub", "idx", "smi")

        val files = parent.listFiles() ?: return emptyList()
        val companionList = mutableListOf<File>()

        for (file in files) {
            if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in extensions) {
                    val fileBase = file.nameWithoutExtension.lowercase()
                    // Exact match: Video.srt
                    // Language suffix: Video.en.srt, Video_jp.ass, Video.fra.vtt
                    if (fileBase == baseName || fileBase.startsWith("$baseName.") || fileBase.startsWith("${baseName}_")) {
                        companionList.add(file)
                    }
                }
            }
        }
        return companionList
    }

    private fun handlePlaybackFailure() {
        val video = currentVideo ?: return
        val currentPos = currentPositionMs
        val key = UserPreferences.getVideoKey(video)

        DiagnosticLogger.logFailsafeEvent(
            context = context,
            error = "VLC playback failure",
            container = video.path.substringAfterLast('.', ""),
            audioTrack = mediaPlayer.audioTrack.toString(),
            subtitleTrack = mediaPlayer.spuTrack.toString(),
            fallbackStep = activeFallbackStep
        )

        when (activeFallbackStep) {
            0 -> {
                // Step 1: Retry without subtitles
                activeFallbackStep = 1
                listeners.forEach { it.onNotice("Subtitles couldn't be loaded, retrying...") }
                // Blacklist current subtitle track
                UserPreferences.blacklistSubtitleTrack(context, key, mediaPlayer.spuTrack.toString())
                playVideoInternal(video, currentPos, fallbackStep = 1)
            }
            1 -> {
                // Step 2: Retry without external subtitle files
                activeFallbackStep = 2
                listeners.forEach { it.onNotice("External subtitles couldn't be loaded, retrying...") }
                playVideoInternal(video, currentPos, fallbackStep = 2)
            }
            2 -> {
                // Step 3: Retry without saved audio track
                activeFallbackStep = 3
                listeners.forEach { it.onNotice("Audio track couldn't be played, resetting audio...") }
                UserPreferences.blacklistAudioTrack(context, key, mediaPlayer.audioTrack.toString())
                playVideoInternal(video, currentPos, fallbackStep = 3)
            }
            3 -> {
                // Step 4: Retry with software decoding only
                activeFallbackStep = 4
                listeners.forEach { it.onNotice("Hardware decoding failed, switching to software decoding...") }
                playVideoInternal(video, currentPos, fallbackStep = 4)
            }
            else -> {
                // All 4 fallback steps failed -> show error card with Back
                DiagnosticLogger.log(context, "VlcPlayerEngine.Failsafe", "All fallback steps exhausted for ${video.title}")
                listeners.forEach { it.onError("This video can't be played", false) }
            }
        }
    }

    private fun handleAutoplayNext() {
        if (hasNext()) {
            next()
        } else {
            notifyPlaybackState(Player.STATE_ENDED)
        }
    }

    override fun next() {
        if (hasNext()) {
            currentIndex++
            val nextVideo = playlist[currentIndex]
            currentVideo = nextVideo
            activeFallbackStep = 0
            listeners.forEach { it.onMediaItemTransition(nextVideo, currentIndex) }
            playVideoInternal(nextVideo, seekPos = 0L, fallbackStep = 0)
        }
    }

    override fun previous() {
        if (hasPrevious()) {
            currentIndex--
            val prevVideo = playlist[currentIndex]
            currentVideo = prevVideo
            activeFallbackStep = 0
            listeners.forEach { it.onMediaItemTransition(prevVideo, currentIndex) }
            playVideoInternal(prevVideo, seekPos = 0L, fallbackStep = 0)
        }
    }

    override fun hasNext(): Boolean = currentIndex < playlist.size - 1
    override fun hasPrevious(): Boolean = currentIndex > 0

    override fun attachVideoLayout(layout: VLCVideoLayout) {
        attachedLayout = layout
        if (!mediaPlayer.vlcVout.areViewsAttached()) {
            mediaPlayer.attachViews(layout, null, false, false)
        }
    }

    override fun detachVideoLayout() {
        if (mediaPlayer.vlcVout.areViewsAttached()) {
            mediaPlayer.detachViews()
        }
        attachedLayout = null
    }

    override fun addListener(listener: PlayerEngineListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlayerEngineListener) {
        listeners.remove(listener)
    }

    override fun release() {
        detachVideoLayout()
        mediaPlayer.stop()
        mediaPlayer.release()
        libVLC.release()
    }

    private fun parseLanguageCode(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val lower = name.lowercase()
        return when {
            lower.contains("eng") -> "en"
            lower.contains("jap") -> "ja"
            lower.contains("spa") -> "es"
            lower.contains("fre") || lower.contains("fra") -> "fr"
            lower.contains("ger") || lower.contains("deu") -> "de"
            lower.contains("ita") -> "it"
            lower.contains("chi") || lower.contains("zho") -> "zh"
            lower.contains("rus") -> "ru"
            lower.contains("hin") -> "hi"
            else -> ""
        }
    }

    private fun matchLanguage(trackName: String?, prefLang: String): Boolean {
        if (trackName.isNullOrBlank()) return false
        val cleanTrack = trackName.trim().lowercase()
        val cleanPref = prefLang.trim().lowercase()
        if (cleanTrack.contains(cleanPref)) return true
        return try {
            val locPref = Locale(cleanPref)
            val iso3 = locPref.isO3Language.lowercase()
            cleanTrack.contains(iso3) || cleanTrack.contains(locPref.displayLanguage.lowercase())
        } catch (_: Exception) {
            false
        }
    }
}
