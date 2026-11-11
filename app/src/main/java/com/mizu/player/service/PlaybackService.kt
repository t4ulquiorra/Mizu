package com.mizu.player.service

import android.app.Notification
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
import android.util.Log
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mizu.player.MainActivity
import com.mizu.player.R
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class MizuAudioTrack(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean
)

data class MizuSubtitleTrack(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean
)

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
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "mizu_playback_channel"

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

        var availableAudioTracks by mutableStateOf<List<MizuAudioTrack>>(emptyList())
            private set
        var availableSubtitleTracks by mutableStateOf<List<MizuSubtitleTrack>>(emptyList())
            private set

        fun start(context: Context, videos: List<VideoItem>, startIndex: Int) {
            val target = videos.getOrNull(startIndex)
            DiagnosticLogger.log(context, "PlaybackService.start", "count=${videos.size}, startIndex=$startIndex, video=${target?.title}")
            playlist = videos
            currentIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
            currentVideo = target
            errorMessage = null

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

        fun getAudioTracks(): List<MizuAudioTrack> {
            val p = player ?: return emptyList()
            val trackGroups = p.currentTracks.groups
            val list = mutableListOf<MizuAudioTrack>()
            var audioCount = 0

            trackGroups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (trackIndex in 0 until group.length) {
                        audioCount++
                        val format = group.getTrackFormat(trackIndex)
                        val lang = format.language?.lowercase() ?: ""
                        val langName = if (lang.isNotEmpty() && lang != "und") {
                            try {
                                val loc = Locale(lang)
                                loc.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
                            } catch (_: Exception) {
                                lang
                            }
                        } else null

                        val mime = format.sampleMimeType ?: ""
                        val formatName = when {
                            mime.contains("mp4a-latm", true) || mime.contains("aac", true) -> "AAC"
                            mime.contains("opus", true) -> "Opus"
                            mime.contains("ac3", true) -> "AC3"
                            mime.contains("eac3", true) -> "E-AC3"
                            mime.contains("dts", true) -> "DTS"
                            mime.contains("flac", true) -> "FLAC"
                            mime.contains("vorbis", true) -> "Vorbis"
                            mime.contains("true-hd", true) -> "TrueHD"
                            mime.contains("mpeg", true) -> "MP3"
                            else -> format.codecs?.uppercase() ?: ""
                        }

                        val channels = when (format.channelCount) {
                            1 -> "1.0"
                            2 -> "2.0"
                            6 -> "5.1"
                            8 -> "7.1"
                            in 3..5 -> "${format.channelCount}.0"
                            else -> if (format.channelCount > 0) "${format.channelCount} ch" else ""
                        }

                        val details = listOfNotNull(formatName.ifEmpty { null }, channels.ifEmpty { null }).joinToString(" · ")
                        val trackName = if (langName != null) {
                            if (details.isNotEmpty()) "$langName · $details" else langName
                        } else {
                            if (details.isNotEmpty()) "Track $audioCount · $details" else "Track $audioCount"
                        }

                        val isSelected = group.isTrackSelected(trackIndex)
                        val trackId = "${format.id ?: ""}_${lang}_${formatName}_$channels"

                        list.add(
                            MizuAudioTrack(
                                id = trackId,
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                name = trackName,
                                language = lang,
                                isSelected = isSelected
                            )
                        )
                    }
                }
            }
            return list
        }

        fun getSubtitleTracks(): List<MizuSubtitleTrack> {
            val p = player ?: return emptyList()
            val trackGroups = p.currentTracks.groups
            val list = mutableListOf<MizuSubtitleTrack>()
            var subCount = 0

            trackGroups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (trackIndex in 0 until group.length) {
                        subCount++
                        val format = group.getTrackFormat(trackIndex)
                        val lang = format.language?.lowercase() ?: ""
                        val langName = if (lang.isNotEmpty() && lang != "und") {
                            try {
                                val loc = Locale(lang)
                                loc.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
                            } catch (_: Exception) {
                                lang
                            }
                        } else null

                        val label = format.label
                        val trackName = when {
                            langName != null && !label.isNullOrBlank() && label != langName -> "$langName ($label)"
                            langName != null -> langName
                            !label.isNullOrBlank() -> label
                            else -> "Subtitle $subCount"
                        }

                        val isSelected = group.isTrackSelected(trackIndex)
                        val trackId = "${format.id ?: ""}_${lang}_$trackName"

                        list.add(
                            MizuSubtitleTrack(
                                id = trackId,
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                name = trackName,
                                language = lang,
                                isSelected = isSelected
                            )
                        )
                    }
                }
            }
            return list
        }

        fun refreshTracks() {
            availableAudioTracks = getAudioTracks()
            availableSubtitleTracks = getSubtitleTracks()
        }

        fun selectAudioTrack(context: Context, track: MizuAudioTrack) {
            val p = player ?: return
            val groups = p.currentTracks.groups
            if (track.groupIndex in groups.indices) {
                val group = groups[track.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setOverrideForType(override)
                    .build()

                currentVideo?.let { v ->
                    val key = UserPreferences.getVideoKey(v)
                    UserPreferences.setSavedAudioTrack(context, key, track.id)
                }
                refreshTracks()
            }
        }

        fun selectSubtitleTrack(context: Context, track: MizuSubtitleTrack) {
            val p = player ?: return
            val groups = p.currentTracks.groups
            if (track.groupIndex in groups.indices) {
                val group = groups[track.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(override)
                    .build()

                currentVideo?.let { v ->
                    val key = UserPreferences.getVideoKey(v)
                    UserPreferences.setSavedSubtitleTrack(context, key, track.id)
                }
                refreshTracks()
            }
        }

        fun disableSubtitles(context: Context) {
            val p = player ?: return
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()

            currentVideo?.let { v ->
                val key = UserPreferences.getVideoKey(v)
                UserPreferences.setSavedSubtitleTrack(context, key, UserPreferences.SUBTITLE_OFF)
            }
            refreshTracks()
        }

        private fun matchLanguage(trackLang: String?, prefLang: String): Boolean {
            if (trackLang.isNullOrBlank() || trackLang.equals("und", ignoreCase = true)) return false
            val cleanTrack = trackLang.trim().lowercase()
            val cleanPref = prefLang.trim().lowercase()
            if (cleanTrack == cleanPref) return true
            if (cleanTrack.startsWith("$cleanPref-") || cleanTrack.startsWith("${cleanPref}_")) return true
            return try {
                val localePref = Locale(cleanPref)
                val iso3Pref = localePref.isO3Language.lowercase()
                if (cleanTrack == iso3Pref) return true
                val localeTrack = Locale(cleanTrack)
                localeTrack.language.lowercase() == cleanPref || localeTrack.isO3Language.lowercase() == iso3Pref
            } catch (_: Exception) {
                false
            }
        }

        fun applyTrackPreferences(context: Context, video: VideoItem) {
            val p = player ?: return
            val key = UserPreferences.getVideoKey(video)
            val builder = p.trackSelectionParameters.buildUpon()

            val audioTracks = getAudioTracks()
            val subtitleTracks = getSubtitleTracks()

            // 1. Subtitles
            val savedSubId = UserPreferences.getSavedSubtitleTrack(context, key)
            if (savedSubId == UserPreferences.SUBTITLE_OFF) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else if (savedSubId != null) {
                val matchedSub = subtitleTracks.find { it.id == savedSubId }
                if (matchedSub != null) {
                    val group = p.currentTracks.groups.getOrNull(matchedSub.groupIndex)
                    if (group != null) {
                        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, matchedSub.trackIndex))
                    }
                } else {
                    applyDefaultSubtitlePriorities(context, subtitleTracks, builder, p)
                }
            } else {
                applyDefaultSubtitlePriorities(context, subtitleTracks, builder, p)
            }

            // 2. Audio
            val savedAudioId = UserPreferences.getSavedAudioTrack(context, key)
            if (savedAudioId != null) {
                val matchedAudio = audioTracks.find { it.id == savedAudioId }
                if (matchedAudio != null) {
                    val group = p.currentTracks.groups.getOrNull(matchedAudio.groupIndex)
                    if (group != null) {
                        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, matchedAudio.trackIndex))
                    }
                } else {
                    applyDefaultAudioPriorities(context, audioTracks, builder, p)
                }
            } else {
                applyDefaultAudioPriorities(context, audioTracks, builder, p)
            }

            p.trackSelectionParameters = builder.build()
            refreshTracks()
        }

        private fun applyDefaultSubtitlePriorities(
            context: Context,
            tracks: List<MizuSubtitleTrack>,
            builder: TrackSelectionParameters.Builder,
            player: ExoPlayer
        ) {
            if (UserPreferences.isSubtitlesOffByDefault(context)) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                return
            }
            val prefs = UserPreferences.getPreferredSubtitleLanguages(context)
            for (pref in prefs) {
                val matched = tracks.find { matchLanguage(it.language, pref) }
                if (matched != null) {
                    val group = player.currentTracks.groups.getOrNull(matched.groupIndex)
                    if (group != null) {
                        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, matched.trackIndex))
                        return
                    }
                }
            }
        }

        private fun applyDefaultAudioPriorities(
            context: Context,
            tracks: List<MizuAudioTrack>,
            builder: TrackSelectionParameters.Builder,
            player: ExoPlayer
        ) {
            val prefs = UserPreferences.getPreferredAudioLanguages(context)
            for (pref in prefs) {
                val matched = tracks.find { matchLanguage(it.language, pref) }
                if (matched != null) {
                    val group = player.currentTracks.groups.getOrNull(matched.groupIndex)
                    if (group != null) {
                        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, matched.trackIndex))
                        return
                    }
                }
            }
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
            val metadata = MediaMetadata.Builder()
                .setTitle(video.title)
                .setDisplayTitle(video.title)
                .apply {
                    if (artworkBytes != null) {
                        setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                }
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
        DiagnosticLogger.log(this, "PlaybackService.onCreate")

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
        addSession(session)
        DiagnosticLogger.log(this, "PlaybackService.onCreate", "addSession called")
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
                    updateArtworkWindow(newIndex)
                }
                hasPrevious = p.hasPreviousMediaItem()
                hasNext = p.hasNextMediaItem()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                DiagnosticLogger.log(applicationContext, "PlaybackService.onPlaybackStateChanged", "state=$playbackState")
                hasPrevious = p.hasPreviousMediaItem()
                hasNext = p.hasNextMediaItem()

                val v = currentVideo
                if (v != null) {
                    val dur = if (p.duration > 0) p.duration else v.durationMs
                    if (playbackState == Player.STATE_ENDED) {
                        MediaRepository.savePlaybackPosition(applicationContext, v, 0L, dur)
                        if (!p.hasNextMediaItem()) {
                            isPlaying = false
                            stopPlayback()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                DiagnosticLogger.log(applicationContext, "PlaybackService.onIsPlayingChanged", "playing=$playing")
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
                DiagnosticLogger.log(applicationContext, "PlaybackService.onPlayerError", Log.getStackTraceString(error))
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

            override fun onTracksChanged(tracks: Tracks) {
                val v = currentVideo
                if (v != null) {
                    applyTrackPreferences(applicationContext, v)
                } else {
                    refreshTracks()
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
                loadAndPlayQueue(targetIndex)
            }
            ACTION_STOP -> {
                stopPlayback()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        DiagnosticLogger.log(this, "PlaybackService.onUpdateNotification", "startInForegroundRequired=$startInForegroundRequired")
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private var artworkJob: Job? = null

    private fun updateArtworkWindow(centerIndex: Int) {
        artworkJob?.cancel()
        artworkJob = serviceScope.launch(Dispatchers.IO) {
            val targets = listOf(centerIndex, centerIndex - 1, centerIndex + 1)
                .filter { it in playlist.indices }

            for (idx in targets) {
                val video = playlist.getOrNull(idx) ?: continue
                val artworkBytes = getThumbnailBytes(applicationContext, video) ?: continue

                withContext(Dispatchers.Main) {
                    val p = exoPlayer ?: return@withContext
                    if (idx < p.mediaItemCount) {
                        val item = p.getMediaItemAt(idx)
                        if (item.mediaMetadata.artworkData == null) {
                            val newMetadata = item.mediaMetadata.buildUpon()
                                .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build()
                            val newItem = item.buildUpon()
                                .setMediaMetadata(newMetadata)
                                .build()
                            if (idx != p.currentMediaItemIndex) {
                                p.replaceMediaItem(idx, newItem)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadAndPlayQueue(startIndex: Int) {
        DiagnosticLogger.log(this, "PlaybackService.loadAndPlayQueue", "startIndex=$startIndex, count=${playlist.size}")
        val p = exoPlayer ?: return
        if (playlist.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, playlist.size - 1)
        val targetVideo = playlist[safeIndex]
        val savedPos = MediaRepository.getSavedPosition(targetVideo)
        val initialSeekPos = if (savedPos > 3000L && (targetVideo.durationMs <= 0 || savedPos < targetVideo.durationMs - 5000L)) {
            savedPos
        } else {
            0L
        }

        serviceScope.launch(Dispatchers.IO) {
            // Fast: only load artwork for starting item so it has artwork right away
            val startArtwork = getThumbnailBytes(applicationContext, targetVideo)

            // Build all MediaItems on background dispatcher without extracting thumbnails for all items
            val mediaItems = playlist.mapIndexed { i, video ->
                buildMediaItem(
                    applicationContext,
                    video,
                    artworkBytes = if (i == safeIndex) startArtwork else null
                )
            }

            withContext(Dispatchers.Main) {
                val activePlayer = exoPlayer ?: return@withContext
                activePlayer.setMediaItems(mediaItems, safeIndex, initialSeekPos)
                activePlayer.prepare()
                activePlayer.playWhenReady = true

                hasPrevious = activePlayer.hasPreviousMediaItem()
                hasNext = activePlayer.hasNextMediaItem()

                updateArtworkWindow(safeIndex)
            }
        }
    }

    private fun stopPlayback() {
        DiagnosticLogger.log(this, "PlaybackService.stopPlayback", "invoked")
        val v = currentVideo
        val p = exoPlayer
        if (v != null && p != null) {
            val dur = if (p.duration > 0) p.duration else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, p.currentPosition, dur)
        }
        p?.stop()
        isPlaying = false
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

        serviceJob.cancel()
        artworkJob?.cancel()

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}

        val v = currentVideo
        val p = exoPlayer
        if (v != null && p != null) {
            val dur = if (p.duration > 0) p.duration else v.durationMs
            MediaRepository.savePlaybackPosition(applicationContext, v, p.currentPosition, dur)
        }

        mediaSession?.let {
            removeSession(it)
            it.player.release()
            it.release()
            mediaSession = null
        }
        exoPlayer = null
        player = null
        currentVideo = null
        isPlaying = false
    }
}
