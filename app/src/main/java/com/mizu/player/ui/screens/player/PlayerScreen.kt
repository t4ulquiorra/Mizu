package com.mizu.player.ui.screens.player

import android.net.Uri
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.ui.components.NeuButton
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuStickerBadge
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    mediaUri: Uri,
    isVideo: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val colors = MizuThemeObj.colors
    val playbackState by viewModel.playbackState.collectAsState()
    val currentEngine by viewModel.currentEngine.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(mediaUri) {
        viewModel.initialize(mediaUri, isVideo)
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, playbackState.isPlaying, isControlsLocked) {
        if (controlsVisible && playbackState.isPlaying && !isControlsLocked) {
            delay(4000)
            controlsVisible = false
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 2) {
                            viewModel.seekBy(-10000L)
                        } else {
                            viewModel.seekBy(10000L)
                        }
                    }
                )
            }
    ) {
        // Video Surface
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).also { surfaceView ->
                        viewModel.attachSurface(surfaceView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            DisposableEffect(Unit) {
                onDispose {
                    viewModel.detachSurface()
                }
            }
        } else {
            // Audio mode visualizer card
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sun)
                        .border(3.dp, Ink, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        // Buffering Indicator
        if (playbackState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = Sun
            )
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                                .background(colors.surface)
                                .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Ink
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = playbackState.title.ifEmpty { "Playing Media" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Engine Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (currentEngine == PlayerEngine.EXOPLAYER) Sun else Coral)
                            .border(1.5.dp, Ink, RoundedCornerShape(4.dp))
                            .clickable {
                                viewModel.switchEngine(
                                    if (currentEngine == PlayerEngine.EXOPLAYER) PlayerEngine.VLC else PlayerEngine.EXOPLAYER
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentEngine == PlayerEngine.EXOPLAYER) "EXO" else "VLC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Lock button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(if (isControlsLocked) Coral else colors.surface)
                            .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                            .clickable { isControlsLocked = !isControlsLocked },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isControlsLocked) {
                    // Center Controls: Rewind 10s, Play/Pause, Forward 10s
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                                .background(colors.surface)
                                .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                                .clickable { viewModel.seekBy(-10000L) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind",
                                tint = Ink,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Play/Pause
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                                .background(Sun)
                                .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Ink,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Forward 10
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                                .background(colors.surface)
                                .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                                .clickable { viewModel.seekBy(10000L) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward",
                                tint = Ink,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Bottom Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        // Time & Scrubber
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentPos = if (isUserSeeking) sliderPosition.toLong() else playbackState.currentPosition
                            Text(
                                text = FormatUtils.formatDuration(currentPos),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = FormatUtils.formatDuration(playbackState.duration),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Slider(
                            value = if (isUserSeeking) sliderPosition else playbackState.currentPosition.toFloat(),
                            onValueChange = {
                                isUserSeeking = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(sliderPosition.toLong())
                                isUserSeeking = false
                            },
                            valueRange = 0f..playbackState.duration.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Sun,
                                activeTrackColor = Sun,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        // Action Buttons: Speed, Subtitles, Audio Track
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed
                            NeuButton(
                                text = "${playbackState.playbackSpeed}x",
                                onClick = { showSpeedDialog = true },
                                color = colors.surface,
                                textColor = Ink,
                                icon = Icons.Default.Speed
                            )

                            // Subtitles
                            NeuButton(
                                text = "Subs (${playbackState.subtitleTracks.size})",
                                onClick = { showSubtitleDialog = true },
                                color = colors.surface,
                                textColor = Ink,
                                icon = Icons.Default.ClosedCaption
                            )

                            // Audio Tracks
                            NeuButton(
                                text = "Audio (${playbackState.audioTracks.size})",
                                onClick = { showAudioTrackDialog = true },
                                color = colors.surface,
                                textColor = Ink,
                                icon = Icons.Default.Audiotrack
                            )
                        }
                    }
                }
            }
        }

        // Speed Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = colors.paper,
                title = { Text("PLAYBACK SPEED", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                            NeuChip(
                                text = "${speed}x",
                                selected = playbackState.playbackSpeed == speed,
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    NeuButton(text = "CLOSE", onClick = { showSpeedDialog = false })
                }
            )
        }

        // Subtitle Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = colors.paper,
                title = { Text("SUBTITLE TRACKS", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeuChip(
                            text = "Disable Subtitles",
                            selected = playbackState.selectedSubtitleTrack == -1,
                            onClick = {
                                viewModel.selectSubtitle(-1)
                                showSubtitleDialog = false
                            }
                        )
                        playbackState.subtitleTracks.forEach { track ->
                            NeuChip(
                                text = track.label,
                                selected = playbackState.selectedSubtitleTrack == track.index,
                                onClick = {
                                    viewModel.selectSubtitle(track.index)
                                    showSubtitleDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    NeuButton(text = "CLOSE", onClick = { showSubtitleDialog = false })
                }
            )
        }

        // Audio Track Dialog
        if (showAudioTrackDialog) {
            AlertDialog(
                onDismissRequest = { showAudioTrackDialog = false },
                containerColor = colors.paper,
                title = { Text("AUDIO TRACKS", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playbackState.audioTracks.forEach { track ->
                            NeuChip(
                                text = "${track.label} (${track.channelCount}ch)",
                                selected = playbackState.selectedAudioTrack == track.index,
                                onClick = {
                                    viewModel.selectAudioTrack(track.index)
                                    showAudioTrackDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    NeuButton(text = "CLOSE", onClick = { showAudioTrackDialog = false })
                }
            )
        }
    }
}
