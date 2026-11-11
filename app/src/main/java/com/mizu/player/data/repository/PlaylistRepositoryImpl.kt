package com.mizu.player.data.repository

import com.mizu.player.data.local.dao.PlaylistDao
import com.mizu.player.data.local.dao.PlaylistItemDao
import com.mizu.player.data.local.entity.PlaylistEntity
import com.mizu.player.data.local.entity.PlaylistItemEntity
import com.mizu.player.domain.model.Playlist
import com.mizu.player.domain.model.PlaylistItem
import com.mizu.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAll().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> {
        return playlistItemDao.getItemsForPlaylist(playlistId).map { entities ->
            entities.map { entity ->
                PlaylistItem(
                    id = entity.id,
                    playlistId = entity.playlistId,
                    mediaUri = entity.mediaUri,
                    title = entity.title,
                    duration = entity.duration,
                    position = entity.position,
                    isVideo = entity.isVideo
                )
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistItemDao.deleteAllForPlaylist(playlistId)
        playlistDao.delete(playlistId)
    }

    override suspend fun addToPlaylist(playlistId: Long, item: PlaylistItem) {
        playlistItemDao.insert(
            PlaylistItemEntity(
                playlistId = playlistId,
                mediaUri = item.mediaUri,
                title = item.title,
                duration = item.duration,
                position = item.position,
                isVideo = item.isVideo
            )
        )
    }

    override suspend fun removeFromPlaylist(itemId: Long) {
        playlistItemDao.delete(itemId)
    }

    override suspend fun reorderPlaylist(playlistId: Long, items: List<PlaylistItem>) {
        val entities = items.mapIndexed { index, item ->
            PlaylistItemEntity(
                id = item.id,
                playlistId = playlistId,
                mediaUri = item.mediaUri,
                title = item.title,
                duration = item.duration,
                position = index,
                isVideo = item.isVideo
            )
        }
        playlistItemDao.updateAll(entities)
    }
}
