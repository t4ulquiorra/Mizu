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
 * High-speed hardware thumbnail fetcher with in-memory LruCache.
 * Delivers cached video thumbnails in nanoseconds, eliminating scroll stutter.
 */
class AndroidThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    companion object {
        // Fast in-memory cache of 200 decoded thumbnails for silky 60/120fps scrolling
        private val memoryCache = object : LruCache<String, Bitmap>(200) {}
    }

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        val cached = memoryCache.get(key)
        if (cached != null) {
            return@withContext DrawableResult(
                drawable = BitmapDrawable(context.resources, cached),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Request 16:9 thumbnail from Android hardware media cache
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(320, 180), null)
                memoryCache.put(key, bitmap)
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
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
