package com.mizu.player.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mizu.player.model.VideoItem
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItemBuilder = MediaItem.Builder().setUri(video.uri)

            // Attach companion subtitle if available
            if (video.subtitleType != null && video.path.isNotEmpty()) {
                val videoFile = File(video.path)
                val parent = videoFile.parentFile
                val baseName = videoFile.nameWithoutExtension
                val subFile = File(parent, "$baseName.${video.subtitleType.lowercase()}")

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
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x66000000), shape = androidx.compose.foundation.shape.CircleShape)
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
