package com.amitabh.rungs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One dark scheme: glass needs a dark backdrop, and the app should look the same on every phone.
private val Colors = darkColorScheme(
    primary = Color(0xFFA596FF),
    onPrimary = Color(0xFF14103A),
    primaryContainer = Color(0xFF2E2960),
    onPrimaryContainer = Color(0xFFECE9FF),
    secondaryContainer = Color(0xFF3A3470),
    onSecondaryContainer = Color(0xFFECE9FF),
    background = Ink,
    onBackground = Color(0xFFF2F3FA),
    surface = Color(0xFF12152A),
    onSurface = Color(0xFFF2F3FA),
    onSurfaceVariant = Color(0xFFA9ADC6),
    // Dialogs and menus stay solid so their text is readable over anything.
    surfaceContainer = Color(0xFF161A30),
    surfaceContainerHigh = Color(0xFF1A1E36),
    surfaceContainerHighest = Color(0xFF20243E),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x33FFFFFF),
)

@Composable
fun RungsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Typography, content = content)
}
