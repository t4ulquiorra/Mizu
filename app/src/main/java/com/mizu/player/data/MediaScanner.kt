package com.mizu.player.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.provider.MediaStore
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {

    private val SUBTITLE_EXTENSIONS = setOf("srt", "ssa", "ass", "vtt", "sub")

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

        val rawVideos = mutableListOf<RawVideo>()
        val uniqueFolderPaths = mutableSetOf<String>()

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

                    val parentPath = if (path.isNotEmpty()) {
                        File(path).parent ?: "Unknown"
                    } else {
                        "Unknown"
                    }

                    if (parentPath != "Unknown") {
                        uniqueFolderPaths.add(parentPath)
                    }

                    rawVideos.add(
                        RawVideo(
                            id = id,
                            title = name,
                            path = path,
                            parentPath = parentPath,
                            duration = duration,
                            size = size,
                            dateModified = dateModified
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // STEP 1: Scan unique folders ONCE in batch to collect companion subtitles (O(1) lookups per folder)
        val folderSubtitles = mutableMapOf<String, List<File>>()
        for (folderPath in uniqueFolderPaths) {
            try {
                val dir = File(folderPath)
                if (dir.exists() && dir.isDirectory) {
                    val subs = dir.listFiles { _, fileName ->
                        val ext = fileName.substringAfterLast('.', "").lowercase()
                        ext in SUBTITLE_EXTENSIONS
                    }?.toList() ?: emptyList()
                    folderSubtitles[folderPath] = subs
                }
            } catch (e: Exception) {
                // Ignore folder read error
            }
        }

        // STEP 2: Process videos with fast in-memory matching
        for (raw in rawVideos) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                raw.id
            )

            val subsInFolder = folderSubtitles[raw.parentPath] ?: emptyList()
            var detectedSubtitle = findCompanionSubtitle(raw.path, subsInFolder)

            // If no companion subtitle file on disk, check for embedded subtitle tracks in container
            if (detectedSubtitle == null && raw.path.isNotEmpty()) {
                val ext = raw.path.substringAfterLast('.', "").lowercase()
                if (ext == "mkv" || ext == "mp4") {
                    detectedSubtitle = checkEmbeddedSubtitle(raw.path)
                }
            }

            val videoItem = VideoItem(
                id = raw.id,
                title = raw.title,
                uri = uri,
                path = raw.path,
                durationMs = raw.duration,
                sizeBytes = raw.size,
                dateModified = raw.dateModified,
                subtitleType = detectedSubtitle
            )

            videoList.add(videoItem)
            folderMap.getOrPut(raw.parentPath) { mutableListOf() }.add(videoItem)
        }

        val folderList = folderMap.map { (path, videos) ->
            val name = if (path != "Unknown") File(path).name else "Videos"
            FolderItem(
                name = name,
                path = path,
                videoCount = videos.size,
                videos = videos
            )
        }

        Pair(folderList, videoList)
    }

    private fun findCompanionSubtitle(videoPath: String, subsInFolder: List<File>): String? {
        if (videoPath.isEmpty() || subsInFolder.isEmpty()) return null
        return try {
            val videoBase = File(videoPath).nameWithoutExtension.lowercase()

            // 1. Check exact base name match (e.g. video.mp4 -> video.srt)
            val exact = subsInFolder.firstOrNull {
                it.nameWithoutExtension.equals(videoBase, ignoreCase = true)
            }
            if (exact != null) {
                return exact.extension.uppercase()
            }

            // 2. Check prefix / language match (e.g. video.mp4 -> video.en.srt, video.eng.ass)
            val prefixMatch = subsInFolder.firstOrNull {
                it.nameWithoutExtension.lowercase().startsWith(videoBase)
            }
            if (prefixMatch != null) {
                return prefixMatch.extension.uppercase()
            }

            // 3. Check contains / relaxed match
            val relaxedMatch = subsInFolder.firstOrNull {
                val subBase = it.nameWithoutExtension.lowercase()
                videoBase.startsWith(subBase) || videoBase.contains(subBase)
            }
            relaxedMatch?.extension?.uppercase()
        } catch (e: Exception) {
            null
        }
    }

    private fun checkEmbeddedSubtitle(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null

            val extractor = MediaExtractor()
            extractor.setDataSource(path)
            var subType: String? = null
            val trackCount = extractor.trackCount

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("text/") || mime.startsWith("application/")) {
                    when {
                        mime.contains("subrip") || mime.contains("srt") -> {
                            subType = "SRT"
                            break
                        }
                        mime.contains("ssa") -> {
                            subType = "SSA"
                            break
                        }
                        mime.contains("ass") -> {
                            subType = "ASS"
                            break
                        }
                        mime.contains("vtt") -> {
                            subType = "VTT"
                            break
                        }
                        mime.contains("pgs") || mime.contains("sub") -> {
                            subType = "SUB"
                            break
                        }
                    }
                }
            }
            extractor.release()
            subType
        } catch (e: Throwable) {
            null
        }
    }

    private data class RawVideo(
        val id: Long,
        val title: String,
        val path: String,
        val parentPath: String,
        val duration: Long,
        val size: Long,
        val dateModified: Long
    )
}
