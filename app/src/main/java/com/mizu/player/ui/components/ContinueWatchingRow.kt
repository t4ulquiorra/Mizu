package com.mizu.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils

@Composable
fun ContinueWatchingRow(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return

    val colors = MizuThemeObj.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CONTINUE WATCHING",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(videos, key = { it.id }) { video ->
                ContinueWatchingCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (video.duration > 0) {
        (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    NeuCard(
        modifier = modifier.width(180.dp),
        onClick = onClick
    ) {
        Column {
            // Thumbnail with play overlay & duration badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Ink)
            ) {
                AsyncImage(
                    model = video.uri,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                // Play icon button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Sun)
                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Progress Bar at bottom of thumbnail
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = Coral,
                    trackColor = Ink.copy(alpha = 0.5f)
                )
            }

            // Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${FormatUtils.formatDuration(video.lastPlayedPosition)} / ${FormatUtils.formatDuration(video.duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.copy(alpha = 0.7f)
                )
            }
        }
    }
}
