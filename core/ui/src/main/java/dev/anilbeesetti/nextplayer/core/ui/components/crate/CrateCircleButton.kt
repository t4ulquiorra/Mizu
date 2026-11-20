package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
    backgroundColor: Color = CrateTheme.colors.bgSurface,
    borderColor: Color = CrateTheme.colors.borderStrong,
    iconColor: Color = CrateTheme.colors.inkPrimary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffsetY = if (isPressed && enabled) 2.dp else 0.dp
    val shadowOffset = if (isPressed && enabled) 1.dp else 3.dp

    val animatedOffsetY by animateDpAsState(
        targetValue = pressOffsetY,
        animationSpec = tween(durationMillis = 80),
        label = "CrateCircleBtnOffset",
    )
    val animatedShadowOffset by animateDpAsState(
        targetValue = shadowOffset,
        animationSpec = tween(durationMillis = 80),
        label = "CrateCircleBtnShadow",
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val shadowPx = animatedShadowOffset.toPx()
                val radius = size.toPx() / 2f
                drawCircle(
                    color = CrateTheme.colors.bgBase,
                    radius = radius,
                    center = Offset(size.width / 2f, (size.height / 2f) + shadowPx),
                )
            }
            .offset(y = animatedOffsetY)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) iconColor else iconColor.copy(alpha = 0.4f),
            modifier = Modifier.size(iconSize),
        )
    }
}
