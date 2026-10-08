package com.alibi.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel

/** First launch: the detective gets their police ID card. */
@Composable
fun IdScreen(vm: GameViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    Page {
        SceneTitle("Day one on the job", "Welcome to the Crime Branch, Detective.", "Every day, one new case lands on your desk. First, your ID card.")
        IdCard(name = input.trim().ifEmpty { "______" }, rank = "Rookie")
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(18) },
            label = { Text("What's your name, Detective?") },
            singleLine = true,
            textStyle = TextStyle(fontFamily = Noir.Typewriter, fontSize = 18.sp, color = Noir.Fg),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.reportForDuty(input) }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Noir.Lamp, unfocusedBorderColor = Noir.Night3, focusedLabelColor = Noir.Lamp),
            modifier = Modifier.fillMaxWidth(),
        )
        LampButton("Report for duty", onClick = { vm.reportForDuty(input) }, enabled = input.isNotBlank())
    }
}

/** The detective's desk: ID card, career, streak, and today's case waiting by the phone. */
@Composable
fun DeskScreen(vm: GameViewModel) {
    val rank = vm.careerRank
    Page {
        SceneTitle("Crime Branch · Your desk", "Good evening, Inspector.")
        IdCard(name = vm.name, rank = rank.title)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("🗂️", "${vm.solvedCount}", "cases solved", Modifier.weight(1f))
            StatTile("🌆", "${vm.streak}", if (vm.streak == 1) "day the city is safe" else "days the city is safe", Modifier.weight(1f))
        }
        rank.next?.let { next ->
            val left = next.casesNeeded - vm.solvedCount
            Text(
                "🎖️ Solve $left more case${if (left == 1) "" else "s"} to become ${next.title}",
                color = Noir.Muted,
                fontSize = 14.sp,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Noir.Night2, Noir.Night)))
                .border(1.dp, Noir.Night3, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("CASE #${vm.case.number}", color = Noir.Lamp, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 2.sp)
            if (vm.todaysPage == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    RingingPhone()
                    Column(Modifier.weight(1f)) {
                        Text("Incoming call", color = Noir.Fg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Commissioner Rathore · 11:58 PM", color = Noir.Muted, fontSize = 14.sp)
                    }
                }
                LampButton("Answer the phone 📞", onClick = vm::takeTheCall)
            } else {
                Text(vm.file.title, color = Noir.Fg, fontFamily = Noir.Typewriter, fontSize = 20.sp)
                Text("✅ Case closed. The city reads about you in tomorrow's paper.", color = Noir.Muted, fontSize = 14.sp)
                LampButton("Read today's newspaper 📰", onClick = vm::readTodaysPaper)
                GhostButton("Replay for practice", onClick = vm::takeTheCall, modifier = Modifier.fillMaxWidth())
                Text("A new case arrives tomorrow at midnight.", color = Noir.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun IdCard(name: String, rank: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .rotate(-1.5f)
            .clip(RoundedCornerShape(14.dp))
            .background(Noir.Paper)
    ) {
        Row(Modifier.fillMaxWidth().height(8.dp)) {
            repeat(10) { i -> Box(Modifier.weight(1f).height(8.dp).background(if (i % 2 == 0) Color(0xFF1D3A6B) else Noir.String)) }
        }
        Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 80.dp, height = 96.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFD6CCB6)), contentAlignment = Alignment.Center) {
                Text("🕵️", fontSize = 48.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("CITY POLICE · CRIME BRANCH", color = Color(0xFF5B5244), fontFamily = Noir.Typewriter, fontSize = 11.sp, letterSpacing = 1.sp)
                Text("Inspector $name", color = Noir.PaperInk, fontFamily = Noir.Typewriter, fontSize = 21.sp, lineHeight = 25.sp, modifier = Modifier.padding(vertical = 4.dp))
                Text("Rank: $rank · Badge #0427", color = Color(0xFF5B5244), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Noir.Night2)
            .padding(12.dp)
    ) {
        Text("$emoji $value", color = Noir.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Noir.Muted, fontSize = 13.sp)
    }
}

/** A phone icon with a pulsing ring around it. */
@Composable
fun RingingPhone(size: Int = 56) {
    val pulse = rememberInfiniteTransition(label = "ring")
    val scale by pulse.animateFloat(0.9f, 1.35f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "scale")
    val alpha by pulse.animateFloat(0.9f, 0f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "alpha")
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size.dp)
                .scale(scale)
                .border(2.dp, Noir.Lamp.copy(alpha = alpha), RoundedCornerShape(50))
        )
        Box(Modifier.size((size - 10).dp).clip(RoundedCornerShape(50)).background(Noir.Night3), contentAlignment = Alignment.Center) {
            Text("📞", fontSize = (size / 2).sp)
        }
    }
}
