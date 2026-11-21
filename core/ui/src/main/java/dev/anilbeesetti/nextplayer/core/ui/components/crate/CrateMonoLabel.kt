package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateMonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CrateTheme.colors.inkMuted,
    style: TextStyle = CrateTheme.typography.labelMono,
) {
    Text(
        text = text.uppercase(),
        style = style,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun CrateMonoBreadcrumb(
    section: String,
    count: String,
    modifier: Modifier = Modifier,
    color: Color = CrateTheme.colors.inkMuted,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrateMonoLabel(
            text = section,
            color = color,
        )
        Text(
            text = " · ",
            style = CrateTheme.typography.labelMono,
            color = CrateTheme.colors.inkDim,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        CrateMonoLabel(
            text = count,
            color = color,
        )
    }
}
