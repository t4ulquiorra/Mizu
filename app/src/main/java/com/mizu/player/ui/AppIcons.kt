package com.mizu.player.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for all icons used across the app.
 * Can be replaced by editing only this file.
 */
object AppIcons {
    private inline fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathBuilder = block
            )
        }.build()
    }

    val Close: ImageVector = icon("Close") {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }

    val Pip: ImageVector = icon("Pip") {
        moveTo(19f, 11f)
        horizontalLineToRelative(-8f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(-6f)
        close()
        moveTo(21f, 3f)
        horizontalLineTo(3f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(18f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(21f, 19f)
        horizontalLineTo(3f)
        verticalLineTo(5f)
        horizontalLineToRelative(18f)
        verticalLineToRelative(14f)
        close()
    }

    val Volume: ImageVector = icon("Volume") {
        moveTo(3f, 9f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(4f)
        lineToRelative(5f, 5f)
        verticalLineTo(4f)
        lineTo(7f, 9f)
        horizontalLineTo(3f)
        close()
        moveTo(16.5f, 12f)
        curveToRelative(0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
        verticalLineToRelative(8.05f)
        curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
        close()
        moveTo(14f, 3.23f)
        verticalLineToRelative(2.06f)
        curveToRelative(2.89f, 0.86f, 5f, 3.54f, 5f, 6.71f)
        reflectiveCurveToRelative(-2.11f, 5.85f, -5f, 6.71f)
        verticalLineToRelative(2.06f)
        curveToRelative(4.01f, -0.91f, 7f, -4.49f, 7f, -8.77f)
        reflectiveCurveToRelative(-2.99f, -7.86f, -7f, -8.77f)
        close()
    }

    val Replay10: ImageVector = icon("Replay10") {
        moveTo(12f, 5f)
        verticalLineTo(1f)
        lineTo(7f, 6f)
        lineToRelative(5f, 5f)
        verticalLineTo(7f)
        curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
        reflectiveCurveToRelative(-2.69f, 6f, -6f, 6f)
        reflectiveCurveToRelative(-6f, -2.69f, -6f, -6f)
        horizontalLineTo(4f)
        curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
        reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
        reflectiveCurveToRelative(-3.58f, -8f, -8f, -8f)
        close()
        moveTo(10.25f, 10.5f)
        verticalLineToRelative(5f)
        horizontalLineToRelative(1f)
        verticalLineToRelative(-5f)
        close()
        moveTo(13.75f, 10.5f)
        curveToRelative(-0.7f, 0f, -1.25f, 0.5f, -1.25f, 1.25f)
        verticalLineToRelative(2.5f)
        curveToRelative(0f, 0.75f, 0.55f, 1.25f, 1.25f, 1.25f)
        reflectiveCurveToRelative(1.25f, -0.5f, 1.25f, -1.25f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -0.75f, -0.55f, -1.25f, -1.25f, -1.25f)
        close()
        moveTo(14.1f, 14.25f)
        curveToRelative(0f, 0.25f, -0.15f, 0.45f, -0.35f, 0.45f)
        reflectiveCurveToRelative(-0.35f, -0.2f, -0.35f, -0.45f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -0.25f, 0.15f, -0.45f, 0.35f, -0.45f)
        reflectiveCurveToRelative(0.35f, 0.2f, 0.35f, 0.45f)
        verticalLineToRelative(2.5f)
        close()
    }

    val Forward10: ImageVector = icon("Forward10") {
        moveTo(12f, 5f)
        verticalLineTo(1f)
        lineToRelative(5f, 5f)
        lineToRelative(-5f, 5f)
        verticalLineTo(7f)
        curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
        reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
        reflectiveCurveToRelative(6f, -2.69f, 6f, -6f)
        horizontalLineToRelative(2f)
        curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
        reflectiveCurveToRelative(-8f, -3.58f, -8f, -8f)
        reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
        close()
        moveTo(10.25f, 10.5f)
        verticalLineToRelative(5f)
        horizontalLineToRelative(1f)
        verticalLineToRelative(-5f)
        close()
        moveTo(13.75f, 10.5f)
        curveToRelative(-0.7f, 0f, -1.25f, 0.5f, -1.25f, 1.25f)
        verticalLineToRelative(2.5f)
        curveToRelative(0f, 0.75f, 0.55f, 1.25f, 1.25f, 1.25f)
        reflectiveCurveToRelative(1.25f, -0.5f, 1.25f, -1.25f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -0.75f, -0.55f, -1.25f, -1.25f, -1.25f)
        close()
        moveTo(14.1f, 14.25f)
        curveToRelative(0f, 0.25f, -0.15f, 0.45f, -0.35f, 0.45f)
        reflectiveCurveToRelative(-0.35f, -0.2f, -0.35f, -0.45f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -0.25f, 0.15f, -0.45f, 0.35f, -0.45f)
        reflectiveCurveToRelative(0.35f, 0.2f, 0.35f, 0.45f)
        verticalLineToRelative(2.5f)
        close()
    }

    val Play: ImageVector = icon("Play") {
        moveTo(8f, 5f)
        verticalLineToRelative(14f)
        lineToRelative(11f, -7f)
        close()
    }

    val Pause: ImageVector = icon("Pause") {
        moveTo(6f, 19f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineTo(6f)
        verticalLineToRelative(14f)
        close()
        moveTo(14f, 5f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineToRelative(-4f)
        close()
    }

    val Subtitles: ImageVector = icon("Subtitles") {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(12f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(20f, 18f)
        horizontalLineTo(4f)
        verticalLineTo(6f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(12f)
        close()
        moveTo(6f, 10f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        horizontalLineTo(6f)
        close()
        moveTo(6f, 14f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(2f)
        horizontalLineTo(6f)
        close()
        moveTo(16f, 14f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(10f, 10f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(-8f)
        close()
    }

    val AudioTrack: ImageVector = icon("AudioTrack") {
        moveTo(12f, 3f)
        verticalLineToRelative(10.55f)
        curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2f, -0.55f)
        curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
        verticalLineTo(7f)
        horizontalLineToRelative(4f)
        verticalLineTo(3f)
        horizontalLineToRelative(-6f)
        close()
    }

    val Speed: ImageVector = icon("Speed") {
        moveTo(20.38f, 8.57f)
        lineToRelative(-1.23f, 1.85f)
        curveToRelative(0.53f, 2.37f, -0.06f, 4.93f, -1.6f, 6.84f)
        curveToRelative(-2.03f, 2.53f, -5.39f, 3.52f, -8.45f, 2.47f)
        curveToRelative(-3.06f, -1.05f, -5.16f, -3.88f, -5.16f, -7.11f)
        curveToRelative(0f, -3.27f, 2.15f, -6.13f, 5.25f, -7.15f)
        curveToRelative(1.86f, -0.61f, 3.86f, -0.42f, 5.56f, 0.45f)
        lineToRelative(1.85f, -1.23f)
        curveTo(14.88f, 3.53f, 12.46f, 3.23f, 10.15f, 3.99f)
        curveTo(6.01f, 5.35f, 3.14f, 9.17f, 3.14f, 13.53f)
        curveToRelative(0f, 4.31f, 2.8f, 8.08f, 6.88f, 9.48f)
        curveToRelative(4.08f, 1.4f, 8.56f, 0.08f, 11.27f, -3.3f)
        curveToRelative(2.05f, -2.55f, 2.84f, -5.96f, 2.14f, -9.11f)
        lineToRelative(-3.05f, -2.03f)
        close()
        moveTo(10.59f, 15.41f)
        curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0f)
        lineToRelative(4.58f, -4.58f)
        lineToRelative(-0.7f, -0.7f)
        lineToRelative(-4.59f, 4.58f)
        curveToRelative(-0.59f, -0.3f, -1.33f, -0.2f, -1.82f, 0.29f)
        curveToRelative(-0.59f, 0.59f, -0.59f, 1.54f, 0f, 2.13f)
        close()
    }
}
