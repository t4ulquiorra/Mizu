package com.mizu.player.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.util.TimeFormatter

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
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekPreview: (Boolean) -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeDrag: (Boolean) -> Unit,
    onClose: () -> Unit,
    onPip: () -> Unit,
    modifier: Modifier = Modifier,
    controlsAlpha: Float = 1f
) {
    var draggingSeekFraction by remember { mutableStateOf<Float?>(null) }
    var isDraggingVolume by remember { mutableStateOf(false) }

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
        // Top row: close, pip (left) and volume slider (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = controlsAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.Close, contentDescription = "Close", tint = Color.White)
                }
                // PiP is available on Android 8+ (API 26+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(onClick = onPip, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcons.Pip, contentDescription = "Picture in Picture", tint = Color.White)
                    }
                }
            }

            // Volume control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
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
                    trackHeight = 4.dp,
                    thumbRadius = 5.dp,
                    modifier = Modifier
                        .width(110.dp)
                        .height(48.dp)
                )
            }
        }

        // Center row: replay 10, play/pause (larger), forward 10
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = controlsAlpha },
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

        // Bottom section: title & extra buttons row, then seek bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha },
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

            // Seek bar with elapsed and remaining timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = elapsedText,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
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
                    },
                    onValueChangeFinished = {
                        val targetMs = ((draggingSeekFraction ?: sliderProgress) * durationMs).toLong()
                        onSeek(targetMs)
                        draggingSeekFraction = null
                        onSeekPreview(false)
                    },
                    enabled = durationMs > 0,
                    trackHeight = 4.dp,
                    thumbRadius = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = remainingText,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
