package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Editorial, tactile color tokens extracted directly from "The Crate" design spec.
 */
val CrateBgBase = Color(0xFF0D0C0A)
val CrateBgDeep = Color(0xFF14120D)
val CrateBgSurface = Color(0xFF17150F)
val CrateBgSurfaceKey = Color(0xFF1B1812)

val CrateBorder = Color(0xFF26221A)
val CrateBorderStrong = Color(0xFF2E2819)
val CrateBorderInput = Color(0xFF3A3220)

val CrateInkPrimary = Color(0xFFECE4D2)
val CrateInkSecondary = Color(0xFFB9B09B)
val CrateInkMuted = Color(0xFF8F8774)
val CrateInkDim = Color(0xFF6F6858)
val CrateInkPaper = Color(0xFF1A150C)

val CrateStampRed = Color(0xFFC23A12)

val CratePaper0 = Color(0xFFD9CBA6)
val CratePaper1 = Color(0xFFCDBE96)
val CratePaper2 = Color(0xFFE2D6B8)
val CratePaper3 = Color(0xFFC4B48A)

val CratePaperColors = listOf(
    CratePaper0,
    CratePaper1,
    CratePaper2,
    CratePaper3,
)

fun getCratePaperColor(index: Int): Color {
    val nonNegative = if (index < 0) -index else index
    return CratePaperColors[nonNegative % CratePaperColors.size]
}

enum class CrateAccentOption(val title: String, val accent: Color, val shadow: Color) {
    ORANGE(
        title = "International Orange",
        accent = Color(0xFFFF5A2E),
        shadow = Color(0xFF7A2A12),
    ),
    LIME(
        title = "Acid Lime",
        accent = Color(0xFFC8F23D),
        shadow = Color(0xFF4B660B),
    ),
    SEAFOAM(
        title = "Seafoam",
        accent = Color(0xFF7FE7D0),
        shadow = Color(0xFF1B6E5B),
    ),
    AMBER(
        title = "Amber",
        accent = Color(0xFFFFC247),
        shadow = Color(0xFF7A5212),
    ),
}

@Immutable
data class CrateColors(
    val bgBase: Color = CrateBgBase,
    val bgDeep: Color = CrateBgDeep,
    val bgSurface: Color = CrateBgSurface,
    val bgSurfaceKey: Color = CrateBgSurfaceKey,
    val border: Color = CrateBorder,
    val borderStrong: Color = CrateBorderStrong,
    val borderInput: Color = CrateBorderInput,
    val inkPrimary: Color = CrateInkPrimary,
    val inkSecondary: Color = CrateInkSecondary,
    val inkMuted: Color = CrateInkMuted,
    val inkDim: Color = CrateInkDim,
    val inkPaper: Color = CrateInkPaper,
    val stamp: Color = CrateStampRed,
    val accent: Color = CrateAccentOption.ORANGE.accent,
    val accentShadow: Color = CrateAccentOption.ORANGE.shadow,
    val papers: List<Color> = CratePaperColors,
)

val LocalCrateColors = staticCompositionLocalOf { CrateColors() }
