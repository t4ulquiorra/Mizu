package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateStampBadge(
    text: String,
    modifier: Modifier = Modifier,
    rotation: Float = -6f,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
            }
            .border(
                width = 1.5.dp,
                color = CrateTheme.colors.stamp,
                shape = CrateTheme.shapes.stamp,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = CrateTheme.typography.stampBadge,
            color = CrateTheme.colors.stamp,
        )
    }
}
