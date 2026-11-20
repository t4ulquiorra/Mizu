package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.getCratePaperColor

@Composable
fun CratePaperCard(
    index: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CrateTheme.shapes.folder,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    overrideColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val paperColor = overrideColor ?: getCratePaperColor(index)
    val inkColor = CrateTheme.colors.inkPaper

    val clickableModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(paperColor)
            .then(clickableModifier)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides inkColor) {
            content()
        }
    }
}
