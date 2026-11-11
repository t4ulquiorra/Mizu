package com.mizu.player.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US)).format(value) + " " + units[index]
    }

    fun formatDate(epochSeconds: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(epochSeconds * 1000))
    }

    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return ""
        val h = Math.min(width, height)
        return when {
            h >= 2160 -> "4K"
            h >= 1440 -> "2K"
            h >= 1080 -> "1080p"
            h >= 720 -> "720p"
            h >= 480 -> "480p"
            else -> "${width}x${height}"
        }
    }
}
