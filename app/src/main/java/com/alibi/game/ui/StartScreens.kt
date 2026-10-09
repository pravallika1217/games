package com.alibi.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import kotlinx.coroutines.delay

/** First launch: one question, one button. */
@Composable
fun NameScreen(vm: GameViewModel) {
    var input by rememberSaveable { mutableStateOf(vm.name) }
    CaseScreen(step = null, bottom = {
        BigButton("Start", onClick = { vm.saveName(input) }, enabled = input.isNotBlank())
    }) {
        Spacer(Modifier.height(60.dp))
        Text("🕵️", fontSize = 64.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Title("What's your name, Detective?", "Everyone at the station will call you Inspector.")
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(16) },
            placeholder = { Text("Your name") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 20.sp, color = Noir.Text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.saveName(input) }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Noir.Amber, unfocusedBorderColor = Noir.Line),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Home: one big "today's case" card, the streak, and the cases to come. */
@Composable
fun HomeScreen(vm: GameViewModel) {
    val done = vm.todaysResult
    CaseScreen(step = null, bottom = {
        if (done == null) BigButton("Play", onClick = vm::play)
        else {
            BigButton("See today's result", onClick = vm::showTodaysResult)
            QuietButton("Replay for practice", onClick = vm::play)
        }
    }) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Good evening,", color = Noir.Dim, fontSize = 13.sp)
                Text("Inspector ${vm.name}", color = Noir.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = vm::changeName))
            }
            Text(if (vm.soundOn) "🔊" else "🔇", fontSize = 18.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = vm::toggleSound).padding(8.dp))
            Text(
                "🔥 ${vm.streak}",
                color = Noir.Text,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Noir.Panel).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Noir.Panel).border(1.dp, Noir.Line, RoundedCornerShape(20.dp))) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFF6B3A5A), Color(0xFF241A2C), Color(0xFF14121A)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(vm.file.people.joinToString(" ") { it.emoji }, fontSize = 30.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    if (done == null) "TODAY'S CASE" else "CASE CLOSED ✓",
                    color = Noir.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0x8C000000)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CASE #${vm.case.number}", color = Noir.Amber, fontFamily = Noir.Typewriter, fontSize = 13.sp, letterSpacing = 1.sp)
                Text(vm.file.title, color = Noir.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("⏱ 5 min   🔎 ${vm.file.leads.size} clues   👥 ${vm.file.people.size} people", color = Noir.Dim, fontSize = 14.sp)
            }
        }

        Text("Your cases", color = Noir.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        (0 until 3).forEach { offset ->
            val number = vm.case.number + offset
            val title = if (offset == 0) vm.file.title else "Case #$number"
            val today = offset == 0
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Noir.Panel)
                    .border(1.dp, Noir.Line, RoundedCornerShape(14.dp))
                    .graphicsLayer { alpha = if (today) 1f else 0.55f }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(if (today) Noir.Amber else Noir.Panel2),
                    contentAlignment = Alignment.Center,
                ) { Text("$number", color = if (today) Noir.AmberInk else Noir.Text, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Noir.Text, fontSize = 15.sp)
                    Text(
                        when (offset) { 0 -> "Today"; 1 -> "Unlocks tomorrow"; else -> "Unlocks in $offset days" },
                        color = Noir.Dim,
                        fontSize = 13.sp,
                    )
                }
                Text(if (today) "▶" else "🔒")
            }
        }
        Text("🎖️ Rank: ${vm.careerRank.title} · ${vm.solvedCount} solved", color = Noir.Dim, fontSize = 13.sp)
    }
}

/** A real-looking incoming call. Answer, hear two lines, go. */
@Composable
fun CallScreen(vm: GameViewModel) {
    var answered by remember { mutableStateOf(false) }
    var declined by remember { mutableStateOf(false) }
    var linesShown by remember { mutableIntStateOf(0) }
    val lines = vm.file.callLines(vm.name)

    LaunchedEffect(answered) {
        if (!answered) while (true) { vm.sfx.ring(); delay(2200) }
        else while (linesShown < lines.size) { linesShown++; delay(1100) }
    }
    val ring = rememberInfiniteTransition(label = "ring")
    val pulse by ring.animateFloat(1f, 1.4f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "pulse")
    val bob by ring.animateFloat(0f, -6f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bob")

    CaseScreen(step = null, scroll = false, bottom = {
        BigButton("Go to the crime scene", onClick = vm::goToScene, enabled = linesShown >= lines.size)
    }) {
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (!answered) Box(
                        Modifier.size(112.dp).scale(pulse).graphicsLayer { alpha = 1.4f - pulse }
                            .border(2.dp, Noir.Amber, RoundedCornerShape(50))
                    )
                    Box(Modifier.size(112.dp).clip(RoundedCornerShape(50)).background(Noir.Panel2), contentAlignment = Alignment.Center) {
                        Text("👮‍♂️", fontSize = 60.sp)
                    }
                }
                Text("Commissioner", color = Noir.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        answered -> "On call"
                        declined -> "You can't ignore the Commissioner 😅"
                        else -> "Incoming call…"
                    },
                    color = Noir.Dim,
                )
            }
            Column(Modifier.heightIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lines.take(linesShown).forEach {
                    Text(it, color = Noir.Text, fontSize = 19.sp, lineHeight = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            if (!answered) {
                Row(horizontalArrangement = Arrangement.spacedBy(70.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    CallButton("✕", "Decline", Noir.Red, 0f) { declined = true }
                    CallButton("📞", "Answer", Noir.Green, bob) { answered = true }
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CallButton(icon: String, label: String, color: Color, lift: Float, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .graphicsLayer { translationY = lift * density }
                .size(72.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 28.sp, color = Color.White) }
        Text(label, color = Noir.Dim, fontSize = 13.sp)
    }
}
