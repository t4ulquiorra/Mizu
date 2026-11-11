package com.mizu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mizu.player.data.local.entity.FolderSeenEntity
import com.mizu.player.data.local.entity.PlaybackHistoryEntity
import com.mizu.player.data.local.entity.PlaylistEntity
import com.mizu.player.data.local.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun getContinueWatching(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: Long): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun delete(videoId: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun getItemCount(playlistId: Long): Flow<Int>
}

@Dao
interface PlaylistItemDao {
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert
    suspend fun insert(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllForPlaylist(playlistId: Long)

    @Update
    suspend fun updateAll(items: List<PlaylistItemEntity>)
}

@Dao
interface FolderSeenDao {
    @Query("SELECT * FROM folder_seen WHERE folderPath = :path")
    suspend fun getByPath(path: String): FolderSeenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FolderSeenEntity)

    @Query("SELECT * FROM folder_seen")
    suspend fun getAll(): List<FolderSeenEntity>
}
