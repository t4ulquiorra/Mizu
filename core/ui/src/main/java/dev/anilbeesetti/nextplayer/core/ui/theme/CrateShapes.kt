package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class CrateShapes(
    val keycap: RoundedCornerShape = RoundedCornerShape(14.dp),
    val card: RoundedCornerShape = RoundedCornerShape(18.dp),
    val folder: RoundedCornerShape = RoundedCornerShape(20.dp),
    val dock: RoundedCornerShape = RoundedCornerShape(22.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
    val stamp: RoundedCornerShape = RoundedCornerShape(6.dp),
)

val LocalCrateShapes = staticCompositionLocalOf { CrateShapes() }
