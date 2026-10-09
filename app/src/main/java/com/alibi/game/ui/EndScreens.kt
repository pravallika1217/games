package com.alibi.game.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- Questioning

/** Tap a suspect, hear their alibi, show them a clue. The right clue catches the liar. */
@Composable
fun QuestioningScreen(vm: GameViewModel) {
    val file = vm.file
    val q = vm.questioning
    val sel = vm.selected
    val gotcha = remember { Animatable(0f) }
    LaunchedEffect(vm.gotchaTick) {
        if (vm.gotchaTick > 0) { gotcha.snapTo(1f); delay(1100); gotcha.animateTo(0f, tween(400)) }
    }
    val shake = rememberInfiniteTransition(label = "tremble")
    val tremble by shake.animateFloat(-2f, 2f, infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse), label = "t")

    Box(Modifier.fillMaxSize()) {
        CaseScreen(step = 3, part = if (q.lying.isNotEmpty()) 1f else if (sel == null) 0f else 0.5f, onClose = vm::goHome, bottom = {
            BigButton(
                if (q.lying.isNotEmpty()) "I know who did it" else "Catch a lie first",
                onClick = vm::goToVote,
                enabled = q.lying.isNotEmpty(),
                tone = Tone.RED,
            )
        }) {
            Title("Who's lying?", "Tap a suspect. Then show them a piece of evidence.")
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                file.suspects.forEachIndexed { i, s ->
                    val lying = i in q.lying
                    Box(Modifier.weight(1f)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel == i) Color(0xFF2A2416) else Noir.Panel)
                                .border(2.dp, if (sel == i) Noir.Amber else Noir.Line, RoundedCornerShape(16.dp))
                                .clickable { vm.selectSuspect(i) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(s.emoji, fontSize = 44.sp, modifier = Modifier.graphicsLayer { translationX = if (lying) tremble * density else 0f })
                            Text(s.name, color = Noir.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                        if (lying) Text(
                            "LYING",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.TopEnd).graphicsLayer { rotationZ = 8f; translationY = -8f * density }
                                .clip(RoundedCornerShape(50)).background(Noir.Red).padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (sel != null) {
                val s = file.suspects[sel]
                Speech("${s.name} says", "\"${s.interview!!.alibi}\"", highlight = false)
                vm.lastReply?.let { Speech(if (it.bySuspect) s.name else "You", it.text, highlight = true) }
                Text("SHOW EVIDENCE TO ${s.shortName.uppercase()}", color = Noir.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                file.leads.forEachIndexed { c, lead ->
                    val used = c in q.shown[sel]
                    EvidenceCard(
                        lead.emoji,
                        lead.card,
                        Modifier.graphicsLayer { alpha = if (used) 0.5f else 1f }.clip(RoundedCornerShape(10.dp)).clickable { vm.showEvidence(c) },
                    )
                }
            }
        }
        if (gotcha.value > 0f) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f * gotcha.value)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "GOTCHA!",
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = -8f; alpha = gotcha.value }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Noir.Red)
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Speech(who: String, text: String, highlight: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .background(Noir.Panel)
            .border(1.dp, if (highlight) Noir.Amber else Noir.Line, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .padding(14.dp)
    ) {
        Text(who, color = Noir.Dim, fontSize = 13.sp)
        Text(text, color = Noir.Text, fontSize = 17.sp, lineHeight = 23.sp)
    }
}

// ---------------------------------------------------------------- Vote and reveal

/** "Who did it?" Pick a face and arrest them. Two warrants. */
@Composable
fun VoteScreen(vm: GameViewModel) {
    val file = vm.file
    val arrest = vm.arrest
    CaseScreen(step = 4, part = 0.3f, onClose = vm::goHome, bottom = {
        val pick = vm.votePick
        BigButton(if (pick == null) "Pick a suspect" else "Arrest ${file.suspects[pick].name}", onClick = vm::makeArrest, enabled = pick != null, tone = Tone.RED)
    }) {
        Title("Who did it?", "You have ${arrest.warrants} arrest warrant${if (arrest.warrants == 1) "" else "s"}.")
        file.suspects.forEachIndexed { i, s ->
            val released = i in arrest.released
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (released) 0.4f else 1f }
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (vm.votePick == i) Noir.RedBg else Noir.Panel)
                    .border(2.dp, if (vm.votePick == i) Noir.Red else Noir.Line, RoundedCornerShape(16.dp))
                    .clickable(enabled = !released) { vm.pickForArrest(i) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(s.emoji, fontSize = 40.sp)
                Column {
                    Text(s.name, color = Noir.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            released -> "Innocent"
                            i in vm.questioning.lying -> "🔴 Caught lying"
                            else -> "\"${s.interview!!.alibi}\""
                        },
                        color = Noir.Dim,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/** The big reveal, like Among Us: the face drops in, then the verdict. */
@Composable
fun RevealScreen(vm: GameViewModel) {
    val file = vm.file
    val who = vm.lastArrested ?: return
    val s = file.suspects[who]
    val right = who == file.culpritIndex
    val escaped = vm.arrest.escaped
    val drop = remember { Animatable(-160f) }
    val words = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        drop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        words.animateTo(1f, tween(500))
    }
    CaseScreen(step = 4, part = 1f, onClose = vm::goHome, scroll = false, bottom = {
        BigButton(if (right || escaped) "See your result" else "Try again", onClick = vm::afterReveal)
    }) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF2A1414), Noir.Bg)))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(s.emoji + if (right) "⛓️" else "", fontSize = 96.sp, modifier = Modifier.graphicsLayer { translationY = drop.value * density; alpha = 1f + drop.value / 160f })
            Text(
                "${s.name} " + if (right) "was the culprit." else "was innocent.",
                color = if (right) Noir.Red else Noir.Green,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 31.sp,
                modifier = Modifier.graphicsLayer { alpha = words.value },
            )
            Text(
                when {
                    right -> file.confession
                    escaped -> file.escapeStory
                    else -> s.interview?.release ?: ""
                },
                color = Noir.Dim,
                fontFamily = Noir.Typewriter,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = words.value },
            )
        }
    }
}

// ---------------------------------------------------------------- Result

/** Wordle-style result: stars, time, mistakes, streak, share, and a countdown to the next case. */
@Composable
fun ResultScreen(vm: GameViewModel) {
    val r = vm.result ?: return
    val context = LocalContext.current
    var left by remember { mutableLongStateOf(0L) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            left = Duration.between(now, LocalDate.now().plusDays(1).atStartOfDay()).seconds
            delay(1000)
        }
    }
    CaseScreen(step = null, bottom = {
        BigButton("Share", onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, r.shareText)
            }
            context.startActivity(Intent.createChooser(send, "Share your result"))
            copied = true
        })
        QuietButton("Back to home", onClick = vm::goHome)
    }) {
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row { (0 until 3).forEach { Text("⭐", fontSize = 40.sp, modifier = Modifier.graphicsLayer { alpha = if (it < r.stars) 1f else 0.2f }) } }
            Text(if (r.caught) "Case closed!" else r.title, color = Noir.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Inspector ${vm.name} · Case #${vm.case.number}${if (r.caught) " · ${r.title}" else ""}", color = Noir.Dim, fontSize = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(r.time, "Time", Modifier.weight(1f))
            Stat("${r.mistakes}", "Mistakes", Modifier.weight(1f))
            Stat("🔥 ${vm.streak}", "Streak", Modifier.weight(1f))
        }
        Text(
            r.shareText,
            color = Noir.Text,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Noir.Panel).border(1.dp, Noir.Line, RoundedCornerShape(14.dp)).padding(14.dp),
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Next case in", color = Noir.Dim, fontSize = 14.sp)
            Text(
                "%02d:%02d:%02d".format(left / 3600, (left / 60) % 60, left % 60),
                color = Noir.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (copied) Text("Send it to your friends and see who solves it faster!", color = Noir.Green, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Noir.Panel).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Noir.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Noir.Dim, fontSize = 12.sp)
    }
}
