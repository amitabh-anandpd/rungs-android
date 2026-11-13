package com.amitabh.rungs.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF090B17)
private val Violet = Color(0xFF6C4DFF)
private val Teal = Color(0xFF19C2B0)
private val CardShape = RoundedCornerShape(20.dp)

/** The fixed backdrop the glass sits on: deep navy with two soft colour glows. Content scrolls over it. */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Ink).drawBehind {
            drawRect(Brush.radialGradient(listOf(Violet.copy(alpha = 0.50f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.08f), size.width * 0.9f))
            drawRect(Brush.radialGradient(listOf(Teal.copy(alpha = 0.32f), Color.Transparent), Offset(size.width, size.height * 0.75f), size.width * 0.9f))
        },
    ) { content() }
}

/**
 * A pane of glass: translucent fill, brighter at the top, with a thin light edge.
 * ponytail: no live blur of what's behind (needs API 31 or the Haze library); the glows are
 * already soft, so the frosted look holds without it.
 */
@Composable
fun Modifier.glass(shape: Shape = CardShape, accent: Boolean = false): Modifier {
    val tint = if (accent) MaterialTheme.colorScheme.primary else Color.White
    return clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = if (accent) 0.26f else 0.11f), tint.copy(alpha = if (accent) 0.12f else 0.04f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))), shape)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.glass(accent = accent), content = content)
