package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateBgBase
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

enum class CrateButtonVariant {
    ACCENT,
    SECONDARY,
    PAPER,
}

@Composable
fun CrateTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: CrateButtonVariant = CrateButtonVariant.ACCENT,
    shape: Shape = CrateTheme.shapes.keycap,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val maxShadowHeight = 6.dp
    val minShadowHeight = 1.dp
    val pressOffsetY = if (isPressed && enabled) 4.dp else 0.dp
    val shadowHeight = if (isPressed && enabled) minShadowHeight else maxShadowHeight

    val animatedOffsetY by animateDpAsState(
        targetValue = pressOffsetY,
        animationSpec = tween(durationMillis = 80),
        label = "CrateBtnOffsetY",
    )
    val animatedShadowHeight by animateDpAsState(
        targetValue = shadowHeight,
        animationSpec = tween(durationMillis = 80),
        label = "CrateBtnShadow",
    )

    val (bgColor, contentColor, shadowColor, borderColor) = when (variant) {
        CrateButtonVariant.ACCENT -> Quadruple(
            CrateTheme.colors.accent,
            CrateBgBase,
            CrateTheme.colors.accentShadow,
            Color.Transparent,
        )
        CrateButtonVariant.SECONDARY -> Quadruple(
            CrateTheme.colors.bgSurfaceKey,
            CrateTheme.colors.inkPrimary,
            CrateTheme.colors.borderStrong,
            CrateTheme.colors.border,
        )
        CrateButtonVariant.PAPER -> Quadruple(
            CrateTheme.colors.papers[0],
            CrateTheme.colors.inkPaper,
            CrateTheme.colors.borderStrong,
            Color.Transparent,
        )
    }

    Box(
        modifier = modifier
            .drawBehind {
                val cornerPx = 14.dp.toPx()
                val shadowPx = animatedShadowHeight.toPx()
                // Draw hard tactile bottom shadow
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(0f, shadowPx),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }
            .offset(y = animatedOffsetY)
            .clip(shape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, shape)
                } else Modifier,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
