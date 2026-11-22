package com.sharath.statusbartotal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppColors {
    val Indigo = Color(0xFF3F3DBA)
    val Violet = Color(0xFF7B3FE4)
    val IndigoDark = Color(0xFF2C2A8F)
    val VioletDark = Color(0xFF5A2DB0)

    val Upi = Color(0xFF00A896)
    val Cc = Color(0xFFF2545B)
    val CcUpi = Color(0xFFF4A300)
    val CardPalette = listOf(
        Color(0xFF3D7DF6), Color(0xFF9B5DE5), Color(0xFF00B4D8),
        Color(0xFFF15BB5), Color(0xFF6A994E), Color(0xFFE07A5F)
    )
    val Negative = Color(0xFFD32F2F)
    val NegativeDark = Color(0xFFFF6B6B)
    val Positive = Color(0xFF2E7D32)
    val PositiveDark = Color(0xFF66BB6A)
    val Amber = Color(0xFFF4A300)

    /** Colour for a bucket key ("UPI", "CC", "CCUPI", "card:<code>"). */
    fun bucket(key: String, cardCodes: List<String>): Color = when (key) {
        "UPI" -> Upi
        "CC" -> Cc
        "CCUPI" -> CcUpi
        else -> {
            val code = key.removePrefix("card:")
            val idx = cardCodes.sorted().indexOf(code)
            val slot = if (idx >= 0) idx else Math.floorMod(code.hashCode(), CardPalette.size)
            CardPalette[slot % CardPalette.size]
        }
    }
}

@Composable
fun heroBrush(): Brush =
    if (isSystemInDarkTheme())
        Brush.verticalGradient(listOf(AppColors.IndigoDark, AppColors.VioletDark))
    else Brush.verticalGradient(listOf(AppColors.Indigo, AppColors.Violet))

@Composable
fun negativeColor(): Color = if (isSystemInDarkTheme()) AppColors.NegativeDark else AppColors.Negative

@Composable
fun positiveColor(): Color = if (isSystemInDarkTheme()) AppColors.PositiveDark else AppColors.Positive

private val LightColors = lightColorScheme(
    primary = AppColors.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2FF),
    onPrimaryContainer = Color(0xFF14126B),
    secondary = AppColors.Violet,
    background = Color(0xFFF6F5FC),
    surface = Color(0xFFF6F5FC),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEEDF8),
    error = AppColors.Negative
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9A7FF),
    onPrimary = Color(0xFF1B1A6B),
    primaryContainer = Color(0xFF2C2A8F),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFFD0B8FF),
    background = Color(0xFF111118),
    surface = Color(0xFF111118),
    surfaceContainer = Color(0xFF1C1C26),
    surfaceContainerHigh = Color(0xFF262633),
    error = AppColors.NegativeDark
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
