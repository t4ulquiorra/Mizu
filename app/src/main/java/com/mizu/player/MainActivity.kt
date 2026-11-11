package com.mizu.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.VideoItem
import com.mizu.player.service.PlaybackService
import com.mizu.player.ui.LibraryScreen
import com.mizu.player.ui.PermissionHelper
import com.mizu.player.ui.PermissionScreen
import com.mizu.player.ui.PlayerScreen
import com.mizu.player.ui.SettingsScreen
import com.mizu.player.util.DiagnosticLogger

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var isPlayingVideo by mutableStateOf(false)

    companion object {
        const val ACTION_PIP_CONTROL = "com.mizu.player.action.PIP_CONTROL"
        const val EXTRA_CONTROL = "extra_control"
        const val CONTROL_PREV = "control_prev"
        const val CONTROL_PLAY_PAUSE = "control_play_pause"
        const val CONTROL_NEXT = "control_next"
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                val p = PlaybackService.player ?: return
                when (intent.getStringExtra(EXTRA_CONTROL)) {
                    CONTROL_PREV -> if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
                    CONTROL_PLAY_PAUSE -> {
                        if (p.isPlaying) p.pause() else p.play()
                    }
                    CONTROL_NEXT -> if (p.hasNextMediaItem()) p.seekToNextMediaItem()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticLogger.init(applicationContext)
        DiagnosticLogger.log(this, "MainActivity.onCreate", "intent=$intent")
        MediaRepository.initPreferences(this)

        // Register PiP action broadcast receiver
        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0D0F14),
                    surface = Color(0xFF13161F),
                    primary = Color(0xFF3B82F6)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0F14))
                ) {
                    AppContent(
                        intent = intent,
                        isInPipMode = isInPipMode,
                        onVideoPlayingStateChange = { active ->
                            isPlayingVideo = active
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        DiagnosticLogger.log(this, "MainActivity.onStart")
    }

    override fun onResume() {
        super.onResume()
        DiagnosticLogger.log(this, "MainActivity.onResume")
    }

    override fun onPause() {
        super.onPause()
        DiagnosticLogger.log(this, "MainActivity.onPause")
    }

    private fun handlePipModeChange(inPip: Boolean) {
        DiagnosticLogger.log(this, "MainActivity.handlePipModeChange", "inPip=$inPip")
        isInPipMode = inPip
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        @Suppress("DEPRECATION")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        DiagnosticLogger.log(this, "MainActivity.onPictureInPictureModeChanged", "isInPip=$isInPictureInPictureMode")
        handlePipModeChange(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        DiagnosticLogger.log(this, "MainActivity.onStop", "isInPipMode=$isInPipMode, isFinishing=$isFinishing")
        // Stop playback only if user explicitly closed the PiP window via the cross (X) button, which finishes the activity
        if (isInPipMode && isFinishing) {
            PlaybackService.stop(this)
            isPlayingVideo = false
        }
    }

    fun buildPipParams(isPlaying: Boolean, hasPrev: Boolean, hasNext: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                builder.setAspectRatio(Rational(16, 9))
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(isPlayingVideo)
                builder.setSeamlessResizeEnabled(true)
            }

            val actions = mutableListOf<RemoteAction>()

            // 1. Previous Action (matching unified Material 24dp vector icon)
            val prevIntent = PendingIntent.getBroadcast(
                this, 101,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val prevIcon = Icon.createWithResource(this, R.drawable.ic_pip_previous)
            val prevAction = RemoteAction(prevIcon, "Previous", "Previous", prevIntent).apply {
                isEnabled = hasPrev
            }
            actions.add(prevAction)

            // 2. Play / Pause Action (matching unified Material 24dp vector icon)
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 102,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseIcon = Icon.createWithResource(
                this,
                if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
            )
            val playPauseAction = RemoteAction(
                playPauseIcon,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            actions.add(playPauseAction)

            // 3. Next Action (matching unified Material 24dp vector icon)
            val nextIntent = PendingIntent.getBroadcast(
                this, 103,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextIcon = Icon.createWithResource(this, R.drawable.ic_pip_next)
            val nextAction = RemoteAction(nextIcon, "Next", "Next", nextIntent).apply {
                isEnabled = hasNext
            }
            actions.add(nextAction)

            builder.setActions(actions)
        }
        return builder.build()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        DiagnosticLogger.log(this, "MainActivity.onUserLeaveHint", "isPlayingVideo=$isPlayingVideo")
        if (isPlayingVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(
                    isPlaying = PlaybackService.isPlaying,
                    hasPrev = PlaybackService.hasPrevious,
                    hasNext = PlaybackService.hasNext
                )
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DiagnosticLogger.log(this, "MainActivity.onDestroy", "isFinishing=$isFinishing")
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
        if (isFinishing) {
            PlaybackService.stop(this)
            isPlayingVideo = false
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AppContent(
    intent: Intent?,
    isInPipMode: Boolean = false,
    onVideoPlayingStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasStoragePermission(context))
    }

    val activeServiceVideo = PlaybackService.currentVideo
    var playingVideo by remember { mutableStateOf<VideoItem?>(activeServiceVideo) }
    var currentQueue by remember { mutableStateOf<List<VideoItem>>(PlaybackService.playlist) }
    var currentQueueIndex by remember { mutableIntStateOf(PlaybackService.currentIndex) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(activeServiceVideo) {
        if (activeServiceVideo != null && playingVideo == null) {
            playingVideo = activeServiceVideo
            currentQueue = PlaybackService.playlist
            currentQueueIndex = PlaybackService.currentIndex
        }
    }

    // Dynamically update PiP actions whenever playing state or next/prev availability changes
    val isPlaying = PlaybackService.isPlaying
    val hasPrev = PlaybackService.hasPrevious
    val hasNext = PlaybackService.hasNext

    LaunchedEffect(isInPipMode, isPlaying, hasPrev, hasNext) {
        if (isInPipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                activity?.setPictureInPictureParams(
                    activity.buildPipParams(isPlaying, hasPrev, hasNext)
                )
            } catch (_: Exception) {}
        }
    }

    // Inform activity whether video playback is currently active
    LaunchedEffect(playingVideo) {
        DiagnosticLogger.log(context, "AppContent.playingVideoChanged", "playingVideo=${playingVideo?.title} (null=${playingVideo == null})")
        onVideoPlayingStateChange(playingVideo != null)
    }

    // Check for "Open with" Intent on first launch
    remember(intent) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            val title = uri.lastPathSegment ?: "Playing Media"
            val externalVideo = VideoItem(
                id = -1,
                title = title,
                uri = uri,
                path = uri.path ?: "",
                durationMs = 0L,
                sizeBytes = 0L,
                dateModified = 0L,
                subtitleType = null
            )
            currentQueue = listOf(externalVideo)
            currentQueueIndex = 0
            playingVideo = externalVideo
        }
    }

    // Automatically re-check permission when the user returns to Mizu from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionHelper.hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onPermissionGranted = {
                hasPermission = true
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                showSettings -> {
                    SettingsScreen(
                        onBack = {
                            showSettings = false
                        }
                    )
                }
                else -> {
                    LibraryScreen(
                        onVideoClick = { video, queue, index ->
                            currentQueue = queue
                            currentQueueIndex = index
                            playingVideo = video
                        },
                        onOpenSettings = {
                            showSettings = true
                        }
                    )

                    playingVideo?.let { video ->
                        PlayerScreen(
                            video = video,
                            playlist = currentQueue.ifEmpty { listOf(video) },
                            initialIndex = currentQueueIndex,
                            onBack = {
                                playingVideo = null
                            },
                            isInPipMode = isInPipMode
                        )
                    }
                }
            }
        }
    }
}
