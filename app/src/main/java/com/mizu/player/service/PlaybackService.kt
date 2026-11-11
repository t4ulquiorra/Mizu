package com.mizu.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mizu.player.MainActivity
import com.mizu.player.R
import com.mizu.player.data.MediaRepository
import com.mizu.player.engine.AspectRatioMode
import com.mizu.player.engine.AudioTrackInfo
import com.mizu.player.engine.PlayerEngine
import com.mizu.player.engine.PlayerEngineListener
import com.mizu.player.engine.SubtitleTrackInfo
import com.mizu.player.engine.VlcPlayerEngine
import com.mizu.player.model.VideoItem
import com.mizu.player.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var playerEngine: PlayerEngine? = null
    private var mediaSession: MediaSession? = null
    private var sessionAdapter: VlcMediaSessionAdapter? = null
    private var audioFocusHelper: AudioFocusHelper? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                playerEngine?.let { engine ->
                    if (engine.isPlaying) {
                        engine.pause()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_QUEUE = "com.mizu.player.action.PLAY_QUEUE"
        const val ACTION_STOP = "com.mizu.player.action.STOP"
        const val EXTRA_START_INDEX = "extra_start_index"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "mizu_playback_channel"

        // Observable state for Compose UI & PiP
        var engine by mutableStateOf<PlayerEngine?>(null)
            private set
        var currentVideo by mutableStateOf<VideoItem?>(null)
            private set
        var playlist by mutableStateOf<List<VideoItem>>(emptyList())
            private set
        var currentIndex by mutableIntStateOf(0)
            private set
        var isPlaying by mutableStateOf(false)
            private set
        var currentPositionMs by mutableLongStateOf(0L)
            private set
        var durationMs by mutableLongStateOf(0L)
            private set
        var currentNotice by mutableStateOf<String?>(null)
            private set
        var errorMessage by mutableStateOf<String?>(null)
        var hasPrevious by mutableStateOf(false)
            private set
        var hasNext by mutableStateOf(false)
            private set

        var availableAudioTracks by mutableStateOf<List<AudioTrackInfo>>(emptyList())
            private set
        var availableSubtitleTracks by mutableStateOf<List<SubtitleTrackInfo>>(emptyList())
            private set

        var currentPlaybackSpeed by mutableFloatStateOf(1.0f)
            private set
        var currentAspectRatio by mutableStateOf(AspectRatioMode.FIT)
            private set
        var currentAudioDelayMs by mutableLongStateOf(0L)
            private set
        var currentSubtitleDelayMs by mutableLongStateOf(0L)
            private set
        var currentVolumePercent by mutableIntStateOf(100)
            private set

        fun start(context: Context, videos: List<VideoItem>, startIndex: Int) {
            val target = videos.getOrNull(startIndex)
            DiagnosticLogger.log(context, "PlaybackService.start", "count=${videos.size}, startIndex=$startIndex, video=${target?.title}")
            playlist = videos
            currentIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
            currentVideo = target
            errorMessage = null
            currentNotice = null

            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_PLAY_QUEUE
                putExtra(EXTRA_START_INDEX, currentIndex)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            DiagnosticLogger.log(context, "PlaybackService.stop", "requested")
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun play() = engine?.play()
        fun pause() = engine?.pause()
        fun seekTo(posMs: Long) = engine?.seekTo(posMs)
        fun next() = engine?.next()
        fun previous() = engine?.previous()

        fun setPlaybackSpeed(speed: Float) {
            currentPlaybackSpeed = speed
            engine?.setPlaybackSpeed(speed)
        }

        fun setAspectRatio(mode: AspectRatioMode) {
            currentAspectRatio = mode
            engine?.setAspectRatio(mode)
        }

        fun setAudioDelay(delayMs: Long) {
            currentAudioDelayMs = delayMs
            engine?.setAudioDelay(delayMs)
        }

        fun setSubtitleDelay(delayMs: Long) {
            currentSubtitleDelayMs = delayMs
            engine?.setSubtitleDelay(delayMs)
        }

        fun setVolumeBoost(volumePercent: Int) {
            currentVolumePercent = volumePercent
            engine?.setVolumeBoost(volumePercent)
        }

        fun selectAudioTrack(trackId: String) {
            engine?.selectAudioTrack(trackId)
        }

        fun selectSubtitleTrack(trackId: String?) {
            engine?.selectSubtitleTrack(trackId)
        }

        fun addExternalSubtitle(uri: Uri): Boolean {
            return engine?.addExternalSubtitle(uri, true) ?: false
        }

        fun getThumbnailBytes(context: Context, video: VideoItem): ByteArray? {
            val diskCacheDir = File(context.cacheDir, "mizu_thumbs").apply { if (!exists()) mkdirs() }
            val cacheKeyName = (video.uri.lastPathSegment ?: video.uri.hashCode().toString()).replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val cacheFile = File(diskCacheDir, "$cacheKeyName.jpg")

            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    return cacheFile.readBytes()
                } catch (_: Exception) {}
            }

            try {
                var bitmap: Bitmap? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        bitmap = context.contentResolver.loadThumbnail(video.uri, Size(512, 288), null)
                    } catch (_: Throwable) {}
                }

                if (bitmap == null) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, video.uri)
                        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        val seekUs = when {
                            durationMs > 20_000L -> 12_000_000L
                            durationMs > 2_000L -> (durationMs * 500L)
                            else -> 0L
                        }
                        bitmap = retriever.getFrameAtTime(seekUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (_: Throwable) {
                    } finally {
                        try { retriever.release() } catch (_: Throwable) {}
                    }
                }

                if (bitmap != null) {
                    val targetW = 512
                    val targetH = (512f * bitmap.height / bitmap.width).toInt().coerceAtLeast(288)
                    val scaled = if (bitmap.width != targetW || bitmap.height != targetH) {
                        val s = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                        if (s != bitmap) bitmap.recycle()
                        s
                    } else bitmap

                    val outStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                    val bytes = outStream.toByteArray()

                    try {
                        FileOutputStream(cacheFile).use { fos ->
                            fos.write(bytes)
                        }
                    } catch (_: Throwable) {}

                    return bytes
                }
            } catch (_: Throwable) {}

            return null
        }

        fun buildMediaItem(context: Context, video: VideoItem, artworkBytes: ByteArray? = null): MediaItem {
            val artBytes = artworkBytes ?: getThumbnailBytes(context, video)
            val metadata = MediaMetadata.Builder()
                .setTitle(video.title)
                .setDisplayTitle(video.title)
                .apply {
                    if (artBytes != null) {
                        setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                }
                .build()

            return MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(if (video.id > 0) video.id.toString() else video.path.ifEmpty { video.uri.toString() })
                .setMediaMetadata(metadata)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.log(this, "PlaybackService.onCreate")

        val vlcEngine = VlcPlayerEngine(this)
        playerEngine = vlcEngine
        engine = vlcEngine

        val focusHelper = AudioFocusHelper(this, vlcEngine)
        audioFocusHelper = focusHelper
        focusHelper.registerNoisyReceiver()

        val adapter = VlcMediaSessionAdapter(this, vlcEngine)
        sessionAdapter = adapter

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val session = MediaSession.Builder(this, adapter)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        mediaSession = session
        addSession(session)
        DiagnosticLogger.log(this, "PlaybackService.onCreate", "MediaSession registered with libVLC adapter")

        vlcEngine.addListener(object : PlayerEngineListener {
            override fun onPlaybackStateChanged(state: Int) {
                hasPrevious = vlcEngine.hasPrevious()
                hasNext = vlcEngine.hasNext()
                val v = currentVideo
                if (v != null) {
                    val dur = if (vlcEngine.durationMs > 0) vlcEngine.durationMs else v.durationMs
                    if (state == Player.STATE_ENDED) {
                        MediaRepository.savePlaybackPosition(applicationContext, v, 0L, dur)
                        if (!vlcEngine.hasNext()) {
                            isPlaying = false
                            stopPlayback()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                hasPrevious = vlcEngine.hasPrevious()
                hasNext = vlcEngine.hasNext()
                if (playing) {
                    audioFocusHelper?.requestAudioFocus()
                } else {
                    currentVideo?.let { v ->
                        val dur = if (vlcEngine.durationMs > 0) vlcEngine.durationMs else v.durationMs
                        MediaRepository.savePlaybackPosition(applicationContext, v, vlcEngine.currentPositionMs, dur)
                    }
                }
            }

            override fun onTimeChanged(currentTimeMs: Long, durationMs: Long) {
                currentPositionMs = currentTimeMs
                this@PlaybackService.durationMs = durationMs
            }

            override fun onTracksChanged(audioTracks: List<AudioTrackInfo>, subtitleTracks: List<SubtitleTrackInfo>) {
                availableAudioTracks = audioTracks
                availableSubtitleTracks = subtitleTracks
            }

            override fun onMediaItemTransition(video: VideoItem, index: Int) {
                currentVideo = video
                currentIndex = index
                hasPrevious = vlcEngine.hasPrevious()
                hasNext = vlcEngine.hasNext()
                errorMessage = null
                currentNotice = null
            }

            override fun onError(errorMessage: String, canFallback: Boolean) {
                DiagnosticLogger.log(applicationContext, "PlaybackService.onError", "msg=$errorMessage, canFallback=$canFallback")
                PlaybackService.errorMessage = errorMessage
                if (!canFallback) {
                    isPlaying = false
                }
            }

            override fun onNotice(notice: String) {
                currentNotice = notice
                serviceScope.launch {
                    delay(4000)
                    if (currentNotice == notice) {
                        currentNotice = null
                    }
                }
            }
        })

        // Periodic 2-second position snapshot
        serviceScope.launch {
            while (isActive) {
                delay(2000)
                val v = currentVideo
                val eng = playerEngine
                if (v != null && eng != null && eng.isPlaying) {
                    val dur = if (eng.durationMs > 0) eng.durationMs else v.durationMs
                    MediaRepository.savePlaybackPosition(applicationContext, v, eng.currentPositionMs, dur)
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    private fun startForegroundWithPlaceholder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mizu video playback notification"
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentTitle = currentVideo?.title ?: "Playing Media"
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pip_play)
            .setContentTitle(currentTitle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            DiagnosticLogger.log(this, "PlaybackService.startForeground", "success, id=$NOTIFICATION_ID")
        } catch (e: Exception) {
            DiagnosticLogger.log(this, "PlaybackService.startForeground_error", e.message ?: "error")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DiagnosticLogger.log(this, "PlaybackService.onStartCommand", "action=${intent?.action}, startId=$startId")
        when (intent?.action) {
            ACTION_PLAY_QUEUE -> {
                startForegroundWithPlaceholder()
                val targetIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                playerEngine?.loadQueue(playlist, targetIndex)
            }
            ACTION_STOP -> {
                stopPlayback()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopPlayback() {
        DiagnosticLogger.log(this, "PlaybackService.stopPlayback", "invoked")
        val v = currentVideo
        val eng = playerEngine
        if (v != null && eng != null) {
            val dur = if (eng.durationMs > 0) eng.durationMs else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, eng.currentPositionMs, dur)
        }
        eng?.pause()
        isPlaying = false
        audioFocusHelper?.abandonAudioFocus()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        DiagnosticLogger.log(this, "PlaybackService.onTaskRemoved")
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        DiagnosticLogger.log(this, "PlaybackService.onDestroy")
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}

        audioFocusHelper?.unregisterNoisyReceiver()
        audioFocusHelper?.abandonAudioFocus()

        serviceJob.cancel()

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}

        val v = currentVideo
        val eng = playerEngine
        if (v != null && eng != null) {
            val dur = if (eng.durationMs > 0) eng.durationMs else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, eng.currentPositionMs, dur)
        }

        mediaSession?.let {
            removeSession(it)
            it.player.release()
            it.release()
            mediaSession = null
        }

        eng?.release()
        playerEngine = null
        engine = null
        currentVideo = null
        isPlaying = false
        currentNotice = null
        errorMessage = null
    }
}
