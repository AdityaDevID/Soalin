package com.soalin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val Bg = Color(0xFF0B0F1A)
    val Surface = Color(0xFF151A2B)
    val SurfaceHi = Color(0xFF1E2540)
    val Primary = Color(0xFF8B7BFF)
    val Accent = Color(0xFF2DD4BF)
    val Good = Color(0xFF34D399)
    val Bad = Color(0xFFF87171)
    val Text = Color(0xFFE8EAF6)
    val Dim = Color(0xFF9AA3C7)
}

private val scheme = darkColorScheme(
    primary = C.Primary,
    onPrimary = Color(0xFF0B0F1A),
    secondary = C.Accent,
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Surface,
    onSurface = C.Text,
    surfaceVariant = C.SurfaceHi,
    onSurfaceVariant = C.Dim,
    error = C.Bad
)

@Composable
fun SoalinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
