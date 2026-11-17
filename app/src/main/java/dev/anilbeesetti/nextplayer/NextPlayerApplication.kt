package dev.anilbeesetti.nextplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class NextPlayerApplication :
    Application(),
    SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin<NextPlayerApplication> {
            allowOverride(override = false)
            androidContext(androidContext = this@NextPlayerApplication)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
