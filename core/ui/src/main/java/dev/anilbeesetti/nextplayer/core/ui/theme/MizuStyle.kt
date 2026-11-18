package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.model.AppSkin

// Neubrutalism Palette
val MizuSunYellow = Color(0xFFFFD84D)
val MizuAqua = Color(0xFF5CE1E6)
val MizuLavender = Color(0xFFB9A4FF)
val MizuLime = Color(0xFFB8F36B)
val MizuCoral = Color(0xFFFF6B57)
val MizuInk = Color(0xFF111111)
val MizuPaperLight = Color(0xFFFFF8EE)
val MizuPaperDark = Color(0xFF191826)
val MizuSurfaceDark = Color(0xFF242236)
val MizuInkDark = Color(0xFFECE7DF)
val MizuShadowDark = Color(0xFF0C0B12)

@Immutable
data class MizuStyle(
    val skin: AppSkin = AppSkin.NEUBRUTALISM,
    val isDark: Boolean = false,
    val borderWidth: Dp = 2.dp,
    val borderColor: Color = MizuInk,
    val shadowOffset: Dp = 3.dp,
    val shadowColor: Color = MizuInk,
    val cornerRadius: Dp = 10.dp,
    val accentFolder: Color = MizuSunYellow,
    val accentVideo: Color = MizuAqua,
    val accentAudio: Color = MizuLavender,
    val accentPlaylist: Color = MizuLime,
    val accentMore: Color = MizuCoral,
    val cardBackground: Color = Color.White,
) {
    val isNeubrutalism: Boolean get() = skin == AppSkin.NEUBRUTALISM
    val shape: CornerBasedShape get() = RoundedCornerShape(cornerRadius)
}

val LocalMizuStyle = staticCompositionLocalOf { MizuStyle() }

fun getMizuStyle(
    skin: AppSkin,
    isDark: Boolean,
): MizuStyle {
    return if (skin == AppSkin.NEUBRUTALISM) {
        if (isDark) {
            MizuStyle(
                skin = skin,
                isDark = true,
                borderWidth = 2.dp,
                borderColor = MizuInkDark,
                shadowOffset = 3.dp,
                shadowColor = MizuShadowDark,
                cornerRadius = 10.dp,
                accentFolder = MizuSunYellow,
                accentVideo = MizuAqua,
                accentAudio = MizuLavender,
                accentPlaylist = MizuLime,
                accentMore = MizuCoral,
                cardBackground = MizuSurfaceDark,
            )
        } else {
            MizuStyle(
                skin = skin,
                isDark = false,
                borderWidth = 2.dp,
                borderColor = MizuInk,
                shadowOffset = 3.dp,
                shadowColor = MizuInk,
                cornerRadius = 10.dp,
                accentFolder = MizuSunYellow,
                accentVideo = MizuAqua,
                accentAudio = MizuLavender,
                accentPlaylist = MizuLime,
                accentMore = MizuCoral,
                cardBackground = Color.White,
            )
        }
    } else {
        MizuStyle(
            skin = skin,
            isDark = isDark,
            borderWidth = 0.dp,
            borderColor = Color.Transparent,
            shadowOffset = 0.dp,
            shadowColor = Color.Transparent,
            cornerRadius = 16.dp,
            accentFolder = MizuSunYellow,
            accentVideo = MizuAqua,
            accentAudio = MizuLavender,
            accentPlaylist = MizuLime,
            accentMore = MizuCoral,
            cardBackground = Color.Transparent,
        )
    }
}

/**
 * Applies a hard zero-blur offset shadow and solid ink outline when in Neubrutalism style.
 */
fun Modifier.neoShadow(
    offset: Dp = 3.dp,
    shadowColor: Color = MizuInk,
    cornerRadius: Dp = 10.dp,
    enabled: Boolean = true,
): Modifier {
    if (!enabled || offset <= 0.dp) return this
    return this.drawBehind {
        val cornerPx = cornerRadius.toPx()
        val offsetPx = offset.toPx()
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(offsetPx, offsetPx),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
    }
}

/**
 * Tactile clickable modifier with physical button-press down into the drop shadow.
 */
fun Modifier.neoClickable(
    enabled: Boolean = true,
    mizuStyle: MizuStyle,
    onClick: () -> Unit,
): Modifier = composed {
    if (!mizuStyle.isNeubrutalism || !enabled) {
        return@composed this.clickable(
            enabled = enabled,
            onClick = onClick,
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed) mizuStyle.shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "neoPressOffset",
    )

    val currentShadow by animateDpAsState(
        targetValue = if (isPressed) 0.dp else mizuStyle.shadowOffset,
        animationSpec = tween(durationMillis = 80),
        label = "neoPressShadow",
    )

    this
        .neoShadow(
            offset = currentShadow,
            shadowColor = mizuStyle.shadowColor,
            cornerRadius = mizuStyle.cornerRadius,
            enabled = true,
        )
        .offset(x = currentOffset, y = currentOffset)
        .border(
            width = mizuStyle.borderWidth,
            color = mizuStyle.borderColor,
            shape = mizuStyle.shape,
        )
        .background(
            color = mizuStyle.cardBackground,
            shape = mizuStyle.shape,
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
