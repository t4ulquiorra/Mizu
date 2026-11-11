package com.mizu.player.ui.screens.playlist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.domain.model.PlaylistItem
import com.mizu.player.ui.components.NeuButton
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    items: List<PlaylistItem>,
    onItemClick: (PlaylistItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDeletePlaylist: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MizuThemeObj.colors
    var showDeleteDialog by remember { mutableStateOf(false) }

    val totalDuration = remember(items) {
        items.sumOf { it.duration }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.paper)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(colors.surface)
                            .border(NeuTokens.BorderWidth, colors.border, RoundedCornerShape(NeuTokens.ButtonRadius))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = Lime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = playlistName,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${items.size} items • ${FormatUtils.formatDuration(totalDuration)}",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(Coral.copy(alpha = 0.2f))
                            .border(NeuTokens.BorderWidth, Coral, RoundedCornerShape(NeuTokens.ButtonRadius))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = Coral
                        )
                    }
                }
            }
        },
        containerColor = colors.paper,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Actions Row: Play All & Shuffle
            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuButton(
                        text = "PLAY ALL",
                        onClick = { onItemClick(items.first()) },
                        color = Sun,
                        textColor = Ink,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = "SHUFFLE",
                        onClick = {
                            val shuffled = items.shuffled()
                            if (shuffled.isNotEmpty()) {
                                onItemClick(shuffled.first())
                            }
                        },
                        color = Lime,
                        textColor = Ink,
                        icon = Icons.Default.Shuffle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Playlist items list
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Playlist is empty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select videos in the library to add them here.",
                                fontSize = 13.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        NeuCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) },
                            color = colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Index badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Sun)
                                        .border(1.5.dp, Ink, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Ink
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = FormatUtils.cleanMediaTitle(item.title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FormatUtils.formatDuration(item.duration),
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "DELETE PLAYLIST",
                    fontWeight = FontWeight.Black,
                    color = Ink
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"$playlistName\"? The media files will not be deleted.",
                    color = Ink
                )
            },
            confirmButton = {
                NeuButton(
                    text = "DELETE",
                    onClick = {
                        showDeleteDialog = false
                        onDeletePlaylist()
                    },
                    color = Coral,
                    textColor = Ink
                )
            },
            dismissButton = {
                NeuButton(
                    text = "CANCEL",
                    onClick = { showDeleteDialog = false },
                    color = colors.surface,
                    textColor = Ink
                )
            },
            containerColor = colors.paper,
            shape = RoundedCornerShape(NeuTokens.CardRadius),
            modifier = Modifier.border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.CardRadius))
        )
    }
}
