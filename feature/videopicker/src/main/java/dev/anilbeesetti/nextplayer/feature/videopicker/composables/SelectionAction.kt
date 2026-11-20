package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
internal fun SelectionAction(
    imageVector: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTv: Boolean = false,
) {
    Column(
        modifier = modifier
            .defaultMinSize(
                minWidth = 68.dp,
                minHeight = 58.dp,
            )
            .tvFocusRing(
                isTv = isTv,
                shape = CrateTheme.shapes.keycap,
            )
            .clip(shape = CrateTheme.shapes.keycap)
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = CrateTheme.colors.inkPrimary,
        )
        Spacer(modifier = Modifier.size(3.dp))
        Text(
            text = title.uppercase(),
            style = CrateTheme.typography.labelMonoSmall,
            color = CrateTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
