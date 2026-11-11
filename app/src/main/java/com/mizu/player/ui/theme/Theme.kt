package com.mizu.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class MizuColors(
    val ink: androidx.compose.ui.graphics.Color,
    val paper: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val sun: androidx.compose.ui.graphics.Color,
    val coral: androidx.compose.ui.graphics.Color,
    val aqua: androidx.compose.ui.graphics.Color,
    val lavender: androidx.compose.ui.graphics.Color,
    val lime: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val isDark: Boolean
)

val LightMizuColors = MizuColors(
    ink = Ink,
    paper = Paper,
    surface = Surface,
    sun = Sun,
    coral = Coral,
    aqua = Aqua,
    lavender = Lavender,
    lime = Lime,
    border = Ink,
    isDark = false
)

val DarkMizuColors = MizuColors(
    ink = DarkOnSurface,
    paper = DarkBackground,
    surface = DarkSurface,
    sun = Sun,
    coral = Coral,
    aqua = Aqua,
    lavender = Lavender,
    lime = Lime,
    border = DarkBorder,
    isDark = true
)

val LocalMizuColors = staticCompositionLocalOf { LightMizuColors }

private val LightColorScheme = lightColorScheme(
    primary = Sun,
    onPrimary = Ink,
    secondary = Coral,
    onSecondary = Ink,
    tertiary = Aqua,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    error = ErrorRed,
    onError = Surface
)

private val DarkColorScheme = darkColorScheme(
    primary = Sun,
    onPrimary = Ink,
    secondary = Coral,
    onSecondary = Ink,
    tertiary = Aqua,
    onTertiary = Ink,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = ErrorRed,
    onError = Surface
)

@Composable
fun MizuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val mizuColors = if (darkTheme) DarkMizuColors else LightMizuColors

    CompositionLocalProvider(LocalMizuColors provides mizuColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MizuTypography,
            content = content
        )
    }
}

object MizuThemeObj {
    val colors: MizuColors
        @Composable
        get() = LocalMizuColors.current
}
