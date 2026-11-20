package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateBgBase
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun NextSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: ImageVector = NextIcons.Check,
) {
    val thumbContent: (@Composable () -> Unit)? = if (checked) {
        {
            Icon(
                imageVector = checkedIcon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = thumbContent,
        colors = SwitchDefaults.colors(
            checkedThumbColor = CrateBgBase,
            checkedTrackColor = CrateTheme.colors.accent,
            checkedBorderColor = CrateTheme.colors.accent,
            checkedIconColor = CrateTheme.colors.accent,
            uncheckedThumbColor = CrateTheme.colors.inkMuted,
            uncheckedTrackColor = CrateTheme.colors.bgSurfaceKey,
            uncheckedBorderColor = CrateTheme.colors.borderStrong,
        ),
    )
}
