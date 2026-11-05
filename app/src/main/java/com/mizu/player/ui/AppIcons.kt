package com.mizu.player.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
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
    private inline fun icon(
        name: String,
        viewportWidth: Float = 24f,
        viewportHeight: Float = 24f,
        fillType: PathFillType = PathFillType.NonZero,
        block: PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = fillType,
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

    val Brightness: ImageVector = icon("Brightness") {
        moveTo(12f, 7.5f)
        curveTo(9.51f, 7.5f, 7.5f, 9.51f, 7.5f, 12f)
        curveTo(7.5f, 14.49f, 9.51f, 16.5f, 12f, 16.5f)
        curveTo(14.49f, 16.5f, 16.5f, 14.49f, 16.5f, 12f)
        curveTo(16.5f, 9.51f, 14.49f, 7.5f, 12f, 7.5f)
        close()
        moveTo(11f, 1f)
        horizontalLineTo(13f)
        verticalLineTo(4.5f)
        horizontalLineTo(11f)
        close()
        moveTo(11f, 19.5f)
        horizontalLineTo(13f)
        verticalLineTo(23f)
        horizontalLineTo(11f)
        close()
        moveTo(1f, 11f)
        verticalLineTo(13f)
        horizontalLineTo(4.5f)
        verticalLineTo(11f)
        close()
        moveTo(19.5f, 11f)
        verticalLineTo(13f)
        horizontalLineTo(23f)
        verticalLineTo(11f)
        close()
        moveTo(4.45f, 3.05f)
        lineTo(3.05f, 4.45f)
        lineTo(5.52f, 6.92f)
        lineTo(6.92f, 5.52f)
        close()
        moveTo(17.08f, 18.48f)
        lineTo(18.48f, 17.08f)
        lineTo(20.95f, 19.55f)
        lineTo(19.55f, 20.95f)
        close()
        moveTo(5.52f, 17.08f)
        lineTo(3.05f, 19.55f)
        lineTo(4.45f, 20.95f)
        lineTo(6.92f, 18.48f)
        close()
        moveTo(19.55f, 3.05f)
        lineTo(17.08f, 5.52f)
        lineTo(18.48f, 6.92f)
        lineTo(20.95f, 4.45f)
        close()
    }

    // Play: centered inside square viewport (85.42 x 85.42) for 1:1 aspect ratio
    val Play: ImageVector = icon("Play", viewportWidth = 85.42f, viewportHeight = 85.42f) {
        moveTo(80.23f, 50.56f)
        arcTo(8.79f, 8.79f, 0.0f, false, false, 80.23f, 34.87f)
        lineTo(13.12f, 0.94f)
        arcTo(8.79f, 8.79f, 0.0f, false, false, 0.36f, 8.79f)
        lineTo(0.36f, 76.64f)
        arcTo(8.79f, 8.79f, 0.0f, false, false, 13.12f, 84.48f)
        close()
    }

    // Pause: centered inside square viewport (84.84 x 84.84) for 1:1 aspect ratio
    val Pause: ImageVector = icon("Pause", viewportWidth = 84.84f, viewportHeight = 84.84f) {
        moveTo(15.64f, 0.00f)
        lineTo(28.27f, 0.00f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 36.51f, 8.24f)
        lineTo(36.51f, 76.60f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 28.27f, 84.84f)
        lineTo(15.64f, 84.84f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 7.40f, 76.60f)
        lineTo(7.40f, 8.24f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 15.64f, 0.00f)
        close()
        moveTo(56.57f, 0.00f)
        lineTo(69.20f, 0.00f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 77.44f, 8.24f)
        lineTo(77.44f, 76.60f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 69.20f, 84.84f)
        lineTo(56.57f, 84.84f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 48.33f, 76.60f)
        lineTo(48.33f, 8.24f)
        arcTo(8.24f, 8.24f, 0.0f, false, true, 56.57f, 0.00f)
        close()
    }

    // Previous: centered inside square viewport (97.61 x 97.61) for 1:1 aspect ratio without stretching
    val Previous: ImageVector = icon("Previous", viewportWidth = 97.61f, viewportHeight = 97.61f) {
        moveTo(2.48f, 53.12f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 2.48f, 44.49f)
        lineTo(41.42f, 21.85f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 48.92f, 26.17f)
        lineTo(48.92f, 71.44f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 41.42f, 75.75f)
        close()
        moveTo(51.17f, 53.12f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 51.17f, 44.49f)
        lineTo(90.11f, 21.85f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 97.61f, 26.17f)
        lineTo(97.61f, 71.44f)
        arcTo(4.99f, 4.99f, 0.0f, false, true, 90.11f, 75.75f)
        close()
    }

    // Next: centered inside square viewport (97.47 x 97.47) for 1:1 aspect ratio without stretching
    val Next: ImageVector = icon("Next", viewportWidth = 97.47f, viewportHeight = 97.47f) {
        moveTo(95.01f, 53.03f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 95.01f, 44.45f)
        lineTo(56.10f, 21.77f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 48.63f, 26.05f)
        lineTo(48.63f, 71.41f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 56.10f, 75.70f)
        close()
        moveTo(46.37f, 53.03f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 46.37f, 44.45f)
        lineTo(7.47f, 21.77f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 0.00f, 26.05f)
        lineTo(0.00f, 71.41f)
        arcTo(4.97f, 4.97f, 0.0f, false, false, 7.47f, 75.70f)
        close()
    }

    // More: centered inside square viewport (40.07 x 40.07) for 1:1 aspect ratio
    val More: ImageVector = icon("More", viewportWidth = 40.07f, viewportHeight = 40.07f) {
        moveTo(20.03f, 0.00f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 10.04f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 0.00f)
        close()
        moveTo(20.03f, 15.01f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 25.05f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 15.01f)
        close()
        moveTo(20.03f, 30.03f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 40.07f)
        arcTo(5.02f, 5.02f, 0.0f, true, true, 20.03f, 30.03f)
        close()
    }

    // Subtitles: speech bubble with two lines
    val Subtitles: ImageVector = icon("Subtitles", fillType = PathFillType.EvenOdd) {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
        verticalLineTo(16f)
        curveTo(2f, 17.1f, 2.9f, 18f, 4f, 18f)
        horizontalLineTo(7f)
        verticalLineTo(21.5f)
        lineTo(10.5f, 18f)
        horizontalLineTo(20f)
        curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
        verticalLineTo(4f)
        curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
        close()
        // Cut out interior
        moveTo(20f, 4f)
        verticalLineTo(16f)
        horizontalLineTo(10f)
        lineTo(8f, 18f)
        verticalLineTo(16f)
        horizontalLineTo(4f)
        verticalLineTo(4f)
        horizontalLineTo(20f)
        close()
        // Line 1
        moveTo(7f, 7.5f)
        horizontalLineTo(17f)
        verticalLineTo(9.5f)
        horizontalLineTo(7f)
        close()
        // Line 2
        moveTo(7f, 11.5f)
        horizontalLineTo(14f)
        verticalLineTo(13.5f)
        horizontalLineTo(7f)
        close()
    }

    // AudioTrack: vertical waveform
    val AudioTrack: ImageVector = icon("AudioTrack") {
        moveTo(5f, 9f)
        curveTo(5.55f, 9f, 6f, 9.45f, 6f, 10f)
        verticalLineTo(14f)
        curveTo(6f, 14.55f, 5.55f, 15f, 5f, 15f)
        curveTo(4.45f, 15f, 4f, 14.55f, 4f, 14f)
        verticalLineTo(10f)
        curveTo(4f, 9.45f, 4.45f, 9f, 5f, 9f)
        close()
        moveTo(9f, 5f)
        curveTo(9.55f, 5f, 10f, 5.45f, 10f, 6f)
        verticalLineTo(18f)
        curveTo(10f, 18.55f, 9.55f, 19f, 9f, 19f)
        curveTo(8.45f, 19f, 8f, 18.55f, 8f, 18f)
        verticalLineTo(6f)
        curveTo(8f, 5.45f, 8.45f, 5f, 9f, 5f)
        close()
        moveTo(13f, 3f)
        curveTo(13.55f, 3f, 14f, 3.45f, 14f, 4f)
        verticalLineTo(20f)
        curveTo(14f, 20.55f, 13.55f, 21f, 13f, 21f)
        curveTo(12.45f, 21f, 12f, 20.55f, 12f, 20f)
        verticalLineTo(4f)
        curveTo(12f, 3.45f, 12.45f, 3f, 13f, 3f)
        close()
        moveTo(17f, 6f)
        curveTo(17.55f, 6f, 18f, 6.45f, 18f, 7f)
        verticalLineTo(17f)
        curveTo(18f, 17.55f, 17.55f, 18f, 17f, 18f)
        curveTo(16.45f, 18f, 16f, 17.55f, 16f, 17f)
        verticalLineTo(7f)
        curveTo(16f, 6.45f, 16.45f, 6f, 17f, 6f)
        close()
        moveTo(21f, 9f)
        curveTo(21.55f, 9f, 22f, 9.45f, 22f, 10f)
        verticalLineTo(14f)
        curveTo(22f, 14.55f, 21.55f, 15f, 21f, 15f)
        curveTo(20.45f, 15f, 20f, 14.55f, 20f, 14f)
        verticalLineTo(10f)
        curveTo(20f, 9.45f, 20.45f, 9f, 21f, 9f)
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
