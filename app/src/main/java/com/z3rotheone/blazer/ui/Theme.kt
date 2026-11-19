package com.z3rotheone.blazer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Bg = Color(0xFF0D0E12)
    val BgTop = Color(0xFF16181F)
    val Surface = Color(0xFF1B1D24)
    val SurfaceHi = Color(0xFF262932)
    val Outline = Color(0xFF2F333D)
    val Text = Color(0xFFF2F3F5)
    val TextDim = Color(0xFF8E929C)
    val Accent = Color(0xFFFF8A3D)
    val Accent2 = Color(0xFFFFC24D)
    val Power = Color(0xFFFF4F5E)

    val accentGradient = Brush.linearGradient(listOf(Accent2, Accent))
    val backgroundGradient = Brush.verticalGradient(listOf(BgTop, Bg))
}

@Composable
fun BlazerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            onPrimary = Palette.Bg,
            secondary = Palette.Accent2,
            background = Palette.Bg,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.SurfaceHi,
            onSurfaceVariant = Palette.TextDim,
            outline = Palette.Outline,
            error = Palette.Power,
        ),
        content = content,
    )
}
