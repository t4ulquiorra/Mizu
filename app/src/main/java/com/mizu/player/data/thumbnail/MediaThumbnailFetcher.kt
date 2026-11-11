package com.mizu.player.data.thumbnail

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * High-performance thumbnail fetcher for local video and audio media.
 * - On API 29+: Leverages ContentResolver.loadThumbnail for fast OS-cached decoding.
 * - Fallback: MediaMetadataRetriever seeking to 10% of duration (never 0 ms to avoid initial black frames).
 */
class MediaThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val cr = context.contentResolver
        val scheme = data.scheme ?: ""

        // API 29+ ContentResolver.loadThumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE)
        ) {
            try {
                val size = Size(512, 384)
                val bitmap = cr.loadThumbnail(data, size, null)
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (_: Throwable) {
                // Fall through to retriever
            }
        }

        // Fallback: MediaMetadataRetriever seeking to 10% of duration
        return try {
            val retriever = MediaMetadataRetriever()
            if (scheme == ContentResolver.SCHEME_CONTENT) {
                retriever.setDataSource(context, data)
            } else {
                retriever.setDataSource(data.path ?: data.toString())
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val targetTimeUs = if (durationMs > 2000L) {
                (durationMs * 1000L) / 10L
            } else {
                1_000_000L
            }

            val frame: Bitmap? = retriever.getFrameAtTime(
                targetTimeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(0)

            retriever.release()

            if (frame != null) {
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, frame),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme
            if (scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE) {
                return MediaThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}
