package com.mizu.player.di

import com.mizu.player.data.repository.MediaRepositoryImpl
import com.mizu.player.data.repository.PlaylistRepositoryImpl
import com.mizu.player.domain.repository.MediaRepository
import com.mizu.player.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository
}
