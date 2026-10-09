package com.alibi.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Night-time detective palette: dark room, lamp amber, case-file paper, red string. */
object Noir {
    val Bg = Color(0xFF101318)
    val Panel = Color(0xFF191D24)
    val Panel2 = Color(0xFF232934)
    val Line = Color(0xFF2D3440)
    val Text = Color(0xFFF1ECE2)
    val Dim = Color(0xFF9A958B)
    val Amber = Color(0xFFF3B544)
    val AmberDark = Color(0xFFB8852A)
    val AmberInk = Color(0xFF1D1505)
    val Green = Color(0xFF58C27D)
    val GreenDark = Color(0xFF368A55)
    val GreenBg = Color(0xFF183224)
    val Red = Color(0xFFEC5B52)
    val RedDark = Color(0xFFA23A33)
    val RedBg = Color(0xFF3A1A1A)
    val Paper = Color(0xFFF3EAD6)
    val PaperEdge = Color(0xFFB9AD93)
    val Ink = Color(0xFF2A241C)
    val String = Color(0xFFC8322B)
    val Tape = Color(0xFFF3C623)

    /** Typed case notes. */
    val Typewriter = FontFamily.Monospace
}

@Composable
fun AlibiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Noir.Amber,
            onPrimary = Noir.AmberInk,
            background = Noir.Bg,
            onBackground = Noir.Text,
            surface = Noir.Panel,
            onSurface = Noir.Text,
            surfaceVariant = Noir.Panel2,
            onSurfaceVariant = Noir.Dim,
            error = Noir.Red,
        ),
        content = content,
    )
}
