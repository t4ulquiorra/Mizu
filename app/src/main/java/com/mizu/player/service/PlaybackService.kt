package com.mizu.player.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mizu.player.MainActivity
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_QUEUE = "com.mizu.player.action.PLAY_QUEUE"
        const val ACTION_STOP = "com.mizu.player.action.STOP"
        const val EXTRA_START_INDEX = "extra_start_index"

        // Observable state for Compose UI & PiP
        var player by mutableStateOf<ExoPlayer?>(null)
            private set
        var currentVideo by mutableStateOf<VideoItem?>(null)
            private set
        var playlist by mutableStateOf<List<VideoItem>>(emptyList())
            private set
        var currentIndex by mutableIntStateOf(0)
            private set
        var isPlaying by mutableStateOf(false)
            private set
        var errorMessage by mutableStateOf<String?>(null)
        var hasPrevious by mutableStateOf(false)
            private set
        var hasNext by mutableStateOf(false)
            private set

        fun start(context: Context, videos: List<VideoItem>, startIndex: Int) {
            playlist = videos
            currentIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
            currentVideo = videos.getOrNull(currentIndex)
            errorMessage = null

            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_PLAY_QUEUE
                putExtra(EXTRA_START_INDEX, currentIndex)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun buildMediaItem(video: VideoItem): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(video.title)
                .setDisplayTitle(video.title)
                .setArtworkUri(video.uri)
                .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(if (video.id > 0) video.id.toString() else video.path.ifEmpty { video.uri.toString() })
                .setMediaMetadata(metadata)

            if (video.subtitleType != null && video.path.isNotEmpty()) {
                val videoFile = File(video.path)
                val parent = videoFile.parentFile
                val baseName = videoFile.nameWithoutExtension
                val directSub = File(parent, "$baseName.${video.subtitleType.lowercase()}")
                val subFile = if (directSub.exists()) directSub else File(parent, "$baseName.ass")

                if (subFile.exists()) {
                    val mimeType = when (video.subtitleType.lowercase()) {
                        "srt" -> MimeTypes.APPLICATION_SUBRIP
                        "ssa", "ass" -> MimeTypes.TEXT_SSA
                        "vtt" -> MimeTypes.TEXT_VTT
                        else -> MimeTypes.APPLICATION_SUBRIP
                    }
                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                        .setMimeType(mimeType)
                        .setLanguage("und")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                }
            }
            return mediaItemBuilder.build()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Configure audio attributes for media playback with system audio focus
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // 2. Initialize ExoPlayer with becoming-noisy receiver and audio focus handling
        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        p.repeatMode = Player.REPEAT_MODE_OFF

        // 3. Create Session Activity Intent for notification clicks
        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Build MediaSession
        val session = MediaSession.Builder(this, p)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        mediaSession = session
        exoPlayer = p
        player = p

        // 5. Player Event Listener
        p.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newIndex = p.currentMediaItemIndex
                if (newIndex in playlist.indices) {
                    currentIndex = newIndex
                    currentVideo = playlist[newIndex]

                    // Resume saved position for this video if available
                    val targetVideo = playlist[newIndex]
                    val savedPos = MediaRepository.getSavedPosition(targetVideo)
                    if (savedPos > 3000L && (targetVideo.durationMs <= 0 || savedPos < targetVideo.durationMs - 5000L)) {
                        p.seekTo(savedPos)
                    }
                }
                hasPrevious = p.hasPreviousMediaItem()
                hasNext = p.hasNextMediaItem()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                hasPrevious = p.hasPreviousMediaItem()
                hasNext = p.hasNextMediaItem()

                val v = currentVideo
                if (v != null) {
                    val dur = if (p.duration > 0) p.duration else v.durationMs
                    if (playbackState == Player.STATE_ENDED) {
                        MediaRepository.savePlaybackPosition(applicationContext, v, 0L, dur)
                        if (!p.hasNextMediaItem()) {
                            isPlaying = false
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                hasPrevious = p.hasPreviousMediaItem()
                hasNext = p.hasNextMediaItem()

                val v = currentVideo
                if (v != null) {
                    val dur = if (p.duration > 0) p.duration else v.durationMs
                    if (!playing) {
                        MediaRepository.savePlaybackPosition(applicationContext, v, p.currentPosition, dur)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val v = currentVideo
                if (v != null) {
                    val isMissing = v.path.isNotEmpty() && !File(v.path).exists()
                    if (isMissing) {
                        errorMessage = "This file no longer exists"
                        MediaRepository.removeVideo(applicationContext, v)
                    } else {
                        errorMessage = "This video can't be played"
                    }
                } else {
                    errorMessage = "This video can't be played"
                }
            }
        })

        // 6. Register screen-off broadcast receiver to pause playback
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        // 7. Periodic position snapshot while actively playing
        serviceScope.launch {
            while (isActive) {
                delay(2000)
                val activePlayer = exoPlayer
                val v = currentVideo
                if (activePlayer != null && v != null && activePlayer.isPlaying) {
                    val dur = if (activePlayer.duration > 0) activePlayer.duration else v.durationMs
                    MediaRepository.savePlaybackPosition(applicationContext, v, activePlayer.currentPosition, dur)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_QUEUE -> {
                val targetIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                loadAndPlayQueue(targetIndex)
            }
            ACTION_STOP -> {
                stopPlayback()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun loadAndPlayQueue(startIndex: Int) {
        val p = exoPlayer ?: return
        if (playlist.isEmpty()) return

        val mediaItems = playlist.map { buildMediaItem(it) }
        val safeIndex = startIndex.coerceIn(0, playlist.size - 1)

        val targetVideo = playlist[safeIndex]
        val savedPos = MediaRepository.getSavedPosition(targetVideo)
        val initialSeekPos = if (savedPos > 3000L && (targetVideo.durationMs <= 0 || savedPos < targetVideo.durationMs - 5000L)) {
            savedPos
        } else {
            0L
        }

        p.setMediaItems(mediaItems, safeIndex, initialSeekPos)
        p.prepare()
        p.playWhenReady = true

        hasPrevious = p.hasPreviousMediaItem()
        hasNext = p.hasNextMediaItem()
    }

    private fun stopPlayback() {
        val v = currentVideo
        val p = exoPlayer
        if (v != null && p != null) {
            val dur = if (p.duration > 0) p.duration else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, p.currentPosition, dur)
        }
        p?.stop()
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}

        serviceJob.cancel()

        val v = currentVideo
        val p = exoPlayer
        if (v != null && p != null) {
            val dur = if (p.duration > 0) p.duration else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, p.currentPosition, dur)
        }

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        player = null
        currentVideo = null
        isPlaying = false
    }
}
