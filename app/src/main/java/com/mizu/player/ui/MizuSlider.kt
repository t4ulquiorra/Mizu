package com.mizu.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom smooth slider: thin rounded track with one round thumb.
 * No vertical line, no end dot, perfectly animated track thickness.
 */
@Composable
fun MizuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    showThumb: Boolean = true,
    onValueChangeStarted: (() -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChangeStarted?.invoke()
                        onValueChange(fraction)
                        val released = tryAwaitRelease()
                        if (released) {
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onValueChangeStarted?.invoke()
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val trackH = trackHeight.toPx()
            val thumbR = thumbRadius.toPx()
            val trackY = (height - trackH) / 2f
            val corner = CornerRadius(trackH / 2f, trackH / 2f)

            val clampedVal = value.coerceIn(0f, 1f)
            val thumbX = clampedVal * width

            // Unplayed track: white at 30% opacity
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, trackY),
                size = Size(width, trackH),
                cornerRadius = corner
            )

            // Played track: solid white
            if (thumbX > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, trackY),
                    size = Size(thumbX, trackH),
                    cornerRadius = corner
                )
            }

            // One small round thumb only: no vertical line, no end dot
            if (showThumb && thumbR > 0f) {
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = Offset(thumbX, height / 2f)
                )
            }
        }
    }
}
