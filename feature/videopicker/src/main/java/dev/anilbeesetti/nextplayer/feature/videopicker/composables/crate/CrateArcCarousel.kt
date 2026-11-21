package dev.anilbeesetti.nextplayer.feature.videopicker.composables.crate

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateButtonVariant
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateCircleButton
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateMonoLabel
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateTactileButton
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateBgBase
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun CrateArcCarousel(
    videos: List<Video>,
    preferences: ApplicationPreferences,
    onVideoClick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val scrollPos = remember { Animatable(0f) }

    // Haptic tick on video index change
    LaunchedEffect(scrollPos) {
        snapshotFlow {
            scrollPos.value
                .roundToInt()
                .coerceAtLeast(0)
                .coerceAtMost(videos.lastIndex)
        }
            .distinctUntilChanged()
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CrateTheme.colors.bgBase),
    ) {
        val stageWidth = maxWidth
        val isPhone = stageWidth < 600.dp
        val cardWidth = if (isPhone) 240.dp else 420.dp
        val cardHeight = cardWidth * (9f / 16f)

        val currentPos = scrollPos.value
        val currentIndex = currentPos.roundToInt().coerceAtLeast(0).coerceAtMost(videos.lastIndex)
        val activeVideo = videos[currentIndex]

        // Dragging gesture horizontally
        val dragModifier = Modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    val stepPx = with(density) { (if (isPhone) 120.dp else 220.dp).toPx() }
                    val maxLimit = (videos.lastIndex + 0.5f).coerceAtLeast(0f)
                    val newPos = (scrollPos.value - delta / stepPx)
                        .coerceAtLeast(-0.5f)
                        .coerceAtMost(maxLimit)
                    scrollPos.snapTo(newPos)
                }
            },
            onDragStopped = {
                coroutineScope.launch {
                    val target = scrollPos.value.roundToInt()
                        .coerceAtLeast(0)
                        .coerceAtMost(videos.lastIndex)
                    scrollPos.animateTo(
                        targetValue = target.toFloat(),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Stage: 3D Arc Cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Sliding window of visible cards [currentIndex - 4 .. currentIndex + 4]
                val minVis = (currentIndex - 4).coerceAtLeast(0)
                val maxVis = (currentIndex + 4).coerceAtMost(videos.lastIndex)

                val radius = if (isPhone) 750f else 1400f

                for (i in minVis..maxVis) {
                    val video = videos[i]
                    val d = i - currentPos
                    val theta = d * 0.26f // radians
                    val xPx =
                        radius * sin(theta) * density.density * (if (isPhone) 0.55f else 0.85f)
                    val zDepth = radius * (1f - cos(theta))
                    val scale =
                        (1f + (1f - abs(d)).coerceAtLeast(0f).coerceAtMost(1f) * 0.28f) *
                            (1f - abs(d) * 0.05f).coerceAtLeast(0.6f)
                    val rotY = (theta * (180f / PI.toFloat()))
                    val alpha = (1f - (abs(d) / 4.2f)).coerceAtLeast(0f).coerceAtMost(1f)
                    val zIndexVal = 100f - abs(d) * 10f
                    val isActive = abs(d) < 0.45f

                    if (alpha > 0.04f) {
                        CrateArcCard(
                            video = video,
                            index = i,
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            isActive = isActive,
                            modifier = Modifier
                                .zIndex(zIndexVal)
                                .graphicsLayer {
                                    this.translationX = xPx
                                    this.translationY = zDepth * 0.12f
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.rotationY = rotY
                                    this.alpha = alpha
                                    this.cameraDistance = 16f * density.density
                                },
                            onClick = {
                                if (isActive) {
                                    onVideoClick(video.uriString.toUri())
                                } else {
                                    coroutineScope.launch {
                                        scrollPos.animateTo(
                                            targetValue = i.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                // Left/Right Stepper Navigation Arrows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrateCircleButton(
                        onClick = {
                            if (currentIndex > 0) {
                                coroutineScope.launch {
                                    scrollPos.animateTo(
                                        (currentIndex - 1).toFloat(),
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        },
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Video",
                        enabled = currentIndex > 0,
                    )
                    CrateCircleButton(
                        onClick = {
                            if (currentIndex < videos.lastIndex) {
                                coroutineScope.launch {
                                    scrollPos.animateTo(
                                        (currentIndex + 1).toFloat(),
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        },
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Video",
                        enabled = currentIndex < videos.lastIndex,
                    )
                }
            }

            // Middle: Active Video Details & Play Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 6.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val titleStyle = if (isPhone) {
                    CrateTheme.typography.titleMedium
                } else {
                    CrateTheme.typography.titleLarge
                }
                Text(
                    text = activeVideo.displayName,
                    style = titleStyle,
                    color = CrateTheme.colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrateMonoLabel(
                        text = "${currentIndex + 1} OF ${videos.size}",
                        color = CrateTheme.colors.accent,
                    )
                    Text(
                        text = "·",
                        color = CrateTheme.colors.inkDim,
                    )
                    CrateMonoLabel(
                        text = Utils.formatDurationMillis(activeVideo.duration),
                    )
                    if (activeVideo.size > 0) {
                        Text(
                            text = "·",
                            color = CrateTheme.colors.inkDim,
                        )
                        CrateMonoLabel(text = Utils.formatFileSize(activeVideo.size))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                CrateTactileButton(
                    onClick = { onVideoClick(activeVideo.uriString.toUri()) },
                    variant = CrateButtonVariant.ACCENT,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAY VIDEO",
                        style = CrateTheme.typography.titleSmall,
                    )
                }
            }

            // Bottom: Precision Ruler Scrubber
            CratePrecisionRuler(
                totalItems = videos.size,
                currentPos = currentPos,
                onSeek = { target ->
                    coroutineScope.launch {
                        val targetPos = target
                            .coerceAtLeast(0f)
                            .coerceAtMost(videos.lastIndex.toFloat())
                        scrollPos.animateTo(
                            targetValue = targetPos,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 4.dp,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CrateArcCard(
    video: Video,
    index: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val borderColor = if (isActive) CrateTheme.colors.accent else CrateTheme.colors.borderStrong

    Column(
        modifier = modifier
            .width(cardWidth)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Main 16:9 Thumbnail Box
        Box(
            modifier = Modifier
                .size(
                    width = cardWidth,
                    height = cardHeight,
                )
                .clip(RoundedCornerShape(16.dp))
                .background(CrateTheme.colors.bgSurfaceKey)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uriString)
                    .crossfade(true)
                    .build(),
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Duration Pill
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CrateTheme.shapes.stamp)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(
                            horizontal = 6.dp,
                            vertical = 3.dp,
                        ),
                ) {
                    Text(
                        text = Utils.formatDurationMillis(video.duration),
                        style = CrateTheme.typography.labelMonoSmall,
                        color = Color.White,
                    )
                }
            }

            // Index Pill Top Start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CrateTheme.shapes.stamp)
                    .background(CrateTheme.colors.bgSurfaceKey.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = CrateTheme.colors.borderStrong,
                        shape = CrateTheme.shapes.stamp,
                    )
                    .padding(
                        horizontal = 6.dp,
                        vertical = 2.dp,
                    ),
            ) {
                val indexStr = if (index + 1 < 10) {
                    "0${index + 1}"
                } else {
                    "${index + 1}"
                }
                Text(
                    text = indexStr,
                    style = CrateTheme.typography.labelMonoSmall,
                    color = CrateTheme.colors.inkPrimary,
                )
            }
        }

        // Mirrored Ground Reflection
        Box(
            modifier = Modifier
                .size(
                    width = cardWidth,
                    height = cardHeight * 0.35f,
                )
                .graphicsLayer {
                    scaleY = -0.85f
                    alpha = 0.35f
                }
                .clip(
                    RoundedCornerShape(
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                ),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uriString)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Gradient Mask fading to black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CrateBgBase.copy(alpha = 0.95f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun CratePrecisionRuler(
    totalItems: Int,
    currentPos: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = CrateTheme.colors.accent
    val borderStrongColor = CrateTheme.colors.borderStrong
    val inkMutedColor = CrateTheme.colors.inkMuted
    val bgSurface = CrateTheme.colors.bgSurfaceKey

    val stepWidthDp = 24.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgSurface)
            .border(
                width = 1.dp,
                color = borderStrongColor,
                shape = RoundedCornerShape(12.dp),
            )
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    val stepPx = with(density) { stepWidthDp.toPx() }
                    val newTarget = currentPos - delta / stepPx
                    onSeek(newTarget)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val stepPx = stepWidthDp.toPx()

            // Draw ruler tick marks
            for (i in 0 until totalItems) {
                val tickX = centerX + (i - currentPos) * stepPx
                if (tickX in -20f..(canvasWidth + 20f)) {
                    val isMajor = (i % 5 == 0)
                    val tickHeight = if (isMajor) 18.dp.toPx() else 9.dp.toPx()
                    val tickColor = if (isMajor) inkMutedColor else borderStrongColor

                    drawLine(
                        color = tickColor,
                        start = Offset(
                            x = tickX,
                            y = canvasHeight - tickHeight - 8f,
                        ),
                        end = Offset(
                            x = tickX,
                            y = canvasHeight - 8f,
                        ),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
            }

            // Draw Center Accent Needle
            drawLine(
                color = accentColor,
                start = Offset(
                    x = centerX,
                    y = 6f,
                ),
                end = Offset(
                    x = centerX,
                    y = canvasHeight - 6f,
                ),
                strokeWidth = 3.dp.toPx(),
            )
            drawCircle(
                color = accentColor,
                radius = 3.5.dp.toPx(),
                center = Offset(
                    x = centerX,
                    y = 6f,
                ),
            )
        }
    }
}
