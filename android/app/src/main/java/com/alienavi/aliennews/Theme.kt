package com.alienavi.aliennews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF0B1B2A)
val DeepBlue = Color(0xFF14344D)
val Gold = Color(0xFFC8A35B)
val Cream = Color(0xFFF7F1E5)
val Paper = Color(0xFFFFFCF5)
val Sage = Color(0xFF8FA99B)
val Ink = Color(0xFF15222C)
val Mist = Color(0xFFE8ECE9)

private val AlienColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Navy,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    outline = Color(0xFFCBD0CA),
    surfaceVariant = Mist,
)

@Composable
fun AlienNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlienColors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 39.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp),
            headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp),
        ),
        content = content,
    )
}
