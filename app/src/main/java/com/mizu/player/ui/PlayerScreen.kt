package com.mizu.player.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUri: Uri,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set up ExoPlayer
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var isDraggingVolume by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val videoTitle = remember(videoUri) { getVideoTitle(context, videoUri) }

    // Audio volume management
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1 }
    var volumeFraction by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        mutableFloatStateOf(if (maxVolume > 0) current.toFloat() / maxVolume.toFloat() else 1f)
    }

    // Lock orientation, hide system bars, keep screen on while player is active
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window?.let { win ->
            val controller = WindowCompat.getInsetsController(win, win.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { win ->
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            exoPlayer.release()
        }
    }

    // Pause on backgrounding
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Track playback state & progress
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            delay(200)
        }
    }

    // Auto-hide controls after 3 seconds of no interaction
    LaunchedEffect(areControlsVisible, isDraggingSeek, isDraggingVolume, lastInteractionTime) {
        if (areControlsVisible && !isDraggingSeek && !isDraggingVolume) {
            delay(3000)
            areControlsVisible = false
        }
    }

    // Back handling returns to picker screen
    BackHandler {
        onClose()
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (areControlsVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "controlsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
                lastInteractionTime = System.currentTimeMillis()
            }
    ) {
        // PlayerView used strictly as video surface with built-in controls disabled
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dim layer tied directly to controlsAlpha (40% black at alpha 1)
        if (controlsAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f * controlsAlpha))
            )
        }

        // Custom Compose overlay controls driven by controlsAlpha
        if (controlsAlpha > 0f) {
            VideoControls(
                controlsAlpha = controlsAlpha,
                title = videoTitle,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeFraction = volumeFraction,
                onTogglePlayPause = {
                    lastInteractionTime = System.currentTimeMillis()
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeek = { targetMs ->
                    lastInteractionTime = System.currentTimeMillis()
                    exoPlayer.seekTo(targetMs.coerceIn(0L, durationMs.coerceAtLeast(0L)))
                },
                onSeekPreview = { dragging ->
                    isDraggingSeek = dragging
                    if (!dragging) lastInteractionTime = System.currentTimeMillis()
                },
                onReplay10 = {
                    lastInteractionTime = System.currentTimeMillis()
                    val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                    exoPlayer.seekTo(target)
                },
                onForward10 = {
                    lastInteractionTime = System.currentTimeMillis()
                    val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(0L))
                    exoPlayer.seekTo(target)
                },
                onVolumeChange = { fraction ->
                    volumeFraction = fraction.coerceIn(0f, 1f)
                    if (maxVolume > 0) {
                        val streamVol = (volumeFraction * maxVolume).roundToInt()
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                    }
                },
                onVolumeDrag = { dragging ->
                    isDraggingVolume = dragging
                    if (!dragging) lastInteractionTime = System.currentTimeMillis()
                },
                onClose = onClose,
                onPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        try {
                            areControlsVisible = false
                            activity.enterPictureInPictureMode(
                                android.app.PictureInPictureParams.Builder().build()
                            )
                        } catch (_: Exception) {}
                    }
                }
            )
        }
    }
}

/**
 * Extracts display name without file extension from content or file URI.
 */
private fun getVideoTitle(context: Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        name = cursor.getString(idx)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    if (name == null) {
        name = uri.lastPathSegment
    }
    return name?.substringBeforeLast('.') ?: "Video"
}
