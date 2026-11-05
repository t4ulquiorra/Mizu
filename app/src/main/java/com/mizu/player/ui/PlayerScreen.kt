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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

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

    // Swipe and double-tap gesture states
    var isSwipingVolume by remember { mutableStateOf(false) }
    var isSwipingBrightness by remember { mutableStateOf(false) }
    var showVolumeGesture by remember { mutableStateOf(false) }
    var showBrightnessGesture by remember { mutableStateOf(false) }
    var volumeGestureJob by remember { mutableStateOf<Job?>(null) }
    var brightnessGestureJob by remember { mutableStateOf<Job?>(null) }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapIsLeft by remember { mutableStateOf(false) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }

    val videoTitle = remember(videoUri) { getVideoTitle(context, videoUri) }

    // Audio volume management
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1 }
    var volumeFraction by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        mutableFloatStateOf(if (maxVolume > 0) current.toFloat() / maxVolume.toFloat() else 1f)
    }

    // App window brightness management (defaults to current or 0.5f)
    var brightnessFraction by remember {
        val winBright = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(
            if (winBright in 0.01f..1.0f) winBright
            else {
                try {
                    android.provider.Settings.System.getInt(
                        context.contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                    ) / 255f
                } catch (_: Exception) {
                    0.5f
                }
            }
        )
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

    val isSwiping = isSwipingVolume || isSwipingBrightness
    val targetControlsAlpha = if (areControlsVisible && !isDraggingSeek && !isSwiping) 1f else 0f
    val controlsAlpha by animateFloatAsState(
        targetValue = targetControlsAlpha,
        animationSpec = tween(200),
        label = "controlsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isDraggingSeek, isDraggingVolume) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()

                    // Safe zones: exclude top 15% (notifications) and bottom 20% (nav bar & seek bar)
                    val topSafeBound = 0.15f * screenHeight
                    val bottomSafeBound = 0.80f * screenHeight
                    val isInVerticalSafeZone = startY in topSafeBound..bottomSafeBound

                    // Horizontal zones: left 35% (brightness / rewind), right 35% (volume / forward), center 30% dead zone
                    val isLeftZone = startX < 0.35f * screenWidth
                    val isRightZone = startX > 0.65f * screenWidth
                    val isSwipeAllowed = isInVerticalSafeZone && (isLeftZone || isRightZone)

                    var hasMovedPastSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    var initialVolumeFraction = volumeFraction
                    var initialBrightnessFraction = brightnessFraction

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            if (!hasMovedPastSlop) {
                                if (isDraggingSeek || isDraggingVolume || change.isConsumed) {
                                    // Seek bar or buttons own this touch
                                    break
                                }
                                val absDx = kotlin.math.abs(dx)
                                val absDy = kotlin.math.abs(dy)
                                if (absDx > touchSlop && absDx > absDy) {
                                    // Horizontal drag - never trigger vertical swipe gestures
                                    break
                                }
                                if (absDy > touchSlop && absDy > absDx) {
                                    hasMovedPastSlop = true
                                    if (isSwipeAllowed) {
                                        change.consume()
                                        singleTapJob?.cancel()
                                        lastTapTime = 0L
                                        initialVolumeFraction = volumeFraction
                                        initialBrightnessFraction = brightnessFraction
                                        if (isRightZone) {
                                            volumeGestureJob?.cancel()
                                            isSwipingVolume = true
                                            showVolumeGesture = true
                                        } else if (isLeftZone) {
                                            brightnessGestureJob?.cancel()
                                            isSwipingBrightness = true
                                            showBrightnessGesture = true
                                        }
                                    }
                                }
                            } else {
                                if (isSwipeAllowed) {
                                    change.consume()
                                    val deltaFraction = -dy / screenHeight
                                    if (isRightZone) {
                                        val newVol = (initialVolumeFraction + deltaFraction).coerceIn(0f, 1f)
                                        volumeFraction = newVol
                                        if (maxVolume > 0) {
                                            val streamVol = (newVol * maxVolume).roundToInt()
                                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                                        }
                                    } else if (isLeftZone) {
                                        val newBright = (initialBrightnessFraction + deltaFraction).coerceIn(0.01f, 1f)
                                        brightnessFraction = newBright
                                        activity?.window?.let { win ->
                                            val lp = win.attributes
                                            lp.screenBrightness = newBright
                                            win.attributes = lp
                                        }
                                    }
                                }
                            }
                        } else {
                            // Pointer up
                            if (!hasMovedPastSlop) {
                                if (!isDraggingSeek && !isDraggingVolume && !change.isConsumed) {
                                    val isDoubleTapZone = isInVerticalSafeZone && (isLeftZone || isRightZone)
                                    val now = System.currentTimeMillis()

                                    if (isDoubleTapZone && (now - lastTapTime < 350L) && (lastTapIsLeft == isLeftZone)) {
                                        // Double-tap in safe side zones: skip 10s without toggling controls
                                        singleTapJob?.cancel()
                                        lastTapTime = now
                                        lastInteractionTime = now
                                        if (isLeftZone) {
                                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                            exoPlayer.seekTo(target)
                                        } else {
                                            val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(0L))
                                            exoPlayer.seekTo(target)
                                        }
                                    } else if (isDoubleTapZone) {
                                        // First tap in safe side zones: wait briefly for potential second tap
                                        lastTapTime = now
                                        lastTapIsLeft = isLeftZone
                                        singleTapJob?.cancel()
                                        singleTapJob = coroutineScope.launch {
                                            delay(250)
                                            areControlsVisible = !areControlsVisible
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                    } else {
                                        // Tap in dead zone / margins: toggle immediately
                                        singleTapJob?.cancel()
                                        lastTapTime = 0L
                                        areControlsVisible = !areControlsVisible
                                        lastInteractionTime = now
                                    }
                                }
                            } else {
                                if (isSwipeAllowed) {
                                    change.consume()
                                    if (isRightZone) {
                                        isSwipingVolume = false
                                        volumeGestureJob?.cancel()
                                        volumeGestureJob = coroutineScope.launch {
                                            delay(400)
                                            showVolumeGesture = false
                                        }
                                    } else if (isLeftZone) {
                                        isSwipingBrightness = false
                                        brightnessGestureJob?.cancel()
                                        brightnessGestureJob = coroutineScope.launch {
                                            delay(400)
                                            showBrightnessGesture = false
                                        }
                                    }
                                    if (areControlsVisible) {
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                            }
                            break
                        }
                    }
                }
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

        // Dim layer tied directly to controlsAlpha (40% black at alpha 1, no dim during swipe gestures)
        if (controlsAlpha > 0f && !isSwiping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f * controlsAlpha))
            )
        }

        // Custom Compose overlay controls driven by controlsAlpha and gesture states
        if (controlsAlpha > 0f || isDraggingSeek || showVolumeGesture || showBrightnessGesture) {
            VideoControls(
                title = videoTitle,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeFraction = volumeFraction,
                brightnessFraction = brightnessFraction,
                isSwipingVolume = isSwipingVolume,
                isSwipingBrightness = isSwipingBrightness,
                showVolumeGesture = showVolumeGesture,
                showBrightnessGesture = showBrightnessGesture,
                controlsAlpha = controlsAlpha,
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
                onPreviousVideo = {
                    lastInteractionTime = System.currentTimeMillis()
                    exoPlayer.seekTo(0L)
                },
                onNextVideo = {
                    lastInteractionTime = System.currentTimeMillis()
                    exoPlayer.seekTo(0L)
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
