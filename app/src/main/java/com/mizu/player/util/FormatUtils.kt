package com.mizu.player.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "f4v", "wmv", "asf",
        "mpg", "mpeg", "ts", "m2ts", "mts", "vob", "3gp", "ogv", "rm", "rmvb",
        "mp3", "flac", "wav", "m4a", "ogg", "opus", "aac", "wma", "mka"
    )

    private val JUNK_PATTERNS = listOf(
        Regex("""(?i)\b(2160p|1080p|720p|480p|4k|uhd|fhd|hd)\b"""),
        Regex("""(?i)\b(x264|x265|h264|h265|hevc|avc|av1|xvid|divx|10bit|8bit)\b"""),
        Regex("""(?i)\b(web-?rip|web-?dl|bluray|bdrip|brrip|dvdrip|hdtv|remux)\b"""),
        Regex("""(?i)\b(dts-?hd|dts|truehd|atmos|aac2?\.?0?|aac5?\.?1?|ac3|flac|mp3)\b"""),
        Regex("""\[[A-F0-9]{8}\]"""), // CRC32 hashes like [7A3B12CD]
        Regex("""(?i)\b(dual-audio|multi-audio|subbed|dubbed)\b""")
    )

    fun cleanMediaTitle(rawName: String): String {
        var title = rawName.trim()

        // Strip known file extensions
        val dotIdx = title.lastIndexOf('.')
        if (dotIdx > 0) {
            val ext = title.substring(dotIdx + 1).lowercase()
            if (ext in EXTENSIONS) {
                title = title.substring(0, dotIdx)
            }
        }

        // Replace periods and underscores with spaces, but keep decimal points in numbers
        title = title.replace(Regex("""(?<=[a-zA-Z])\.(?=[a-zA-Z0-9])|(?<=[0-9])\.(?=[a-zA-Z])"""), " ")
        title = title.replace('_', ' ')

        // Remove square bracket tags that match junk or release groups at start/end
        title = title.replace(Regex("""^\[[^\]]+\]\s*"""), "") // leading release group like [SubsPlease]

        // Strip release tags
        for (pattern in JUNK_PATTERNS) {
            title = title.replace(pattern, "")
        }

        // Remove trailing hyphen or leftover symbols
        title = title.replace(Regex("""\s+-\s*$"""), "")
        title = title.replace(Regex("""\[\s*\]|\(\s*\)"""), "")

        // Collapse multiple whitespace
        title = title.replace(Regex("""\s+"""), " ").trim()

        return title.ifEmpty { rawName }
    }

    fun middleEllipsis(text: String, maxLength: Int): String {
        if (text.length <= maxLength || maxLength < 5) return text
        val keep = (maxLength - 1) / 2
        val start = text.take(keep).trimEnd()
        val end = text.takeLast(keep).trimStart()
        return "$start…$end"
    }

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
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
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
