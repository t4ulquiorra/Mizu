package com.mizu.player.domain.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val duration: Long, // ms
    val size: Long, // bytes
    val width: Int,
    val height: Int,
    val dateAdded: Long, // epoch seconds
    val dateModified: Long,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val lastPlayedPosition: Long = 0, // ms
    val lastPlayedAt: Long = 0
)

data class AudioItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val albumArtUri: Uri? = null
)

data class FolderInfo(
    val path: String,
    val name: String,
    val videoCount: Int,
    val audioCount: Int,
    val thumbnailUris: List<Uri>,
    val lastModified: Long,
    val isNew: Boolean = false
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0
)

data class PlaylistItem(
    val id: Long = 0,
    val playlistId: Long,
    val mediaUri: String,
    val title: String,
    val duration: Long,
    val position: Int, // order
    val isVideo: Boolean
)

data class SubtitleTrack(
    val index: Int,
    val label: String,
    val language: String?,
    val mimeType: String?,
    val uri: Uri? = null,
    val isEmbedded: Boolean = true
)

data class AudioTrackInfo(
    val index: Int,
    val label: String,
    val language: String?,
    val channelCount: Int,
    val sampleRate: Int
)

enum class PlayerEngine {
    EXOPLAYER,
    VLC
}

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DURATION_ASC,
    DURATION_DESC
}

enum class ViewMode {
    GRID,
    LIST
}
