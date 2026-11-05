package com.mizu.player.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.util.TimeFormatter
import kotlin.math.roundToInt

/**
 * Overlay controls for video playback.
 * Respects safe areas and cutouts. Flat white icons and text.
 */
@Composable
fun VideoControls(
    title: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volumeFraction: Float,
    brightnessFraction: Float,
    isSwipingVolume: Boolean,
    isSwipingBrightness: Boolean,
    showVolumeGesture: Boolean,
    showBrightnessGesture: Boolean,
    controlsAlpha: Float,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekPreview: (Boolean) -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeDrag: (Boolean) -> Unit,
    onClose: () -> Unit,
    onPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingSeekFraction by remember { mutableStateOf<Float?>(null) }
    var isDraggingVolume by remember { mutableStateOf(false) }

    val isSeeking = draggingSeekFraction != null
    val seekTransition by animateFloatAsState(
        targetValue = if (isSeeking) 1f else 0f,
        animationSpec = tween(200),
        label = "seekTransition"
    )

    val isSwiping = isSwipingVolume || isSwipingBrightness
    val effectiveControlsAlpha = if (isSwiping) 0f else controlsAlpha

    // Brightness slider alpha: fades in with gesture, fades out 400ms after lift
    val brightnessSliderAlpha by animateFloatAsState(
        targetValue = if (showBrightnessGesture) 1f else 0f,
        animationSpec = tween(if (showBrightnessGesture) 150 else 400),
        label = "brightAlpha"
    )

    // Volume slider alpha: visible if controls are visible (and not swiping brightness) or during volume swipe
    val targetVolumeAlpha = if (showVolumeGesture || (controlsAlpha > 0f && !isSwipingBrightness)) 1f else 0f
    val volumeSliderAlpha by animateFloatAsState(
        targetValue = targetVolumeAlpha,
        animationSpec = tween(if (showVolumeGesture || controlsAlpha > 0f) 150 else 400),
        label = "volAlpha"
    )

    val effectivePositionMs = draggingSeekFraction?.let { (it * durationMs).toLong() } ?: currentPositionMs
    val showHours = durationMs >= 3600_000L
    val elapsedText = TimeFormatter.formatElapsed(effectivePositionMs, showHours)
    val remainingText = TimeFormatter.formatRemaining(effectivePositionMs, durationMs, showHours)

    val sliderProgress = if (durationMs > 0) {
        (effectivePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Top-left: Brightness slider (only during left swipe) OR Close & PiP
            Box(contentAlignment = Alignment.CenterStart) {
                // Brightness slider (sun icon + matching horizontal slider in same style)
                if (brightnessSliderAlpha > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .graphicsLayer { alpha = brightnessSliderAlpha }
                    ) {
                        Icon(
                            AppIcons.Brightness,
                            contentDescription = "Brightness",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MizuSlider(
                            value = brightnessFraction,
                            onValueChange = {},
                            enabled = false,
                            trackHeight = 8.dp,
                            showThumb = false,
                            modifier = Modifier
                                .width(110.dp)
                                .height(48.dp)
                        )
                    }
                }

                // Close and PiP buttons (hidden during brightness swipe or swipe gestures)
                if (effectiveControlsAlpha > 0f && !showBrightnessGesture) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer { alpha = effectiveControlsAlpha }
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                            Icon(AppIcons.Close, contentDescription = "Close", tint = Color.White)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = onPip, modifier = Modifier.size(48.dp)) {
                                Icon(AppIcons.Pip, contentDescription = "Picture in Picture", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Top-right: Volume slider
            if (volumeSliderAlpha > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .graphicsLayer { alpha = volumeSliderAlpha }
                ) {
                    Icon(
                        AppIcons.Volume,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MizuSlider(
                        value = volumeFraction,
                        onValueChange = onVolumeChange,
                        onValueChangeStarted = {
                            isDraggingVolume = true
                            onVolumeDrag(true)
                        },
                        onValueChangeFinished = {
                            isDraggingVolume = false
                            onVolumeDrag(false)
                        },
                        trackHeight = 8.dp,
                        showThumb = false,
                        modifier = Modifier
                            .width(110.dp)
                            .height(48.dp)
                    )
                }
            }
        }

        // Center row: replay 10, play/pause (larger), forward 10
        if (effectiveControlsAlpha > 0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = effectiveControlsAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                IconButton(onClick = onReplay10, modifier = Modifier.size(48.dp)) {
                    Icon(
                        AppIcons.Replay10,
                        contentDescription = "Back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (isPlaying) AppIcons.Pause else AppIcons.Play,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
                IconButton(onClick = onForward10, modifier = Modifier.size(48.dp)) {
                    Icon(
                        AppIcons.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Bottom section: title & extra buttons row, then seek bar
        val seekSectionAlpha = if (isSeeking) 1f else if (isSwiping) 0f else controlsAlpha
        if (effectiveControlsAlpha > 0f || seekSectionAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                if (effectiveControlsAlpha > 0f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = effectiveControlsAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp, end = 16.dp)
                        )
                        // Visual only buttons for Step 1
                        Row {
                            IconButton(onClick = { /* Visual only in Step 1 */ }, modifier = Modifier.size(48.dp)) {
                                Icon(AppIcons.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = { /* Visual only in Step 1 */ }, modifier = Modifier.size(48.dp)) {
                                Icon(AppIcons.AudioTrack, contentDescription = "Audio track", tint = Color.White)
                            }
                            IconButton(onClick = { /* Visual only in Step 1 */ }, modifier = Modifier.size(48.dp)) {
                                Icon(AppIcons.Speed, contentDescription = "Playback speed", tint = Color.White)
                            }
                        }
                    }
                }

                // Seek bar with smoothly animated thickness (4dp to 12dp) and moving labels
                if (seekSectionAlpha > 0f) {
                    Layout(
                        content = {
                            Text(
                                text = elapsedText,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            MizuSlider(
                                value = sliderProgress,
                                onValueChange = { fraction ->
                                    if (draggingSeekFraction == null) {
                                        onSeekPreview(true)
                                    }
                                    draggingSeekFraction = fraction
                                },
                                onValueChangeStarted = {
                                    onSeekPreview(true)
                                    draggingSeekFraction = sliderProgress
                                },
                                onValueChangeFinished = {
                                    val targetMs = ((draggingSeekFraction ?: sliderProgress) * durationMs).toLong()
                                    onSeek(targetMs)
                                    draggingSeekFraction = null
                                    onSeekPreview(false)
                                },
                                enabled = durationMs > 0,
                                trackHeight = (8f + 4f * seekTransition).dp,
                                showThumb = false,
                                modifier = Modifier.height(48.dp)
                            )
                            Text(
                                text = remainingText,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .graphicsLayer { alpha = seekSectionAlpha }
                    ) { measurables, constraints ->
                        val gapPx = 8.dp.roundToPx()
                        val gapBelowPx = 4.dp.roundToPx()
                        val sliderHeightPx = 48.dp.roundToPx()

                        val elapsedPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val remainingPlaceable = measurables[2].measure(constraints.copy(minWidth = 0, minHeight = 0))

                        val fullWidth = constraints.maxWidth
                        val normalSliderWidth = (fullWidth - elapsedPlaceable.width - remainingPlaceable.width - 2 * gapPx).coerceAtLeast(0)
                        val currentSliderWidth = (normalSliderWidth + (fullWidth - normalSliderWidth) * seekTransition).roundToInt()

                        val sliderPlaceable = measurables[1].measure(
                            Constraints.fixed(width = currentSliderWidth, height = sliderHeightPx)
                        )

                        val labelMaxHeight = maxOf(elapsedPlaceable.height, remainingPlaceable.height)
                        val normalHeight = sliderHeightPx
                        val seekingHeight = sliderHeightPx + gapBelowPx + labelMaxHeight
                        val totalHeight = (normalHeight + (seekingHeight - normalHeight) * seekTransition).roundToInt()

                        layout(fullWidth, totalHeight) {
                            val sliderX = ((elapsedPlaceable.width + gapPx) * (1f - seekTransition)).roundToInt()
                            sliderPlaceable.placeRelative(sliderX, 0)

                            val normalElapsedY = (sliderHeightPx - elapsedPlaceable.height) / 2
                            val seekingElapsedY = sliderHeightPx + gapBelowPx
                            val elapsedY = (normalElapsedY + (seekingElapsedY - normalElapsedY) * seekTransition).roundToInt()
                            elapsedPlaceable.placeRelative(0, elapsedY)

                            val normalRemainingY = (sliderHeightPx - remainingPlaceable.height) / 2
                            val seekingRemainingY = sliderHeightPx + gapBelowPx
                            val remainingY = (normalRemainingY + (seekingRemainingY - normalRemainingY) * seekTransition).roundToInt()
                            val remainingX = fullWidth - remainingPlaceable.width
                            remainingPlaceable.placeRelative(remainingX, remainingY)
                        }
                    }
                }
            }
        }
    }
}
