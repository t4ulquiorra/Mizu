package com.mizu.player.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mizu.player.data.local.dao.FolderSeenDao
import com.mizu.player.data.local.dao.PlaybackHistoryDao
import com.mizu.player.data.local.dao.PlaylistDao
import com.mizu.player.data.local.dao.PlaylistItemDao
import com.mizu.player.data.local.entity.FolderSeenEntity
import com.mizu.player.data.local.entity.PlaybackHistoryEntity
import com.mizu.player.data.local.entity.PlaylistEntity
import com.mizu.player.data.local.entity.PlaylistItemEntity

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FolderSeenEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MizuDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun folderSeenDao(): FolderSeenDao
}
