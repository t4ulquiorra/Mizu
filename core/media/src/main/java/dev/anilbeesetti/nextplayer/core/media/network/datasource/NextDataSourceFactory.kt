package dev.anilbeesetti.nextplayer.core.media.network.datasource

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import org.koin.core.annotation.Single

/**
 * The player's data source factory for offline local media playback.
 */
@UnstableApi
@Single
class NextDataSourceFactory(private val context: Context) : DataSource.Factory {

    private val defaultFactory = DefaultDataSource.Factory(context)

    override fun createDataSource(): DataSource = defaultFactory.createDataSource()

    /** Disconnects resources held for playback. Call when the player is released. */
    suspend fun release() {}
}
