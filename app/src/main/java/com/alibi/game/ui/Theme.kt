package com.alibi.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Detective-noir palette: dark desk, amber lamp light. */
object AlibiColors {
    val Background = Color(0xFF0F1115)
    val Surface = Color(0xFF1B1E26)
    val SurfaceHigh = Color(0xFF262A35)
    val Amber = Color(0xFFF2B33D)
    val Ink = Color(0xFFECE6D8)
    val Muted = Color(0xFF9A9486)
    val Good = Color(0xFF7BC47F)
    val Bad = Color(0xFFE5675E)

    /** Board group colours, easiest to trickiest. */
    val Levels = listOf(
        Color(0xFFF9DF6D),
        Color(0xFFA0C35A),
        Color(0xFFB0C4EF),
        Color(0xFFBA81C5),
    )
}

@Composable
fun AlibiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AlibiColors.Amber,
            onPrimary = Color.Black,
            background = AlibiColors.Background,
            onBackground = AlibiColors.Ink,
            surface = AlibiColors.Surface,
            onSurface = AlibiColors.Ink,
            surfaceVariant = AlibiColors.SurfaceHigh,
            onSurfaceVariant = AlibiColors.Muted,
        ),
        content = content,
    )
}
