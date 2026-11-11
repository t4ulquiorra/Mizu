package com.mizu.player.service

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mizu.player.engine.AudioTrackInfo
import com.mizu.player.engine.PlayerEngine
import com.mizu.player.engine.PlayerEngineListener
import com.mizu.player.engine.SubtitleTrackInfo
import com.mizu.player.model.VideoItem

@OptIn(UnstableApi::class)
class VlcMediaSessionAdapter(
    private val context: Context,
    private val engine: PlayerEngine,
    looper: Looper = Looper.getMainLooper()
) : SimpleBasePlayer(looper) {

    init {
        engine.addListener(object : PlayerEngineListener {
            override fun onPlaybackStateChanged(state: Int) {
                invalidateState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                invalidateState()
            }

            override fun onTimeChanged(currentTimeMs: Long, durationMs: Long) {
                invalidateState()
            }

            override fun onTracksChanged(audioTracks: List<AudioTrackInfo>, subtitleTracks: List<SubtitleTrackInfo>) {
                invalidateState()
            }

            override fun onMediaItemTransition(video: VideoItem, index: Int) {
                invalidateState()
            }

            override fun onError(errorMessage: String, canFallback: Boolean) {
                invalidateState()
            }

            override fun onNotice(notice: String) {
                // Non-state notice
            }
        })
    }

    override fun getState(): State {
        val playlist = engine.playlist
        val currentIdx = engine.currentIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))

        val mediaItemDataList = playlist.mapIndexed { index, video ->
            val mediaItem = PlaybackService.buildMediaItem(context, video)
            val durUs = if (video.durationMs > 0) video.durationMs * 1000L else (engine.durationMs * 1000L).coerceAtLeast(C.TIME_UNSET)
            MediaItemData.Builder(index)
                .setMediaItem(mediaItem)
                .setDurationUs(durUs)
                .build()
        }

        val commands = Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_PREPARE,
                COMMAND_STOP,
                COMMAND_SEEK_TO_MEDIA_ITEM,
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_TIMELINE,
                COMMAND_SET_SPEED_AND_PITCH
            )
            .build()

        val playState = when {
            playlist.isEmpty() -> Player.STATE_IDLE
            engine.durationMs <= 0 && engine.isPlaying -> Player.STATE_BUFFERING
            engine.isPlaying -> Player.STATE_READY
            else -> Player.STATE_READY
        }

        val builder = State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(engine.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playState)
            .setPlaybackParameters(PlaybackParameters(engine.currentPlaybackSpeed))
            .setContentPositionMs { engine.currentPositionMs }
            .setCurrentMediaItemIndex(currentIdx)

        if (mediaItemDataList.isNotEmpty()) {
            builder.setPlaylist(mediaItemDataList)
        }

        return builder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            engine.play()
        } else {
            engine.pause()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        engine.play()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        engine.pause()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        if (mediaItemIndex != engine.currentIndex && mediaItemIndex in engine.playlist.indices) {
            if (mediaItemIndex == engine.currentIndex + 1) {
                engine.next()
            } else if (mediaItemIndex == engine.currentIndex - 1) {
                engine.previous()
            } else {
                engine.loadQueue(engine.playlist, mediaItemIndex)
            }
        }
        if (positionMs != C.TIME_UNSET && positionMs >= 0) {
            engine.seekTo(positionMs)
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        engine.setPlaybackSpeed(playbackParameters.speed)
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
