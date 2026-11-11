package com.mizu.player.ui

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.mizu.player.data.MediaRepository
import com.mizu.player.engine.AspectRatioMode
import com.mizu.player.engine.AudioTrackInfo
import com.mizu.player.engine.SubtitleTrackInfo
import com.mizu.player.model.VideoItem
import com.mizu.player.service.PlaybackService
import com.mizu.player.util.DiagnosticLogger
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    val isPlaying = PlaybackService.isPlaying
    val positionMs = PlaybackService.currentPositionMs
    val durationMs = PlaybackService.durationMs
    val currentVid = PlaybackService.currentVideo ?: video
    val errorMessage = PlaybackService.errorMessage
    val currentNotice = PlaybackService.currentNotice
    val hasNext = PlaybackService.hasNext
    val hasPrevious = PlaybackService.hasPrevious

    DiagnosticLogger.log(context, "PlayerScreen.render", "video=${video.title}, currentVid=${currentVid.title}, isInPipMode=$isInPipMode, isPlaying=$isPlaying")

    var isControllerVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }

    var showAudioPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showPlayerControlsSheet by remember { mutableStateOf(false) }

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

    // Auto-hide controls after 4 seconds of inactivity while playing
    LaunchedEffect(isControllerVisible, isPlaying, isSeeking) {
        if (isControllerVisible && isPlaying && !isSeeking) {
            delay(4000)
            isControllerVisible = false
        }
    }

    // External Subtitle Picker
    val subtitleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            PlaybackService.addExternalSubtitle(uri)
        }
    }

    val handleClose = {
        PlaybackService.stop(context)
        onBack()
    }

    BackHandler {
        handleClose()
    }

    // Start playback service on initial launch
    LaunchedEffect(video.id, playlist) {
        val cur = PlaybackService.currentVideo
        val isAlreadyPlayingThis = cur != null && (cur.id == video.id || cur.path == video.path) && PlaybackService.engine != null
        if (!isAlreadyPlayingThis) {
            PlaybackService.start(context, playlist, initialIndex)
        }
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

    DisposableEffect(Unit) {
        onDispose {
            PlaybackService.engine?.detachVideoLayout()
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
        // Main video player surface
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // libVLC Video Layout
            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        PlaybackService.engine?.attachVideoLayout(this)
                        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                            val w = right - left
                            val h = bottom - top
                            if (w > 0 && h > 0) {
                                PlaybackService.engine?.notifySurfaceReady(this, w, h)
                            }
                        }
                        post {
                            if (width > 0 && height > 0) {
                                PlaybackService.engine?.notifySurfaceReady(this, width, height)
                            }
                        }
                    }
                },
                update = { layout ->
                    PlaybackService.engine?.attachVideoLayout(layout)
                    if (layout.width > 0 && layout.height > 0) {
                        PlaybackService.engine?.notifySurfaceReady(layout, layout.width, layout.height)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isControllerVisible = !isControllerVisible
                    }
            )

            // Loading indicator when buffering or starting
            val showLoading = durationMs <= 0L && errorMessage == null
            if (showLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF3B82F6),
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Fallback Notice Pill Banner (e.g. "Subtitles couldn't be loaded, retrying...")
            if (currentNotice != null && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xD91E222D))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentNotice,
                        color = Color(0xFFFBBF24),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val showOverlayControls = !isInPipMode && isControllerVisible

            // Top Control Bar: Back button, Title, Track and Settings Buttons
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x77000000))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }

                    // Action buttons: Audio, Subtitle, Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Track Picker Button
                        IconButton(
                            onClick = { showAudioPicker = true },
                            modifier = Modifier.background(Color(0x66000000), shape = CircleShape)
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
                            onClick = { showSubtitlePicker = true },
                            modifier = Modifier.background(Color(0x66000000), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Advanced Player Controls (Speed, Aspect Ratio, Delays, Volume)
                        IconButton(
                            onClick = { showPlayerControlsSheet = true },
                            modifier = Modifier.background(Color(0x66000000), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Controls",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Center Play / Pause & Skip Controls
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = { PlaybackService.previous() },
                        enabled = hasPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (hasPrevious) Color(0x66000000) else Color(0x22000000), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (hasPrevious) Color.White else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (positionMs - 10_000L).coerceAtLeast(0L)
                            PlaybackService.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Main Play / Pause Button
                    IconButton(
                        onClick = {
                            if (isPlaying) PlaybackService.pause() else PlaybackService.play()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xAA3B82F6), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val max = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                            val newPos = (positionMs + 10_000L).coerceAtMost(max)
                            PlaybackService.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Fast Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = { PlaybackService.next() },
                        enabled = hasNext,
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (hasNext) Color(0x66000000) else Color(0x22000000), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (hasNext) Color.White else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Bottom Progress Bar & Time
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x77000000))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val activePos = if (isSeeking) seekPosition else positionMs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(activePos),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatDuration(durationMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = if (durationMs > 0) (activePos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { frac ->
                            isSeeking = true
                            seekPosition = (frac * durationMs).toLong()
                        },
                        onValueChangeFinished = {
                            PlaybackService.seekTo(seekPosition)
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0x44FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Resume Banner: follows controller visibility, non-blocking, auto-hides after ~6s
            if (!isInPipMode && isControllerVisible && showResumeBanner && savedPos > 3000L) {
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
                                PlaybackService.seekTo(0L)
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
                PlaybackService.selectAudioTrack(track.id)
                showAudioPicker = false
            }
        )
    }

    // Subtitle Track Picker Dialog
    if (showSubtitlePicker) {
        SubtitleTrackPickerDialog(
            tracks = PlaybackService.availableSubtitleTracks,
            onDismiss = { showSubtitlePicker = false },
            onSelectTrack = { trackId ->
                PlaybackService.selectSubtitleTrack(trackId)
                showSubtitlePicker = false
            },
            onAddExternalSubtitle = {
                subtitleFilePickerLauncher.launch(
                    arrayOf(
                        "text/*",
                        "application/x-subrip",
                        "application/octet-stream",
                        "*/*"
                    )
                )
                showSubtitlePicker = false
            }
        )
    }

    // Player Controls Bottom Sheet (Speed, Aspect Ratio, Delays, Volume Boost)
    if (showPlayerControlsSheet) {
        PlayerControlsBottomSheet(
            onDismiss = { showPlayerControlsSheet = false },
            onAddExternalSubtitle = {
                subtitleFilePickerLauncher.launch(arrayOf("*/*"))
                showPlayerControlsSheet = false
            }
        )
    }
}

@Composable
fun AudioTrackPickerDialog(
    tracks: List<AudioTrackInfo>,
    onDismiss: () -> Unit,
    onTrackSelect: (AudioTrackInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E222D),
        title = {
            Text(
                text = "Audio Tracks",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
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
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(tracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackSelect(track) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = track.name,
                                color = if (track.isSelected) Color(0xFF3B82F6) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (track.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (track.isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun SubtitleTrackPickerDialog(
    tracks: List<SubtitleTrackInfo>,
    onDismiss: () -> Unit,
    onSelectTrack: (String?) -> Unit,
    onAddExternalSubtitle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E222D),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtitles",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddExternalSubtitle) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtitle file",
                        tint = Color(0xFF3B82F6)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                // Off option
                item {
                    val isOffSelected = tracks.none { it.isSelected && it.id != "-1" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(null) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Off",
                            color = if (isOffSelected) Color(0xFF3B82F6) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isOffSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isOffSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isOffSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                items(tracks.filter { it.id != "-1" }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = track.name,
                            color = if (track.isSelected) Color(0xFF3B82F6) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (track.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (track.isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlsBottomSheet(
    onDismiss: () -> Unit,
    onAddExternalSubtitle: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentSpeed = PlaybackService.currentPlaybackSpeed
    val currentAspect = PlaybackService.currentAspectRatio
    val audioDelay = PlaybackService.currentAudioDelayMs
    val subtitleDelay = PlaybackService.currentSubtitleDelayMs
    val volumePercent = PlaybackService.currentVolumePercent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E222D),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            item {
                Text(
                    text = "Player Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Playback Speed
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playback Speed", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Text(String.format(Locale.US, "%.2fx", currentSpeed), color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speedPresets.forEach { spd ->
                        FilterChip(
                            selected = (currentSpeed == spd),
                            onClick = { PlaybackService.setPlaybackSpeed(spd) },
                            label = { Text("${spd}x", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF13161F),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Aspect Ratio
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aspect Ratio", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AspectRatioMode.entries.forEach { mode ->
                        FilterChip(
                            selected = (currentAspect == mode),
                            onClick = { PlaybackService.setAspectRatio(mode) },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF13161F),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Audio Delay
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Delay", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Text("${audioDelay}ms", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { PlaybackService.setAudioDelay(audioDelay - 50L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("-50ms", fontSize = 12.sp) }

                    Button(
                        onClick = { PlaybackService.setAudioDelay(0L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset", fontSize = 12.sp) }

                    Button(
                        onClick = { PlaybackService.setAudioDelay(audioDelay + 50L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("+50ms", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Subtitle Delay
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subtitle Delay", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Text("${subtitleDelay}ms", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { PlaybackService.setSubtitleDelay(subtitleDelay - 50L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("-50ms", fontSize = 12.sp) }

                    Button(
                        onClick = { PlaybackService.setSubtitleDelay(0L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset", fontSize = 12.sp) }

                    Button(
                        onClick = { PlaybackService.setSubtitleDelay(subtitleDelay + 50L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13161F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("+50ms", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 5. Volume Boost
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volume Boost", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Text("${volumePercent}%", color = if (volumePercent > 100) Color(0xFFF59E0B) else Color.White, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { PlaybackService.setVolumeBoost(it.toInt()) },
                    valueRange = 0f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3B82F6),
                        activeTrackColor = if (volumePercent > 100) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 6. Add External Subtitle
            item {
                Button(
                    onClick = onAddExternalSubtitle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add External Subtitle File...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
