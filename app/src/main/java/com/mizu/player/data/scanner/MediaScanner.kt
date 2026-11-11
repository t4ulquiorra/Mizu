package com.mizu.player.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.VideoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = cursor.getString(nameCol) ?: "Video $id"
                    val title = cursor.getString(titleCol) ?: displayName
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateModified = cursor.getLong(dateModCol)
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val mimeType = cursor.getString(mimeCol) ?: "video/*"

                    val folderPath = if (dataPath.isNotEmpty()) {
                        File(dataPath).parent ?: ""
                    } else {
                        "Internal Storage"
                    }
                    val folderName = if (folderPath.isNotEmpty()) {
                        File(folderPath).name.ifEmpty { "Videos" }
                    } else {
                        "Videos"
                    }

                    videos.add(
                        VideoItem(
                            id = id,
                            uri = contentUri,
                            title = title,
                            displayName = displayName,
                            duration = duration,
                            size = size,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            folderPath = folderPath,
                            folderName = folderName,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videos
    }

    suspend fun scanAudio(): List<AudioItem> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = cursor.getString(nameCol) ?: "Audio $id"
                    val title = cursor.getString(titleCol) ?: displayName
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val mimeType = cursor.getString(mimeCol) ?: "audio/*"
                    val albumId = cursor.getLong(albumIdCol)

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val folderPath = if (dataPath.isNotEmpty()) {
                        File(dataPath).parent ?: ""
                    } else {
                        "Music"
                    }
                    val folderName = if (folderPath.isNotEmpty()) {
                        File(folderPath).name.ifEmpty { "Audio" }
                    } else {
                        "Audio"
                    }

                    audioList.add(
                        AudioItem(
                            id = id,
                            uri = contentUri,
                            title = title,
                            displayName = displayName,
                            artist = artist,
                            album = album,
                            duration = duration,
                            size = size,
                            dateAdded = dateAdded,
                            folderPath = folderPath,
                            folderName = folderName,
                            mimeType = mimeType,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audioList
    }

    suspend fun scanDocumentTree(treeUri: Uri): Pair<List<VideoItem>, List<AudioItem>> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()
        val audio = mutableListOf<AudioItem>()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext Pair(emptyList(), emptyList())

        fun traverse(dir: DocumentFile, path: String) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    traverse(file, "$path/${file.name ?: "folder"}")
                } else {
                    val mime = file.type ?: ""
                    val name = file.name ?: "Media"
                    val size = file.length()
                    val lastMod = file.lastModified()
                    val uri = file.uri
                    val id = uri.hashCode().toLong()

                    if (mime.startsWith("video/") || name.endsWith(".mp4", true) ||
                        name.endsWith(".mkv", true) || name.endsWith(".webm", true) ||
                        name.endsWith(".avi", true) || name.endsWith(".mov", true)
                    ) {
                        videos.add(
                            VideoItem(
                                id = id,
                                uri = uri,
                                title = name.substringBeforeLast('.'),
                                displayName = name,
                                duration = 0L,
                                size = size,
                                width = 0,
                                height = 0,
                                dateAdded = lastMod / 1000,
                                dateModified = lastMod / 1000,
                                folderPath = path,
                                folderName = dir.name ?: "Folder",
                                mimeType = mime.ifEmpty { "video/*" }
                            )
                        )
                    } else if (mime.startsWith("audio/") || name.endsWith(".mp3", true) ||
                        name.endsWith(".flac", true) || name.endsWith(".wav", true) ||
                        name.endsWith(".m4a", true) || name.endsWith(".ogg", true) ||
                        name.endsWith(".opus", true)
                    ) {
                        audio.add(
                            AudioItem(
                                id = id,
                                uri = uri,
                                title = name.substringBeforeLast('.'),
                                displayName = name,
                                artist = "Unknown Artist",
                                album = "Unknown Album",
                                duration = 0L,
                                size = size,
                                dateAdded = lastMod / 1000,
                                folderPath = path,
                                folderName = dir.name ?: "Folder",
                                mimeType = mime.ifEmpty { "audio/*" }
                            )
                        )
                    }
                }
            }
        }

        traverse(root, root.name ?: "Picked Folder")
        Pair(videos, audio)
    }
}
