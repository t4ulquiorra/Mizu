package com.mizu.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MizuColors(
    val ink: Color,
    val paper: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val textMuted: Color,
    val sun: Color,
    val coral: Color,
    val aqua: Color,
    val lavender: Color,
    val lime: Color,
    val border: Color,
    val shadowColor: Color,
    val isDark: Boolean
) {
    val text: Color get() = ink
}

val LightMizuColors = MizuColors(
    ink = Ink,
    paper = Paper,
    surface = Surface,
    surfaceRaised = Surface,
    onSurface = Ink,
    textMuted = Color(0xFF666666),
    sun = Sun,
    coral = Coral,
    aqua = Aqua,
    lavender = Lavender,
    lime = Lime,
    border = Ink,
    shadowColor = Ink,
    isDark = false
)

val DarkMizuColors = MizuColors(
    ink = DarkText,
    paper = DarkBackground,
    surface = DarkSurface,
    surfaceRaised = DarkSurfaceRaised,
    onSurface = DarkText,
    textMuted = DarkTextMuted,
    sun = Sun,
    coral = Coral,
    aqua = Aqua,
    lavender = Lavender,
    lime = Lime,
    border = DarkBorder,
    shadowColor = Sun, // Hard offset shadow in Sun accent for dark theme
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
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
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
