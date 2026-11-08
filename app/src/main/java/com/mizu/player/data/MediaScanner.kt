package com.mizu.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {

    private val SUBTITLE_EXTENSIONS = listOf("srt", "ssa", "ass", "vtt")

    suspend fun scanMedia(context: Context): Pair<List<FolderItem>, List<VideoItem>> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Untitled"
                    val path = it.getString(dataCol) ?: ""
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    val dateModified = it.getLong(dateCol)

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val subtitleType = findCompanionSubtitle(path)

                    val videoItem = VideoItem(
                        id = id,
                        title = name,
                        uri = uri,
                        path = path,
                        durationMs = duration,
                        sizeBytes = size,
                        dateModified = dateModified,
                        subtitleType = subtitleType
                    )

                    videoList.add(videoItem)

                    val parentDir = if (path.isNotEmpty()) File(path).parentFile else null
                    val folderPath = parentDir?.absolutePath ?: "Unknown"
                    folderMap.getOrPut(folderPath) { mutableListOf() }.add(videoItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val folderList = folderMap.map { (path, videos) ->
            val name = if (path != "Unknown") File(path).name else "Videos"
            FolderItem(
                name = name,
                path = path,
                videoCount = videos.size,
                videos = videos
            )
        }.sortedBy { it.name.lowercase() }

        Pair(folderList, videoList)
    }

    private fun findCompanionSubtitle(videoPath: String): String? {
        if (videoPath.isEmpty()) return null
        return try {
            val videoFile = File(videoPath)
            val parent = videoFile.parentFile ?: return null
            val baseName = videoFile.nameWithoutExtension

            for (ext in SUBTITLE_EXTENSIONS) {
                val directSub = File(parent, "$baseName.$ext")
                if (directSub.exists() && directSub.isFile) {
                    return ext.uppercase()
                }

                // Check for language tagged subtitles like movie.en.srt or movie.eng.srt
                val companion = parent.listFiles { _, fileName ->
                    fileName.startsWith(baseName, ignoreCase = true) &&
                            fileName.endsWith(".$ext", ignoreCase = true)
                }
                if (!companion.isNullOrEmpty()) {
                    return ext.uppercase()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
