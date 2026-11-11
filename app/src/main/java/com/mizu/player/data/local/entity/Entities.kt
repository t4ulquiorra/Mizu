package com.mizu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val videoId: Long,
    val uri: String,
    val title: String,
    val duration: Long,
    val lastPosition: Long,
    val lastPlayedAt: Long,
    val folderPath: String
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val mediaUri: String,
    val title: String,
    val duration: Long,
    val position: Int,
    val isVideo: Boolean
)

@Entity(tableName = "folder_seen")
data class FolderSeenEntity(
    @PrimaryKey
    val folderPath: String,
    val lastSeenAt: Long
)
