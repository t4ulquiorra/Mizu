package dev.anilbeesetti.nextplayer.feature.videopicker.composables.crate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateButtonVariant
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateCircleButton
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateMonoLabel
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateStampBadge
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateTactileButton
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateBgBase
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.getCratePaperColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CrateFolderStack(
    folders: List<Folder>,
    recentlyPlayedFolder: Folder?,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    scatterStrength: Float = 1.0f,
) {
    if (folders.isEmpty()) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val scrollPos = remember { Animatable(0f) }
    var fannedIndex by remember { mutableIntStateOf(-1) }
    var lastTapTime by remember { mutableStateOf(0L) }

    // Haptic tick on folder index transition
    LaunchedEffect(scrollPos) {
        snapshotFlow { scrollPos.value.roundToInt().coerceIn(0, folders.lastIndex) }
            .distinctUntilChanged()
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CrateTheme.colors.bgBase)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        val folderPx = with(density) { 70.dp.toPx() }
                        val newPos = (scrollPos.value - delta / folderPx).coerceIn(
                            -0.5f,
                            (folders.lastIndex + 0.5f).coerceAtLeast(0f),
                        )
                        scrollPos.snapTo(newPos)
                    }
                },
                onDragStopped = {
                    coroutineScope.launch {
                        val target = scrollPos.value.roundToInt().coerceIn(0, folders.lastIndex)
                        scrollPos.animateTo(
                            targetValue = target.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val stageWidth = maxWidth
        val isPhone = stageWidth < 600.dp
        val folderWidth = if (isPhone) stageWidth * 0.90f else 820.dp
        val folderHeight = if (isPhone) 360.dp else 410.dp

        val currentPos = scrollPos.value
        val currentIndex = currentPos.roundToInt().coerceIn(0, folders.lastIndex)

        // Render visible sliding window [currentIndex - 1 .. currentIndex + 4]
        val minVisible = (currentIndex - 1).coerceAtLeast(0)
        val maxVisible = (currentIndex + 4).coerceAtMost(folders.lastIndex)

        // Stack container
        Box(
            modifier = Modifier
                .size(width = folderWidth, height = folderHeight),
            contentAlignment = Alignment.Center,
        ) {
            for (i in maxVisible downTo minVisible) {
                val folder = folders[i]
                val d = i - currentPos
                val isRecentlyPlayed = folder.path == recentlyPlayedFolder?.path
                val isFront = (d in -0.5f..0.5f)
                val isFanned = (fannedIndex == i)

                // 3D Perspective Math
                val translateY: Float
                val scale: Float
                val rotX: Float
                val alpha: Float
                val zIndexVal: Float
                val scatterX: Float
                val scatterZ: Float

                if (d < 0f) {
                    // Fly up, forward and fade out
                    val passed = -d
                    translateY = -passed * with(density) { 320.dp.toPx() }
                    scale = (1f + passed * 0.12f).coerceAtMost(1.3f)
                    rotX = -12f * passed
                    alpha = (1f - passed * 1.2f).coerceIn(0f, 1f)
                    zIndexVal = 200f + passed
                    scatterX = 0f
                    scatterZ = 0f
                } else {
                    // Deep crate stack behind front folder
                    translateY = -d * with(density) { 48.dp.toPx() }
                    scale = (1f - d * 0.045f).coerceAtLeast(0.70f)
                    rotX = (5f * d.coerceAtMost(3.5f))
                    alpha = (1f - (d / 4.0f)).coerceIn(0f, 1f)
                    zIndexVal = (100f - d * 10f).coerceAtLeast(0f)

                    // Organic scatter ease
                    val ease = (d / 2f).coerceIn(0f, 1f)
                    val pseudoX = (((i * 9301 + 49297) % 233280) / 233280f - 0.5f) * 50f
                    val pseudoZ = (((i * 49297 + 9301) % 233280) / 233280f - 0.5f) * 3.5f
                    scatterX = pseudoX * ease * scatterStrength
                    scatterZ = pseudoZ * ease * scatterStrength
                }

                if (alpha > 0.02f) {
                    CrateFolderCard(
                        folder = folder,
                        index = i,
                        isRecentlyPlayed = isRecentlyPlayed,
                        isFront = isFront,
                        isFanned = isFanned,
                        folderWidth = folderWidth,
                        folderHeight = folderHeight,
                        isPhone = isPhone,
                        modifier = Modifier
                            .zIndex(zIndexVal)
                            .graphicsLayer {
                                this.translationX = scatterX * density.density
                                this.translationY = translateY
                                this.scaleX = scale
                                this.scaleY = scale
                                this.rotationX = rotX
                                this.rotationZ = scatterZ
                                this.alpha = alpha
                                this.cameraDistance = 18f * density.density
                            },
                        onTabClick = {
                            coroutineScope.launch {
                                fannedIndex = -1
                                scrollPos.animateTo(
                                    targetValue = i.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                        onBodyClick = {
                            if (isFront) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 350) {
                                    onFolderClick(folder.path)
                                } else {
                                    fannedIndex = if (isFanned) -1 else i
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                lastTapTime = now
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
                        onOpenClick = { onFolderClick(folder.path) },
                    )
                }
            }
        }

        // Stepper Navigation Controls (Top/Bottom Chevrons)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrateCircleButton(
                onClick = {
                    if (currentIndex > 0) {
                        coroutineScope.launch {
                            scrollPos.animateTo(
                                (currentIndex - 1).toFloat(),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            )
                        }
                    }
                },
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous Folder",
                enabled = currentIndex > 0,
            )
            CrateCircleButton(
                onClick = {
                    if (currentIndex < folders.lastIndex) {
                        coroutineScope.launch {
                            scrollPos.animateTo(
                                (currentIndex + 1).toFloat(),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            )
                        }
                    }
                },
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Next Folder",
                enabled = currentIndex < folders.lastIndex,
            )
        }

        // Counter Indicator at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(CrateTheme.shapes.pill)
                .background(CrateTheme.colors.bgSurfaceKey)
                .border(1.dp, CrateTheme.colors.borderStrong, CrateTheme.shapes.pill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${currentIndex + 1} / ${folders.size} FOLDERS",
                style = CrateTheme.typography.labelMono,
                color = CrateTheme.colors.inkPrimary,
            )
        }
    }
}

@Composable
private fun CrateFolderCard(
    folder: Folder,
    index: Int,
    isRecentlyPlayed: Boolean,
    isFront: Boolean,
    isFanned: Boolean,
    folderWidth: androidx.compose.ui.unit.Dp,
    folderHeight: androidx.compose.ui.unit.Dp,
    isPhone: Boolean,
    modifier: Modifier = Modifier,
    onTabClick: () -> Unit,
    onBodyClick: () -> Unit,
    onOpenClick: () -> Unit,
) {
    val paperColor = getCratePaperColor(index)
    val tabColor = if (isRecentlyPlayed) CrateTheme.colors.accent else paperColor
    val slot = index % 4
    val tabWidth = if (isPhone) 110.dp else 165.dp
    val tabOffsetFraction = slot * 0.23f

    Box(
        modifier = modifier
            .size(width = folderWidth, height = folderHeight),
    ) {
        // Staggered Top Tab
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = folderWidth * tabOffsetFraction + 16.dp,
                    y = (-36).dp,
                )
                .size(width = tabWidth, height = 38.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(tabColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab,
                    onClick = onTabClick,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = String.format("%02d", index + 1),
                    style = CrateTheme.typography.labelMonoSmall,
                    color = CrateTheme.colors.inkPaper.copy(alpha = 0.7f),
                )
                Text(
                    text = folder.name,
                    style = CrateTheme.typography.titleSmall,
                    color = CrateTheme.colors.inkPaper,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        // Folder Main Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(paperColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBodyClick,
                )
                .padding(if (isPhone) 20.dp else 32.dp),
        ) {
            // Header row with stamp and last played badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CrateMonoLabel(
                        text = "FOLDER ${String.format("%02d", index + 1)}",
                        color = CrateTheme.colors.inkPaper.copy(alpha = 0.6f),
                        style = CrateTheme.typography.labelMonoSmall,
                    )
                    Text(
                        text = folder.name,
                        style = if (isPhone) CrateTheme.typography.titleLarge else CrateTheme.typography.titleDisplay,
                        color = CrateTheme.colors.inkPaper,
                        maxLines = 2,
                    )
                    Text(
                        text = "${folder.mediaList.size} ${if (folder.mediaList.size == 1) "VIDEO" else "VIDEOS"}",
                        style = CrateTheme.typography.labelMono,
                        color = CrateTheme.colors.inkPaper.copy(alpha = 0.85f),
                    )
                    if (isFront) {
                        Text(
                            text = if (isFanned) "TAP TO COLLAPSE" else "TAP TO FAN OUT",
                            style = CrateTheme.typography.labelMonoSmall,
                            color = CrateTheme.colors.inkPaper.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (folder.mediaList.isNotEmpty()) {
                        CrateStampBadge(text = "${folder.mediaList.size} NEW")
                    }
                    if (isRecentlyPlayed) {
                        Box(
                            modifier = Modifier
                                .clip(CrateTheme.shapes.pill)
                                .background(CrateTheme.colors.accent)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "LAST PLAYED",
                                style = CrateTheme.typography.labelMonoSmall,
                                color = CrateBgBase,
                            )
                        }
                    }
                }
            }

            // Spine Bars Fan-Out Visualization
            val spineCount = folder.mediaList.size.coerceAtMost(26)
            if (spineCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (isPhone) 8.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (k in 0 until spineCount) {
                        val baseHeight = (36 + (((index + 3) * 31 + k * 17) % 5) * 16).dp
                        val fanY = -(30 + (k % 4) * 12).toFloat()
                        val fanR = (k - spineCount / 2f) * 1.6f

                        val animatedFanY by animateFloatAsState(
                            targetValue = if (isFanned) fanY else 0f,
                            animationSpec = tween(
                                durationMillis = 280,
                                delayMillis = (k * 12).coerceAtMost(300),
                                easing = FastOutSlowInEasing,
                            ),
                            label = "spineFanY_$k",
                        )
                        val animatedFanR by animateFloatAsState(
                            targetValue = if (isFanned) fanR else 0f,
                            animationSpec = tween(
                                durationMillis = 280,
                                delayMillis = (k * 12).coerceAtMost(300),
                                easing = FastOutSlowInEasing,
                            ),
                            label = "spineFanR_$k",
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = animatedFanY
                                    rotationZ = animatedFanR
                                }
                                .size(width = 6.dp, height = baseHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (k < 3) CrateTheme.colors.accent else CrateTheme.colors.borderInput,
                                ),
                        )
                    }

                    if (folder.mediaList.size > 26) {
                        Text(
                            text = "+${folder.mediaList.size - 26}",
                            style = CrateTheme.typography.labelMonoSmall,
                            color = CrateTheme.colors.inkPaper.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }

            // "Open folder →" Action Button
            if (isFront) {
                CrateTactileButton(
                    onClick = onOpenClick,
                    variant = CrateButtonVariant.ACCENT,
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    Text(
                        text = "Open folder →",
                        style = CrateTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
