package com.mizu.player.ui

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.VideoItem
import kotlinx.coroutines.delay
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    isInPipMode: Boolean = false,
    onPlaybackStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    // Check if media file is missing before preparing
    LaunchedEffect(video.path) {
        if (video.path.isNotEmpty() && !File(video.path).exists()) {
            errorMessage = "This file no longer exists"
            MediaRepository.removeVideo(context, video)
        }
    }

    // Configure Media3 AudioAttributes for audio focus and becoming-noisy pause
    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                val mediaItemBuilder = MediaItem.Builder().setUri(video.uri)

                // Attach companion subtitle if available
                if (video.subtitleType != null && video.path.isNotEmpty()) {
                    val videoFile = File(video.path)
                    val parent = videoFile.parentFile
                    val baseName = videoFile.nameWithoutExtension
                    val directSub = File(parent, "$baseName.${video.subtitleType.lowercase()}")
                    val subFile = if (directSub.exists()) directSub else File(parent, "$baseName.ass")

                    if (subFile.exists()) {
                        val mimeType = when (video.subtitleType.lowercase()) {
                            "srt" -> MimeTypes.APPLICATION_SUBRIP
                            "ssa", "ass" -> MimeTypes.TEXT_SSA
                            "vtt" -> MimeTypes.TEXT_VTT
                            else -> MimeTypes.APPLICATION_SUBRIP
                        }
                        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                            .setMimeType(mimeType)
                            .setLanguage("und")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                        mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                    }
                }

                setMediaItem(mediaItemBuilder.build())

                // Seamless resume: if previous playback position exists, seek immediately
                val savedPos = MediaRepository.getSavedPosition(video)
                if (savedPos > 3000L) {
                    seekTo(savedPos)
                }

                prepare()
                playWhenReady = true
            }
    }

    // Keep screen on while playing, release immediately when playback stops or player closes
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Listen to playback state changes, audio focus loss, errors, and end-of-stream
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = if (exoPlayer.duration > 0) exoPlayer.duration else video.durationMs
                if (playbackState == Player.STATE_ENDED) {
                    MediaRepository.savePlaybackPosition(context, video, 0L, dur)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlaybackStateChange(playing)
                val dur = if (exoPlayer.duration > 0) exoPlayer.duration else video.durationMs
                if (!playing) {
                    MediaRepository.savePlaybackPosition(context, video, exoPlayer.currentPosition, dur)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val isMissing = video.path.isNotEmpty() && !File(video.path).exists()
                if (isMissing) {
                    errorMessage = "This file no longer exists"
                    MediaRepository.removeVideo(context, video)
                } else {
                    errorMessage = "This video can't be played"
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            onPlaybackStateChange(false)
            val dur = if (exoPlayer.duration > 0) exoPlayer.duration else video.durationMs
            MediaRepository.savePlaybackPosition(context, video, exoPlayer.currentPosition, dur)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic position snapshot while actively playing (survives task-kill / power loss)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(2000)
            if (exoPlayer.isPlaying) {
                val dur = if (exoPlayer.duration > 0) exoPlayer.duration else video.durationMs
                MediaRepository.savePlaybackPosition(context, video, exoPlayer.currentPosition, dur)
            }
        }
    }

    if (errorMessage != null) {
        // Clear error view with message and Back button
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F14)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E222D),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Back",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        // Standard video player surface
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isInPipMode
                        keepScreenOn = isPlaying
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.useController = !isInPipMode
                    view.keepScreenOn = isPlaying
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Back button: visible only in full screen, hidden during Picture-in-Picture
            if (!isInPipMode) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color(0x66000000), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
