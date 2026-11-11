package com.dairypulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FarmGreen = Color(0xFF1B5E20)
val FarmGreenLight = Color(0xFF2E7D32)
val FarmGreenTint = Color(0xFFE8F5E9)
val LossRed = Color(0xFFC62828)
val Surface = Color(0xFFF5F6F8)

private val colors = lightColorScheme(
    primary = FarmGreen,
    onPrimary = Color.White,
    primaryContainer = FarmGreenTint,
    onPrimaryContainer = FarmGreen,
    secondary = FarmGreenLight,
    secondaryContainer = FarmGreenTint,
    onSecondaryContainer = FarmGreen,
    background = Surface,
    surface = Color.White,
    surfaceContainer = Color.White,
    error = LossRed,
)

@Composable
fun DairyPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
