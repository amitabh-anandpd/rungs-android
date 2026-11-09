package com.amitabh.rungs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colours from the wireframes. No dynamic colour: the app should look the same on every phone.
private val Light = lightColorScheme(
    primary = Color(0xFF2747C9),
    onPrimary = Color.White,
    background = Color(0xFFF5F4EF),
    onBackground = Color(0xFF1F1E1D),
    surface = Color.White,
    onSurface = Color(0xFF1F1E1D),
    onSurfaceVariant = Color(0xFF6B6A68),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9DB0FF),
    onPrimary = Color(0xFF0B1A5C),
    background = Color(0xFF1F1E1D),
    onBackground = Color(0xFFF5F4EF),
    surface = Color(0xFF2A2927),
    onSurface = Color(0xFFF5F4EF),
    onSurfaceVariant = Color(0xFFB5B3AE),
)

@Composable
fun RungsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography, content = content)
}
