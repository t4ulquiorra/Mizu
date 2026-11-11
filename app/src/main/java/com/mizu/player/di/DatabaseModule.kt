package com.mizu.player.di

import android.content.Context
import androidx.room.Room
import com.mizu.player.data.local.dao.FolderSeenDao
import com.mizu.player.data.local.dao.PlaybackHistoryDao
import com.mizu.player.data.local.dao.PlaylistDao
import com.mizu.player.data.local.dao.PlaylistItemDao
import com.mizu.player.data.local.database.MizuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMizuDatabase(@ApplicationContext context: Context): MizuDatabase {
        return Room.databaseBuilder(
            context,
            MizuDatabase::class.java,
            "mizu_player.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePlaybackHistoryDao(database: MizuDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }

    @Provides
    fun providePlaylistDao(database: MizuDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun providePlaylistItemDao(database: MizuDatabase): PlaylistItemDao {
        return database.playlistItemDao()
    }

    @Provides
    fun provideFolderSeenDao(database: MizuDatabase): FolderSeenDao {
        return database.folderSeenDao()
    }
}
