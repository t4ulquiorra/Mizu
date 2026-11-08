package com.mizu.player.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
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
 * Fast hardware thumbnail fetcher that reads directly from Android's pre-rendered OS thumbnail cache.
 * Avoids expensive CPU-side video container demuxing and frame decoding.
 */
class AndroidThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Request a 16:9 thumbnail from the system's hardware media cache
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(360, 202), null)
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
