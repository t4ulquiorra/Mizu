package com.mizu.player.util

import java.util.Locale

/**
 * Helpers for formatting video playback elapsed and remaining timestamps.
 */
object TimeFormatter {
    fun formatElapsed(positionMs: Long, showHours: Boolean): String {
        val totalSecs = (positionMs.coerceAtLeast(0L) / 1000)
        val s = totalSecs % 60
        val m = (totalSecs / 60) % 60
        val h = totalSecs / 3600

        return if (showHours || h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun formatRemaining(currentMs: Long, durationMs: Long, showHours: Boolean): String {
        if (durationMs <= 0L) return "--:--"
        val remainingMs = (durationMs - currentMs).coerceAtLeast(0L)
        val totalSecs = remainingMs / 1000
        val s = totalSecs % 60
        val m = (totalSecs / 60) % 60
        val h = totalSecs / 3600

        return if (showHours || h > 0) {
            String.format(Locale.US, "-%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "-%02d:%02d", m, s)
        }
    }
}
