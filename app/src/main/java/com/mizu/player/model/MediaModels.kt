package com.mizu.player.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class VideoItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModified: Long,
    val subtitleType: String? = null
) {
    fun formattedDuration(): String {
        if (durationMs <= 0) return "00:00"
        val totalSec = durationMs / 1000
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hr = totalSec / 3600
        return if (hr > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hr, min, sec)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        }
    }

    fun formattedSize(): String {
        if (sizeBytes <= 0) return ""
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}

@Immutable
data class FolderItem(
    val name: String,
    val path: String,
    val videoCount: Int,
    val videos: List<VideoItem>
)
