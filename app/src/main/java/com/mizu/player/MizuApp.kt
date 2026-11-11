package com.mizu.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.mizu.player.data.thumbnail.MediaThumbnailFetcher
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.mizu.player.util.PlaybackLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MizuApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        PlaybackLog.init(this)

        // Global uncaught exception logger
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            PlaybackLog.log("FATAL_CRASH", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(MediaThumbnailFetcher.Factory(this@MizuApp))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("mizu_thumbnails"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
