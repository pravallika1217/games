package com.alibi.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Detective noir: a dark night, amber lamp light, paper, cork and red string. */
object Noir {
    val Night = Color(0xFF0D0F13)
    val Night2 = Color(0xFF161A20)
    val Night3 = Color(0xFF222832)
    val Lamp = Color(0xFFF0B44C)
    val LampSoft = Color(0x29F0B44C)
    val Paper = Color(0xFFF2E9D4)
    val PaperInk = Color(0xFF2A241C)
    val Sticky = Color(0xFFF6DD7A)
    val Memo = Color(0xFFDFE7EF)
    val Cork = Color(0xFFA87A4F)
    val CorkFrame = Color(0xFF4A3523)
    val Envelope = Color(0xFFD9C39A)
    val String = Color(0xFFC8322B)
    val Fg = Color(0xFFEBE5D8)
    val Muted = Color(0xFF958F84)
    val Good = Color(0xFF7CC48A)
    val Bad = Color(0xFFE2594F)
    val NotebookPaper = Color(0xFFF7F1DF)
    val NotebookInk = Color(0xFF2B3A67)
    val Newsprint = Color(0xFFEFE6CF)
    val NewsInk = Color(0xFF1C1813)

    /** Typed police documents, notes and the confession. */
    val Typewriter = FontFamily.Monospace
    /** The detective's notebook. */
    val Handwriting = FontFamily.Cursive
    /** The newspaper. */
    val News = FontFamily.Serif
}

@Composable
fun AlibiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Noir.Lamp,
            onPrimary = Color(0xFF1A1408),
            background = Noir.Night,
            onBackground = Noir.Fg,
            surface = Noir.Night2,
            onSurface = Noir.Fg,
            surfaceVariant = Noir.Night3,
            onSurfaceVariant = Noir.Muted,
            error = Noir.Bad,
        ),
        content = content,
    )
}
