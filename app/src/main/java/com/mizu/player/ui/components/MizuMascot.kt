package com.mizu.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Sun

@Composable
fun MizuMascot(
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Shadow offset
        val shadowOffset = 6.dp.toPx()
        val borderWidth = 3.dp.toPx()

        fun createDropletPath(offsetX: Float, offsetY: Float): Path {
            return Path().apply {
                moveTo(w * 0.5f + offsetX, h * 0.12f + offsetY)
                // Right curve down
                cubicTo(
                    w * 0.78f + offsetX, h * 0.42f + offsetY,
                    w * 0.90f + offsetX, h * 0.62f + offsetY,
                    w * 0.82f + offsetX, h * 0.82f + offsetY
                )
                // Bottom curve
                cubicTo(
                    w * 0.74f + offsetX, h * 0.98f + offsetY,
                    w * 0.26f + offsetX, h * 0.98f + offsetY,
                    w * 0.18f + offsetX, h * 0.82f + offsetY
                )
                // Left curve up
                cubicTo(
                    w * 0.10f + offsetX, h * 0.62f + offsetY,
                    w * 0.22f + offsetX, h * 0.42f + offsetY,
                    w * 0.50f + offsetX, h * 0.12f + offsetY
                )
                close()
            }
        }

        // Hard shadow
        drawPath(
            path = createDropletPath(shadowOffset, shadowOffset),
            color = Ink,
            style = Fill
        )

        // Droplet Body (Aqua)
        val bodyPath = createDropletPath(0f, 0f)
        drawPath(
            path = bodyPath,
            color = Aqua,
            style = Fill
        )

        // Droplet Border
        drawPath(
            path = bodyPath,
            color = Ink,
            style = Stroke(width = borderWidth)
        )

        // Highlights (glossy white patch)
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = w * 0.08f,
            center = Offset(w * 0.38f, h * 0.35f)
        )

        // Cheeks (Sun yellow blush)
        drawCircle(
            color = Sun,
            radius = w * 0.07f,
            center = Offset(w * 0.32f, h * 0.70f)
        )
        drawCircle(
            color = Sun,
            radius = w * 0.07f,
            center = Offset(w * 0.68f, h * 0.70f)
        )

        // Eyes (black circles with white reflection)
        drawCircle(
            color = Ink,
            radius = w * 0.06f,
            center = Offset(w * 0.40f, h * 0.62f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.02f,
            center = Offset(w * 0.38f, h * 0.60f)
        )

        drawCircle(
            color = Ink,
            radius = w * 0.06f,
            center = Offset(w * 0.60f, h * 0.62f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.02f,
            center = Offset(w * 0.58f, h * 0.60f)
        )

        // Smile
        val smilePath = Path().apply {
            moveTo(w * 0.44f, h * 0.72f)
            quadraticTo(
                w * 0.50f, h * 0.80f,
                w * 0.56f, h * 0.72f
            )
        }
        drawPath(
            path = smilePath,
            color = Ink,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
