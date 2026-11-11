package com.mizu.player

import androidx.compose.ui.graphics.Color
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.DarkBackground
import com.mizu.player.ui.theme.DarkBorder
import com.mizu.player.ui.theme.DarkSurface
import com.mizu.player.ui.theme.DarkSurfaceRaised
import com.mizu.player.ui.theme.DarkText
import com.mizu.player.ui.theme.DarkTextMuted
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lavender
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.Paper
import com.mizu.player.ui.theme.Sun
import com.mizu.player.ui.theme.Surface
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastTest {

    private fun calculateLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) {
                v / 12.92
            } else {
                ((v + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val brighter = max(l1, l2)
        val darker = min(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testLightThemeContrast() {
        val minRatio = 4.5

        // Ink text against light backgrounds
        val inkOnPaper = contrastRatio(Ink, Paper)
        assertTrue("Ink on Paper ratio ($inkOnPaper) must be >= $minRatio", inkOnPaper >= minRatio)

        val inkOnSurface = contrastRatio(Ink, Surface)
        assertTrue("Ink on Surface ratio ($inkOnSurface) must be >= $minRatio", inkOnSurface >= minRatio)

        val inkOnSun = contrastRatio(Ink, Sun)
        assertTrue("Ink on Sun ratio ($inkOnSun) must be >= $minRatio", inkOnSun >= minRatio)

        val inkOnAqua = contrastRatio(Ink, Aqua)
        assertTrue("Ink on Aqua ratio ($inkOnAqua) must be >= $minRatio", inkOnAqua >= minRatio)

        val inkOnLime = contrastRatio(Ink, Lime)
        assertTrue("Ink on Lime ratio ($inkOnLime) must be >= $minRatio", inkOnLime >= minRatio)

        val inkOnLavender = contrastRatio(Ink, Lavender)
        assertTrue("Ink on Lavender ratio ($inkOnLavender) must be >= $minRatio", inkOnLavender >= minRatio)
    }

    @Test
    fun testDarkThemeContrast() {
        val minRatio = 4.5

        // Cream text against dark backgrounds
        val textOnDarkBg = contrastRatio(DarkText, DarkBackground)
        assertTrue("DarkText on DarkBackground ratio ($textOnDarkBg) must be >= $minRatio", textOnDarkBg >= minRatio)

        val textOnDarkSurface = contrastRatio(DarkText, DarkSurface)
        assertTrue("DarkText on DarkSurface ratio ($textOnDarkSurface) must be >= $minRatio", textOnDarkSurface >= minRatio)

        val textOnDarkSurfaceRaised = contrastRatio(DarkText, DarkSurfaceRaised)
        assertTrue("DarkText on DarkSurfaceRaised ratio ($textOnDarkSurfaceRaised) must be >= $minRatio", textOnDarkSurfaceRaised >= minRatio)

        // Muted text against dark background
        val mutedOnDarkBg = contrastRatio(DarkTextMuted, DarkBackground)
        assertTrue("DarkTextMuted on DarkBackground ratio ($mutedOnDarkBg) must be >= $minRatio", mutedOnDarkBg >= minRatio)
    }
}
