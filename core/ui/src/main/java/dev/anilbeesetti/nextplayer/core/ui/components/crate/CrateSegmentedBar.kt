package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun <T> CrateSegmentedBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: @Composable (T) -> String = { it.toString() },
) {
    val barShape = RoundedCornerShape(12.dp)
    val itemShape = RoundedCornerShape(9.dp)

    Row(
        modifier = modifier
            .clip(barShape)
            .background(CrateTheme.colors.bgDeep)
            .border(1.dp, CrateTheme.colors.border, barShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val interactionSource = remember { MutableInteractionSource() }

            val itemBgColor by animateColorAsState(
                targetValue = if (isSelected) CrateTheme.colors.bgSurfaceKey else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "CrateSegBg",
            )
            val itemBorderColor by animateColorAsState(
                targetValue = if (isSelected) CrateTheme.colors.borderStrong else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "CrateSegBorder",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) CrateTheme.colors.inkPrimary else CrateTheme.colors.inkMuted,
                animationSpec = tween(durationMillis = 150),
                label = "CrateSegText",
            )

            Box(
                modifier = Modifier
                    .clip(itemShape)
                    .background(itemBgColor)
                    .border(1.dp, itemBorderColor, itemShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onItemSelected(item) },
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelProvider(item).uppercase(),
                    style = CrateTheme.typography.labelMono,
                    color = textColor,
                )
            }
        }
    }
}
