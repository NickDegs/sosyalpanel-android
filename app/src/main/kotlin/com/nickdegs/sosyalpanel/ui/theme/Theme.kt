package com.nickdegs.sosyalpanel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF7C3AED)        // mor — iOS tint
val BrandBlue = Color(0xFF1D4ED8)
val Gold = Color(0xFFFBBF24)
val Mint = Color(0xFF34D399)
val Danger = Color(0xFFF87171)

private val DarkColors = darkColorScheme(
    primary = Brand,
    secondary = BrandBlue,
    background = Color(0xFF08040F),
    surface = Color(0x14FFFFFF),
    onBackground = Color(0xFFF5F3FF),
    onSurface = Color(0xFFF5F3FF),
)

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = BrandBlue,
    background = Color(0xFFF1EEF9),
    surface = Color(0x40FFFFFF),
    onBackground = Color(0xFF1A0B2E),
    onSurface = Color(0xFF1A0B2E),
)

@Composable
fun SosyalPanelTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
