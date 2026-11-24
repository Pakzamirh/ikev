package com.sepehr.ikev2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF090D16)
val SurfaceCard = Color(0xFF121826)
val SurfaceElevated = Color(0xFF1A2234)
val BorderHairline = Color(0xFF242E42)
val EmeraldShield = Color(0xFF10B981)
val CobaltAccent = Color(0xFF3B82F6)
val CrimsonAlert = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val SepehrDarkColors = darkColorScheme(
    primary = CobaltAccent,
    secondary = EmeraldShield,
    background = ObsidianBg,
    surface = SurfaceCard,
    surfaceVariant = SurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CrimsonAlert
)

@Composable
fun SepehrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SepehrDarkColors,
        content = content
    )
}
