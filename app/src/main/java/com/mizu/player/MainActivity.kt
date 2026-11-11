package com.mizu.player

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
        MediaRepository.initPreferences(this)

        // Register PiP action broadcast receiver
        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addOnPictureInPictureModeChangedListener { info ->
                isInPipMode = info.isInPictureInPictureMode
            }
        }

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

    fun buildPipParams(isPlaying: Boolean, hasPrev: Boolean, hasNext: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = mutableListOf<RemoteAction>()

            // 1. Previous Action
            val prevIntent = PendingIntent.getBroadcast(
                this, 101,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val prevIcon = Icon.createWithResource(this, android.R.drawable.ic_media_previous)
            val prevAction = RemoteAction(prevIcon, "Previous", "Previous", prevIntent).apply {
                isEnabled = hasPrev
            }
            actions.add(prevAction)

            // 2. Play / Pause Action
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 102,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseIcon = Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            val playPauseAction = RemoteAction(
                playPauseIcon,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            actions.add(playPauseAction)

            // 3. Next Action
            val nextIntent = PendingIntent.getBroadcast(
                this, 103,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL, CONTROL_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextIcon = Icon.createWithResource(this, android.R.drawable.ic_media_next)
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
        if (isPlayingVideo && PlaybackService.isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    override fun onStop() {
        super.onStop()
        // If user closes PiP with the cross (X) button, stop playback completely
        if (isInPipMode) {
            PlaybackService.stop(this)
            isInPipMode = false
            isPlayingVideo = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
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

    var playingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var currentQueue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentQueueIndex by remember { mutableIntStateOf(0) }

    // Launcher for POST_NOTIFICATIONS on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Playback continues regardless of notification permission outcome */ }

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
            LibraryScreen(
                onVideoClick = { video, queue, index ->
                    currentQueue = queue
                    currentQueueIndex = index
                    playingVideo = video

                    // Request notification permission once on Android 13+ if not yet granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
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
