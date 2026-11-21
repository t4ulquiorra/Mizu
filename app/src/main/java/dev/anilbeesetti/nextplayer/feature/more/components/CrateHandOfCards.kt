package dev.anilbeesetti.nextplayer.feature.more.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateMonoLabel
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateHandOfCards(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = videos.take(4)
    if (cards.isEmpty()) return

    var raisedIndex by remember { mutableIntStateOf(cards.lastIndex) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hand of cards container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentAlignment = Alignment.Center,
        ) {
            val count = cards.size
            cards.forEachIndexed { index, video ->
                val isRaised = (raisedIndex == index)
                val baseRotation = (index - (count - 1) / 2f) * 6f
                val baseOffsetX = (index - (count - 1) / 2f) * 44f

                val animatedOffsetY by animateDpAsState(
                    targetValue = if (isRaised) (-20).dp else 0.dp,
                    animationSpec = tween(durationMillis = 180),
                    label = "handCardOffsetY_$index",
                )
                val animatedScale by animateFloatAsState(
                    targetValue = if (isRaised) 1.06f else 1.0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "handCardScale_$index",
                )
                val animatedRotation by animateFloatAsState(
                    targetValue = if (isRaised) 0f else baseRotation,
                    animationSpec = tween(durationMillis = 180),
                    label = "handCardRot_$index",
                )

                val zIndexVal = if (isRaised) 20f else (index + 1).toFloat()

                Box(
                    modifier = Modifier
                        .offset(
                            x = baseOffsetX.dp,
                            y = animatedOffsetY,
                        )
                        .zIndex(zIndexVal)
                        .graphicsLayer {
                            rotationZ = animatedRotation
                            scaleX = animatedScale
                            scaleY = animatedScale
                            cameraDistance = 16f * density
                        }
                        .size(
                            width = 190.dp,
                            height = 150.dp,
                        )
                        .clip(CrateTheme.shapes.card)
                        .background(CrateTheme.colors.bgSurfaceKey)
                        .border(
                            width = if (isRaised) 2.dp else 1.dp,
                            color = if (isRaised) {
                                CrateTheme.colors.accent
                            } else {
                                CrateTheme.colors.borderStrong
                            },
                            shape = CrateTheme.shapes.card,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                if (isRaised) {
                                    onVideoClick(video)
                                } else {
                                    raisedIndex = index
                                }
                            },
                        ),
                ) {
                    // Video Thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.uriString)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp),
                    )

                    // Duration badge
                    if (video.duration > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(CrateTheme.shapes.stamp)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(
                                    horizontal = 5.dp,
                                    vertical = 2.dp,
                                ),
                        ) {
                            Text(
                                text = Utils.formatDurationMillis(video.duration),
                                style = CrateTheme.typography.labelMonoSmall,
                                color = Color.White,
                            )
                        }
                    }

                    // Card Bottom info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(CrateTheme.colors.bgSurfaceKey)
                            .padding(
                                horizontal = 10.dp,
                                vertical = 6.dp,
                            ),
                    ) {
                        Text(
                            text = video.displayName,
                            style = CrateTheme.typography.titleSmall,
                            color = if (isRaised) {
                                CrateTheme.colors.inkPrimary
                            } else {
                                CrateTheme.colors.inkSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Active Card Details & Play Hint
        if (raisedIndex in cards.indices) {
            val activeVideo = cards[raisedIndex]
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrateMonoLabel(
                    text = "TAP THUMBNAIL TO PLAY",
                    color = CrateTheme.colors.accent,
                    style = CrateTheme.typography.labelMonoSmall,
                )
                Text(
                    text = "·",
                    color = CrateTheme.colors.inkDim,
                )
                CrateMonoLabel(
                    text = activeVideo.displayName,
                    color = CrateTheme.colors.inkMuted,
                    style = CrateTheme.typography.labelMonoSmall,
                )
            }
        }
    }
}
