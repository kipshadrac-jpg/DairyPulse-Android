package com.dairypulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dairypulse.app.R

// Palette: cream ground, deep farm green, ochre for feed, slate blue and clay red for figures.
object Dp {
    val Cream = Color(0xFFF6F1E7)
    val CreamDeep = Color(0xFFF3EEE2)
    val Track = Color(0xFFF2ECDF)
    val Card = Color(0xFFFFFCF6)
    val NavBg = Color(0xFFFBF8F1)
    val Border = Color(0xFFE7DFCF)
    val Divider = Color(0xFFEEE7D9)
    val InputBorder = Color(0xFFDDD3C0)
    val Ink = Color(0xFF1F2A22)
    val Heading = Color(0xFF1E3B2A)
    val Muted = Color(0xFF6B6658)
    val Neutral = Color(0xFF4F4B40)
    val NeutralTint = Color(0xFFECE6D8)
    val Green = Color(0xFF22593B)
    val GreenDark = Color(0xFF1E4D33)
    val GreenValue = Color(0xFF1F5C3A)
    val GreenSoft = Color(0xFF8DB77C)
    val GreenTint = Color(0xFFE1EDD8)
    val GreenPill = Color(0xFFDCEBD2)
    val GreenPanel = Color(0xFFE6F0DE)
    val Ochre = Color(0xFF7A5A12)
    val OchreBar = Color(0xFFB9893A)
    val OchreTint = Color(0xFFF3EAD3)
    val Slate = Color(0xFF2F5F82)
    val SlateBar = Color(0xFF7FA8C9)
    val Clay = Color(0xFF8F3A2A)
    val ClayDark = Color(0xFF7D3122)
    val ClayTint = Color(0xFFF8E7E0)
    val ClayBorder = Color(0xFFE9C7BA)

    val heroGradient = Brush.linearGradient(listOf(Color(0xFF2F6E49), Color(0xFF1C4A31)))
    val lossGradient = Brush.linearGradient(listOf(Color(0xFFA34A37), Color(0xFF7A2E20)))
}

// Kept for older call sites.
val FarmGreen = Dp.Green
val LossRed = Dp.Clay

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(500), manrope(600), manrope(700), manrope(800))

@OptIn(ExperimentalTextApi::class)
val Fraunces = FontFamily(
    Font(
        R.font.fraunces, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", 48f),
        ),
    ),
)

/** Serif display style used for titles and big figures. */
fun serif(size: Int, color: Color = Dp.Heading) = TextStyle(
    fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = size.sp,
    letterSpacing = (-0.01).em, color = color, lineHeight = (size * 1.15).sp,
)

private val base = Typography()
private fun TextStyle.m() = copy(fontFamily = Manrope)

private val typography = Typography(
    displayLarge = base.displayLarge.m(), displayMedium = base.displayMedium.m(), displaySmall = base.displaySmall.m(),
    headlineLarge = base.headlineLarge.m(), headlineMedium = base.headlineMedium.m(), headlineSmall = base.headlineSmall.m(),
    titleLarge = base.titleLarge.m(), titleMedium = base.titleMedium.m(), titleSmall = base.titleSmall.m(),
    bodyLarge = base.bodyLarge.m().copy(fontWeight = FontWeight.Medium),
    bodyMedium = base.bodyMedium.m().copy(fontWeight = FontWeight.Medium),
    bodySmall = base.bodySmall.m().copy(fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.m().copy(fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.m(), labelSmall = base.labelSmall.m(),
)

private val colors = lightColorScheme(
    primary = Dp.Green,
    onPrimary = Color.White,
    primaryContainer = Dp.GreenTint,
    onPrimaryContainer = Dp.GreenDark,
    secondary = Dp.Green,
    secondaryContainer = Dp.GreenPill,
    onSecondaryContainer = Dp.GreenDark,
    background = Dp.Cream,
    onBackground = Dp.Ink,
    surface = Dp.Card,
    onSurface = Dp.Ink,
    surfaceVariant = Dp.NeutralTint,
    onSurfaceVariant = Dp.Muted,
    surfaceContainer = Dp.Card,
    surfaceContainerLow = Dp.NavBg,
    surfaceContainerHigh = Dp.NavBg,
    surfaceContainerHighest = Dp.Card,
    outline = Dp.InputBorder,
    outlineVariant = Dp.Border,
    error = Dp.Clay,
)

@Composable
fun DairyPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
