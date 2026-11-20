package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCrateAccent = staticCompositionLocalOf { CrateAccentOption.ORANGE }

object CrateTheme {
    val colors: CrateColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCrateColors.current

    val typography: CrateTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCrateTypography.current

    val shapes: CrateShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCrateShapes.current

    val accent: CrateAccentOption
        @Composable
        @ReadOnlyComposable
        get() = LocalCrateAccent.current
}

@Composable
fun CrateTheme(
    accentOption: CrateAccentOption = CrateAccentOption.ORANGE,
    content: @Composable () -> Unit,
) {
    val crateColors = remember(accentOption) {
        CrateColors(
            accent = accentOption.accent,
            accentShadow = accentOption.shadow,
        )
    }
    val crateTypography = remember { CrateTypography() }
    val crateShapes = remember { CrateShapes() }

    val materialColorScheme = remember(crateColors) {
        darkColorScheme(
            primary = crateColors.accent,
            onPrimary = CrateBgBase,
            primaryContainer = crateColors.accentShadow,
            onPrimaryContainer = crateColors.inkPrimary,
            secondary = crateColors.inkSecondary,
            onSecondary = CrateBgBase,
            secondaryContainer = crateColors.bgSurfaceKey,
            onSecondaryContainer = crateColors.inkPrimary,
            tertiary = crateColors.papers[0],
            onTertiary = CrateInkPaper,
            background = crateColors.bgBase,
            onBackground = crateColors.inkPrimary,
            surface = crateColors.bgSurface,
            onSurface = crateColors.inkPrimary,
            surfaceVariant = crateColors.bgSurfaceKey,
            onSurfaceVariant = crateColors.inkSecondary,
            outline = crateColors.borderStrong,
            outlineVariant = crateColors.border,
            error = crateColors.stamp,
            onError = Color.White,
        )
    }

    CompositionLocalProvider(
        LocalCrateColors provides crateColors,
        LocalCrateTypography provides crateTypography,
        LocalCrateShapes provides crateShapes,
        LocalCrateAccent provides accentOption,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
