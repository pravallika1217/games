package com.alibi.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alibi.game.GameViewModel
import com.alibi.game.Screen
import kotlinx.coroutines.delay

@Composable
fun AlibiApp(vm: GameViewModel = viewModel()) {
    BackHandler(enabled = vm.screen !in setOf(Screen.Id, Screen.Desk)) { vm.backToDesk() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Noir.Night)
            .safeDrawingPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("A L I B I", color = Noir.Lamp, fontFamily = Noir.Typewriter, fontSize = 15.sp)
            TextButton(onClick = vm::toggleSound) {
                Text(if (vm.soundOn) "🔊 Sound on" else "🔇 Sound off", color = Noir.Muted, fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (vm.screen) {
                Screen.Id -> IdScreen(vm)
                Screen.Desk -> DeskScreen(vm)
                Screen.Call -> CallScreen(vm)
                Screen.Scene -> SceneScreen(vm)
                Screen.Wall -> WallScreen(vm)
                Screen.Room -> RoomScreen(vm)
                Screen.Arrest -> ArrestScreen(vm)
                Screen.News -> NewsScreen(vm)
            }
        }
    }
}

/** A scrollable page with the shared gutter and spacing. */
@Composable
fun Page(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** Small typed label above a screen's title, like a case-file heading. */
@Composable
fun SceneTitle(label: String, title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = Noir.Lamp, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 2.sp)
        Text(title, color = Noir.Fg, fontFamily = Noir.Typewriter, fontSize = 25.sp, lineHeight = 30.sp)
        if (hint != null) Text(hint, color = Noir.Muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun LampButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Noir.Lamp) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == Noir.Bad) Color.White else Color(0xFF1A1408),
            disabledContainerColor = Noir.Night3,
            disabledContentColor = Noir.Muted,
        ),
    ) { Text(text, Modifier.padding(vertical = 6.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Noir.Night3),
    ) { Text(text, color = Noir.Fg, modifier = Modifier.padding(vertical = 4.dp)) }
}

/** A speech bubble from Constable Pandu, the detective's helpful sidekick. */
@Composable
fun PanduSays(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.background(Noir.Night3, RoundedCornerShape(50)).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) { Text("👮", fontSize = 22.sp) }
        Column(
            Modifier
                .weight(1f)
                .background(Noir.Night2, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                .border(1.dp, Noir.Night3, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("Constable Pandu", color = Noir.Lamp, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text, color = Noir.Fg, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

/**
 * Types [text] out letter by letter, with a blinking-style cursor. Tap to show it all at once.
 * [onDone] runs once the whole text is visible.
 */
@Composable
fun Typewriter(text: String, style: TextStyle, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    var shown by remember(text) { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(text) {
        while (shown < text.length) {
            val c = text[shown]
            delay(if (c == '.' || c == '?' || c == '!') 220L else 28L)
            shown++
        }
        done()
    }
    Text(
        text = text.take(shown) + if (shown < text.length) "▌" else "",
        style = style,
        modifier = modifier.pointerInput(text) { detectTapGestures { shown = text.length } },
    )
}
