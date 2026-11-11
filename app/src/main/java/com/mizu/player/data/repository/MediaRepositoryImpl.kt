package com.mizu.player.data.repository

import android.content.Context
import android.net.Uri
import com.mizu.player.data.local.dao.FolderSeenDao
import com.mizu.player.data.local.dao.PlaybackHistoryDao
import com.mizu.player.data.local.entity.FolderSeenEntity
import com.mizu.player.data.local.entity.PlaybackHistoryEntity
import com.mizu.player.data.scanner.MediaScanner
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.FolderInfo
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: MediaScanner,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val folderSeenDao: FolderSeenDao
) : MediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _videosFlow = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _audioFlow = MutableStateFlow<List<AudioItem>>(emptyList())

    init {
        scope.launch {
            scanMedia()
        }
    }

    override suspend fun scanMedia() {
        val scannedVideos = scanner.scanVideos()
        val scannedAudio = scanner.scanAudio()

        // Sync with playback history
        val seenFolders = folderSeenDao.getAll().associateBy { it.folderPath }
        val now = System.currentTimeMillis()

        // Update folders seen if new
        val currentFolderPaths = scannedVideos.map { it.folderPath }.toSet()
        for (folder in currentFolderPaths) {
            if (!seenFolders.containsKey(folder)) {
                folderSeenDao.upsert(FolderSeenEntity(folderPath = folder, lastSeenAt = now))
            }
        }

        _videosFlow.value = scannedVideos
        _audioFlow.value = scannedAudio
    }

    suspend fun addFolderTree(treeUri: Uri) {
        val (pickedVideos, pickedAudio) = scanner.scanDocumentTree(treeUri)
        _videosFlow.value = (_videosFlow.value + pickedVideos).distinctBy { it.uri.toString() }
        _audioFlow.value = (_audioFlow.value + pickedAudio).distinctBy { it.uri.toString() }
    }

    override fun getVideos(sortOrder: SortOrder): Flow<List<VideoItem>> {
        return combine(_videosFlow, playbackHistoryDao.getContinueWatching()) { videos, history ->
            val historyMap = history.associateBy { it.videoId }
            val mapped = videos.map { video ->
                val hist = historyMap[video.id]
                if (hist != null) {
                    video.copy(
                        lastPlayedPosition = hist.lastPosition,
                        lastPlayedAt = hist.lastPlayedAt
                    )
                } else {
                    video
                }
            }
            sortVideos(mapped, sortOrder)
        }
    }

    override fun getAudioTracks(sortOrder: SortOrder): Flow<List<AudioItem>> {
        return _audioFlow.map { audioList ->
            when (sortOrder) {
                SortOrder.NAME_ASC -> audioList.sortedBy { it.title.lowercase() }
                SortOrder.NAME_DESC -> audioList.sortedByDescending { it.title.lowercase() }
                SortOrder.DATE_ASC -> audioList.sortedBy { it.dateAdded }
                SortOrder.DATE_DESC -> audioList.sortedByDescending { it.dateAdded }
                SortOrder.SIZE_ASC -> audioList.sortedBy { it.size }
                SortOrder.SIZE_DESC -> audioList.sortedByDescending { it.size }
                SortOrder.DURATION_ASC -> audioList.sortedBy { it.duration }
                SortOrder.DURATION_DESC -> audioList.sortedByDescending { it.duration }
            }
        }
    }

    override fun getFolders(): Flow<List<FolderInfo>> {
        return combine(_videosFlow, _audioFlow) { videos, audio ->
            val videoGroups = videos.groupBy { it.folderPath }
            val audioGroups = audio.groupBy { it.folderPath }
            val allFolderPaths = (videoGroups.keys + audioGroups.keys).filter { it.isNotEmpty() }.toSet()

            allFolderPaths.map { folderPath ->
                val folderVideos = videoGroups[folderPath] ?: emptyList()
                val folderAudio = audioGroups[folderPath] ?: emptyList()
                val name = folderVideos.firstOrNull()?.folderName
                    ?: folderAudio.firstOrNull()?.folderName
                    ?: folderPath.substringAfterLast('/')

                val thumbnails = folderVideos.take(4).map { it.uri }
                val lastMod = folderVideos.maxOfOrNull { it.dateModified } ?: 0L

                FolderInfo(
                    path = folderPath,
                    name = name,
                    videoCount = folderVideos.size,
                    audioCount = folderAudio.size,
                    thumbnailUris = thumbnails,
                    lastModified = lastMod,
                    isNew = false
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    override fun getVideosInFolder(folderPath: String, sortOrder: SortOrder): Flow<List<VideoItem>> {
        return getVideos(sortOrder).map { videos ->
            videos.filter { it.folderPath == folderPath }
        }
    }

    override fun getAudioInFolder(folderPath: String): Flow<List<AudioItem>> {
        return _audioFlow.map { audioList ->
            audioList.filter { it.folderPath == folderPath }
        }
    }

    override fun searchVideos(query: String): Flow<List<VideoItem>> {
        val q = query.trim().lowercase()
        return _videosFlow.map { videos ->
            if (q.isEmpty()) emptyList()
            else videos.filter {
                it.title.lowercase().contains(q) ||
                        it.displayName.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }
    }

    override fun searchAudio(query: String): Flow<List<AudioItem>> {
        val q = query.trim().lowercase()
        return _audioFlow.map { audioList ->
            if (q.isEmpty()) emptyList()
            else audioList.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
            }
        }
    }

    override fun getContinueWatching(): Flow<List<VideoItem>> {
        return combine(_videosFlow, playbackHistoryDao.getContinueWatching()) { videos, history ->
            val videoMap = videos.associateBy { it.id }
            history.mapNotNull { entry ->
                videoMap[entry.videoId]?.copy(
                    lastPlayedPosition = entry.lastPosition,
                    lastPlayedAt = entry.lastPlayedAt
                )
            }.filter { it.lastPlayedPosition > 3000 && it.duration > 0 && it.lastPlayedPosition < (it.duration - 5000) }
        }
    }

    override suspend fun updatePlaybackPosition(videoId: Long, position: Long) {
        val video = _videosFlow.value.find { it.id == videoId }
        val now = System.currentTimeMillis()
        if (video != null) {
            playbackHistoryDao.upsert(
                PlaybackHistoryEntity(
                    videoId = videoId,
                    uri = video.uri.toString(),
                    title = video.title,
                    duration = video.duration,
                    lastPosition = position,
                    lastPlayedAt = now,
                    folderPath = video.folderPath
                )
            )
        }
    }

    override fun getVideoByUri(uri: Uri): Flow<VideoItem?> {
        return _videosFlow.map { list ->
            list.find { it.uri == uri || it.uri.toString() == uri.toString() }
        }
    }

    private fun sortVideos(videos: List<VideoItem>, sortOrder: SortOrder): List<VideoItem> {
        return when (sortOrder) {
            SortOrder.NAME_ASC -> videos.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> videos.sortedByDescending { it.title.lowercase() }
            SortOrder.DATE_ASC -> videos.sortedBy { it.dateModified }
            SortOrder.DATE_DESC -> videos.sortedByDescending { it.dateModified }
            SortOrder.SIZE_ASC -> videos.sortedBy { it.size }
            SortOrder.SIZE_DESC -> videos.sortedByDescending { it.size }
            SortOrder.DURATION_ASC -> videos.sortedBy { it.duration }
            SortOrder.DURATION_DESC -> videos.sortedByDescending { it.duration }
        }
    }
}
