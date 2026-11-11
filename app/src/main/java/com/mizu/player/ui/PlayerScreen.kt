package com.mizu.player.ui

import android.app.Activity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.VideoItem
import com.mizu.player.service.MizuAudioTrack
import com.mizu.player.service.MizuSubtitleTrack
import com.mizu.player.service.PlaybackService
import com.mizu.player.util.DiagnosticLogger
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    playlist: List<VideoItem> = listOf(video),
    initialIndex: Int = 0,
    onBack: () -> Unit,
    isInPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val activePlayer = PlaybackService.player
    val isPlaying = PlaybackService.isPlaying
    val errorMessage = PlaybackService.errorMessage
    val currentVid = PlaybackService.currentVideo ?: video

    DiagnosticLogger.log(context, "PlayerScreen.render", "video=${video.title}, currentVid=${currentVid.title}, isInPipMode=$isInPipMode, isPlaying=$isPlaying")

    DisposableEffect(video.id) {
        DiagnosticLogger.log(context, "PlayerScreen.mounted", "video=${video.title}")
        onDispose {
            DiagnosticLogger.log(context, "PlayerScreen.disposed", "video=${video.title}")
        }
    }

    var showAudioPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }

    // Resume banner state
    val savedPos = remember(currentVid.id, currentVid.path) {
        MediaRepository.getSavedPosition(currentVid)
    }
    var showResumeBanner by remember(currentVid.id, currentVid.path) {
        mutableStateOf(savedPos > 3000L)
    }

    LaunchedEffect(currentVid.id, currentVid.path) {
        if (savedPos > 3000L) {
            delay(6000)
            showResumeBanner = false
        }
    }

    // Handle closing the player and stopping the playback service
    val handleClose = {
        PlaybackService.stop(context)
        onBack()
    }

    BackHandler {
        handleClose()
    }

    // Start playback service with queue on initial display
    LaunchedEffect(video.id, playlist) {
        PlaybackService.start(context, playlist, initialIndex)
    }

    // Keep screen on while playing, release immediately when paused or closed
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
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = handleClose,
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
                        player = activePlayer
                        useController = !isInPipMode
                        setShowNextButton(true)
                        setShowPreviousButton(true)
                        setShowSubtitleButton(false)
                        keepScreenOn = isPlaying
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != activePlayer) {
                        view.player = activePlayer
                    }
                    view.useController = !isInPipMode
                    view.setShowSubtitleButton(false)
                    view.keepScreenOn = isPlaying
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Control Bar: visible in full screen, hidden during Picture-in-Picture
            if (!isInPipMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button + title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = handleClose,
                            modifier = Modifier
                                .background(Color(0x66000000), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Library",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currentVid.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }

                    // Audio & Subtitle action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Track Picker Button
                        IconButton(
                            onClick = {
                                PlaybackService.refreshTracks()
                                showAudioPicker = true
                            },
                            modifier = Modifier
                                .background(Color(0x66000000), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Subtitle Track Picker Button
                        IconButton(
                            onClick = {
                                PlaybackService.refreshTracks()
                                showSubtitlePicker = true
                            },
                            modifier = Modifier
                                .background(Color(0x66000000), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Resume Banner: non-blocking, auto-hides after ~6s, hidden in PiP or starting at 0
            if (!isInPipMode && showResumeBanner && savedPos > 3000L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE1E222D))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Resumed from ${formatDuration(savedPos)}",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Button(
                            onClick = {
                                activePlayer?.seekTo(0L)
                                MediaRepository.savePlaybackPosition(context, currentVid, 0L)
                                showResumeBanner = false
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Start over",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { showResumeBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Audio Track Picker Dialog
    if (showAudioPicker) {
        AudioTrackPickerDialog(
            tracks = PlaybackService.availableAudioTracks,
            onDismiss = { showAudioPicker = false },
            onTrackSelect = { track ->
                PlaybackService.selectAudioTrack(context, track)
                showAudioPicker = false
            }
        )
    }

    // Subtitle Track Picker Dialog
    if (showSubtitlePicker) {
        SubtitleTrackPickerDialog(
            tracks = PlaybackService.availableSubtitleTracks,
            onDismiss = { showSubtitlePicker = false },
            onSelectTrack = { track ->
                PlaybackService.selectSubtitleTrack(context, track)
                showSubtitlePicker = false
            },
            onDisableSubtitles = {
                PlaybackService.disableSubtitles(context)
                showSubtitlePicker = false
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, min, sec)
    } else {
        String.format(Locale.US, "%d:%02d", min, sec)
    }
}

@Composable
fun AudioTrackPickerDialog(
    tracks: List<MizuAudioTrack>,
    onDismiss: () -> Unit,
    onTrackSelect: (MizuAudioTrack) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Audio Tracks",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            if (tracks.isEmpty()) {
                Text(
                    text = "No audio tracks detected",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(tracks) { _, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (track.isSelected) Color(0xFF1E293B) else Color.Transparent
                                )
                                .clickable { onTrackSelect(track) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (track.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (track.isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = track.name,
                                fontSize = 14.5.sp,
                                fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (track.isSelected) Color.White else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E222D))
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF13161F)
    )
}

@Composable
fun SubtitleTrackPickerDialog(
    tracks: List<MizuSubtitleTrack>,
    onDismiss: () -> Unit,
    onSelectTrack: (MizuSubtitleTrack) -> Unit,
    onDisableSubtitles: () -> Unit
) {
    val isAnySelected = tracks.any { it.isSelected }
    val isOff = !isAnySelected

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Subtitles",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // "Off" option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isOff) Color(0xFF1E293B) else Color.Transparent
                            )
                            .clickable { onDisableSubtitles() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOff) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isOff) Color(0xFF3B82F6) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Off",
                            fontSize = 14.5.sp,
                            fontWeight = if (isOff) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isOff) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }

                // Subtitle tracks
                itemsIndexed(tracks) { _, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (track.isSelected) Color(0xFF1E293B) else Color.Transparent
                            )
                            .clickable { onSelectTrack(track) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (track.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (track.isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = track.name,
                            fontSize = 14.5.sp,
                            fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (track.isSelected) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E222D))
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF13161F)
    )
}
