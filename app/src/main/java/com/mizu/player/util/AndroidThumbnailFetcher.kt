package com.mizu.player.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-speed hardware thumbnail fetcher with in-memory LruCache and automatic downscaling.
 * Prevents giant 1080p uncompressed bitmaps from exhausting RAM or stalling the GPU.
 */
class AndroidThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    companion object {
        // Fast in-memory cache of downsampled 320x180 thumbnails (~200KB each)
        private val memoryCache = object : LruCache<String, Bitmap>(200) {}
    }

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        val cached = memoryCache.get(key)
        if (cached != null && !cached.isRecycled) {
            return@withContext DrawableResult(
                drawable = BitmapDrawable(context.resources, cached),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Request hardware thumbnail
                val rawBitmap = context.contentResolver.loadThumbnail(uri, Size(320, 180), null)

                // Downscale if Android returned a giant full-resolution frame
                val finalBitmap = if (rawBitmap.width > 360 || rawBitmap.height > 240) {
                    val targetW = 320
                    val targetH = (320f * rawBitmap.height / rawBitmap.width).toInt().coerceAtLeast(180)
                    val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                    if (scaled != rawBitmap) {
                        rawBitmap.recycle()
                    }
                    scaled
                } else {
                    rawBitmap
                }

                memoryCache.put(key, finalBitmap)
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, finalBitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == ContentResolver.SCHEME_CONTENT && data.toString().contains("video")) {
                return AndroidThumbnailFetcher(context, data)
            }
            return null
        }
    }
}
