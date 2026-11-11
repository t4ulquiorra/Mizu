package com.mizu.player.domain.repository

import android.net.Uri
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.FolderInfo
import com.mizu.player.domain.model.Playlist
import com.mizu.player.domain.model.PlaylistItem
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getVideos(sortOrder: SortOrder = SortOrder.DATE_DESC): Flow<List<VideoItem>>
    fun getAudioTracks(sortOrder: SortOrder = SortOrder.NAME_ASC): Flow<List<AudioItem>>
    fun getFolders(): Flow<List<FolderInfo>>
    fun getVideosInFolder(folderPath: String, sortOrder: SortOrder = SortOrder.NAME_ASC): Flow<List<VideoItem>>
    fun getAudioInFolder(folderPath: String): Flow<List<AudioItem>>
    fun searchVideos(query: String): Flow<List<VideoItem>>
    fun searchAudio(query: String): Flow<List<AudioItem>>
    fun getContinueWatching(): Flow<List<VideoItem>>
    suspend fun updatePlaybackPosition(videoId: Long, position: Long)
    suspend fun scanMedia()
    fun getVideoByUri(uri: Uri): Flow<VideoItem?>
}

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addToPlaylist(playlistId: Long, item: PlaylistItem)
    suspend fun removeFromPlaylist(itemId: Long)
    suspend fun reorderPlaylist(playlistId: Long, items: List<PlaylistItem>)
}
