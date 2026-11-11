package com.mizu.player.ui.screens.player

import android.app.Activity
import android.net.Uri
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.SubtitleView
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
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    mediaUri: Uri,
    isVideo: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = MizuThemeObj.colors

    val playbackState by viewModel.playbackState.collectAsState()
    val currentEngine by viewModel.currentEngine.collectAsState()
    val seekSec by viewModel.seekDurationSeconds.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addSidecarSubtitle(uri)
        }
    }

    LaunchedEffect(mediaUri) {
        val rawTitle = mediaUri.lastPathSegment?.substringAfterLast('/') ?: "Media"
        val cleanTitle = FormatUtils.cleanMediaTitle(rawTitle)
        viewModel.initialize(mediaUri, isVideo, cleanTitle)
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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isControlsLocked) {
                detectTapGestures(
                    onTap = {
                        if (!isControlsLocked) {
                            controlsVisible = !controlsVisible
                        } else {
                            // If locked, tap shows unlock button briefly
                            controlsVisible = true
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isControlsLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.4f) {
                                viewModel.seekBackward()
                            } else if (offset.x > screenWidth * 0.6f) {
                                viewModel.seekForward()
                            } else {
                                viewModel.togglePlayPause()
                            }
                        }
                    }
                )
            }
    ) {
        // Video surface and subtitle rendering layer
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        viewModel.attachSurface(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AndroidView(
                factory = { ctx ->
                    SubtitleView(ctx).apply {
                        viewModel.attachSubtitleView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Audio visualizer / cover art display
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(NeuTokens.CardRadius))
                            .background(Sun)
                            .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.CardRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = playbackState.title.ifEmpty { "Playing Audio" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Buffering indicator
        if (playbackState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(NeuTokens.CardRadius))
                        .background(colors.surface)
                        .border(NeuTokens.BorderWidth, colors.border, RoundedCornerShape(NeuTokens.CardRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Sun,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Overlay controls
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
                // Top Bar with Inset Padding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(statusBarPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(colors.surface)
                            .border(NeuTokens.BorderWidth, colors.border, RoundedCornerShape(NeuTokens.ButtonRadius))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.ink
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = FormatUtils.cleanMediaTitle(playbackState.title.ifEmpty { "Playing Media" }),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        val folderSub = remember(mediaUri) {
                            val p = mediaUri.path ?: ""
                            val parent = p.substringBeforeLast('/', "").substringAfterLast('/')
                            if (parent.isNotEmpty() && parent != "0" && parent != "emulated") parent else if (isVideo) "Video Playback" else "Audio Playback"
                        }
                        Text(
                            text = folderSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Engine Badge & Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(if (currentEngine == PlayerEngine.EXOPLAYER) Aqua else Lime)
                            .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                            .clickable { viewModel.toggleOtherEngine() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentEngine == PlayerEngine.EXOPLAYER) "EXO" else "VLC",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Ink
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Engine",
                                tint = Ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Lock button
                    IconButton(
                        onClick = { isControlsLocked = !isControlsLocked },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(if (isControlsLocked) Coral else colors.surface)
                            .border(NeuTokens.BorderWidth, colors.border, RoundedCornerShape(NeuTokens.ButtonRadius))
                    ) {
                        Icon(
                            imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (isControlsLocked) Ink else colors.ink
                        )
                    }
                }

                // Center playback controls (hidden when locked)
                if (!isControlsLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind (-seekSec)
                        NeuButton(
                            text = "-${seekSec}s",
                            onClick = { viewModel.seekBackward() },
                            color = colors.surface,
                            textColor = colors.ink,
                            icon = Icons.Default.Replay
                        )

                        // Big Play / Pause
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Sun)
                                .border(3.dp, Ink, CircleShape)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Ink,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Fast forward (+seekSec)
                        NeuButton(
                            text = "+${seekSec}s",
                            onClick = { viewModel.seekForward() },
                            color = colors.surface,
                            textColor = colors.ink,
                            icon = Icons.Default.PlayArrow
                        )
                    }
                }

                // Bottom Controls Bar with Inset Padding (hidden when locked)
                if (!isControlsLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(navBarPadding)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Floating seek time bubble when scrubbing
                        if (isUserSeeking && playbackState.duration > 0) {
                            val targetMs = (sliderPosition * playbackState.duration).toLong()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                                    .background(Sun)
                                    .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = FormatUtils.formatDuration(targetMs),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Ink
                                )
                            }
                        }

                        // Time & Scrubber Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FormatUtils.formatDuration(playbackState.currentPosition),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(Modifier.width(12.dp))

                            val progress = if (playbackState.duration > 0) {
                                (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Slider(
                                value = if (isUserSeeking) sliderPosition else progress,
                                onValueChange = {
                                    isUserSeeking = true
                                    sliderPosition = it
                                },
                                onValueChangeFinished = {
                                    isUserSeeking = false
                                    val targetMs = (sliderPosition * playbackState.duration).toLong()
                                    viewModel.seekTo(targetMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Sun,
                                    activeTrackColor = Sun,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = FormatUtils.formatDuration(playbackState.duration),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Secondary action bar
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
                                textColor = colors.ink,
                                icon = Icons.Default.Speed
                            )

                            // Subtitles
                            NeuButton(
                                text = "Subs (${playbackState.subtitleTracks.size})",
                                onClick = { showSubtitleDialog = true },
                                color = colors.surface,
                                textColor = colors.ink,
                                icon = Icons.Default.ClosedCaption
                            )

                            // Audio Tracks
                            NeuButton(
                                text = "Audio (${playbackState.audioTracks.size})",
                                onClick = { showAudioTrackDialog = true },
                                color = colors.surface,
                                textColor = colors.ink,
                                icon = Icons.Default.Audiotrack
                            )
                        }
                    }
                }
            }
        }

        // Neubrutalist Playback Speed Panel (0.1x to 8.0x)
        if (showSpeedDialog) {
            var typedSpeed by remember(playbackState.playbackSpeed) {
                mutableStateOf(String.format(Locale.US, "%.2f", playbackState.playbackSpeed))
            }

            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = colors.paper,
                title = { Text("PLAYBACK SPEED", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = String.format(Locale.US, "Current Speed: %.2fx", playbackState.playbackSpeed),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )

                        Slider(
                            value = playbackState.playbackSpeed,
                            onValueChange = {
                                val s = (Math.round(it * 20) / 20f).coerceIn(0.1f, 8.0f)
                                viewModel.setSpeed(s)
                                typedSpeed = String.format(Locale.US, "%.2f", s)
                            },
                            valueRange = 0.1f..8.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Sun,
                                activeTrackColor = Sun,
                                inactiveTrackColor = colors.border.copy(alpha = 0.3f)
                            )
                        )

                        // Typed speed input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = typedSpeed,
                                onValueChange = {
                                    typedSpeed = it
                                    val parsed = it.toFloatOrNull()
                                    if (parsed != null && parsed in 0.1f..8.0f) {
                                        viewModel.setSpeed(parsed)
                                    }
                                },
                                label = { Text("Custom Speed (0.1 - 8.0)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Common preset chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                                NeuChip(
                                    text = "${speed}x",
                                    selected = playbackState.playbackSpeed == speed,
                                    onClick = {
                                        viewModel.setSpeed(speed)
                                        typedSpeed = String.format(Locale.US, "%.2f", speed)
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeuButton(text = "RESET (1.0x)", onClick = { viewModel.setSpeed(1.0f) }, color = Coral)
                        NeuButton(text = "DONE", onClick = { showSpeedDialog = false })
                    }
                }
            )
        }

        // Subtitle Dialog with "Load file..." option
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = colors.paper,
                title = { Text("SUBTITLE TRACKS", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeuChip(
                            text = "Disable Subtitles (Off)",
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

                        Spacer(Modifier.height(8.dp))

                        // Load subtitle from file
                        NeuButton(
                            text = "Load subtitle file...",
                            onClick = {
                                subtitlePickerLauncher.launch(
                                    arrayOf(
                                        "application/x-subrip",
                                        "text/vtt",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                                showSubtitleDialog = false
                            },
                            color = Sun,
                            icon = Icons.Default.FileOpen
                        )
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
                                text = track.label,
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
