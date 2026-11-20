package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateBgBase
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 20.dp
    val padding = 4.dp
    val maxThumbOffset = trackWidth - thumbSize - (padding * 2)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxThumbOffset else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "CrateToggleThumbOffset",
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) CrateTheme.colors.accent else CrateTheme.colors.bgSurfaceKey,
        animationSpec = tween(durationMillis = 180),
        label = "CrateToggleTrackColor",
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) CrateTheme.colors.accent else CrateTheme.colors.borderStrong,
        animationSpec = tween(durationMillis = 180),
        label = "CrateToggleBorderColor",
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) CrateBgBase else CrateTheme.colors.inkMuted,
        animationSpec = tween(durationMillis = 180),
        label = "CrateToggleThumbColor",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CrateTheme.shapes.pill)
            .background(trackColor)
            .border(1.dp, borderColor, CrateTheme.shapes.pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(padding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}
