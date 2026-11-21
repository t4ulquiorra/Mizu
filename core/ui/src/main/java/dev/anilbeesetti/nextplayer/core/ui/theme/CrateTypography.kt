package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.ui.R

val BricolageGrotesqueFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Bold,
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.ExtraBold,
    ),
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(
        resId = R.font.jetbrains_mono,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.jetbrains_mono,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.jetbrains_mono,
        weight = FontWeight.Bold,
    ),
)

@Immutable
data class CrateTypography(
    val titleDisplay: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    val titleSmall: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    val labelMono: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    ),
    val labelMonoSmall: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.2.sp,
    ),
    val stampBadge: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.3.sp,
    ),
    val monoSmallCaps: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    ),
    val monoMedium: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    val monoBold: TextStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    val sansRegular: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val sansMedium: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    val sansBold: TextStyle = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
)

val LocalCrateTypography = staticCompositionLocalOf { CrateTypography() }
