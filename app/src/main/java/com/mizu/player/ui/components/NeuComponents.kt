package com.mizu.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.MizuThemeObj

// Design tokens
object NeuTokens {
    val BorderWidth = 2.5.dp
    val ButtonShadow = 3.dp
    val CardShadow = 4.dp
    val DialogShadow = 6.dp
    val CardRadius = 10.dp
    val ButtonRadius = 8.dp
    val ChipRadius = 6.dp
}

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MizuThemeObj.colors.sun,
    textColor: Color = Ink,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shadowDp: Dp = NeuTokens.ButtonShadow
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowDp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shadow"
    )
    val colors = MizuThemeObj.colors

    Box(modifier = modifier) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowDp, y = shadowDp)
                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                .background(colors.border)
        )
        // Button face
        Row(
            modifier = Modifier
                .offset(x = shadowDp - shadowOffset, y = shadowDp - shadowOffset)
                .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                .background(if (enabled) color else color.copy(alpha = 0.5f))
                .border(
                    BorderStroke(NeuTokens.BorderWidth, colors.border),
                    RoundedCornerShape(NeuTokens.ButtonRadius)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MizuThemeObj.colors.surface,
    content: @Composable () -> Unit
) {
    val colors = MizuThemeObj.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else NeuTokens.CardShadow,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shadow"
    )

    Box(modifier = modifier) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = NeuTokens.CardShadow, y = NeuTokens.CardShadow)
                .clip(RoundedCornerShape(NeuTokens.CardRadius))
                .background(colors.border)
        )
        // Card face
        Box(
            modifier = Modifier
                .offset(
                    x = NeuTokens.CardShadow - shadowOffset,
                    y = NeuTokens.CardShadow - shadowOffset
                )
                .clip(RoundedCornerShape(NeuTokens.CardRadius))
                .background(color)
                .border(
                    BorderStroke(NeuTokens.BorderWidth, colors.border),
                    RoundedCornerShape(NeuTokens.CardRadius)
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

@Composable
fun NeuChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MizuThemeObj.colors.sun
) {
    val colors = MizuThemeObj.colors
    val bgColor = if (selected) selectedColor else colors.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NeuTokens.ChipRadius))
            .background(bgColor)
            .border(
                BorderStroke(NeuTokens.BorderWidth, colors.border),
                RoundedCornerShape(NeuTokens.ChipRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else colors.ink
        )
    }
}

@Composable
fun NeuStickerBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MizuThemeObj.colors.coral
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .border(BorderStroke(1.5.dp, Ink), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Ink,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
        )
    }
}

@Composable
fun NeuTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = MizuThemeObj.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            navigationIcon()
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
        if (actions != null) {
            actions()
        }
    }
}
