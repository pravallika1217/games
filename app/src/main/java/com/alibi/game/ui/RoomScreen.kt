package com.alibi.game.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.engine.cases.ChatLine
import com.alibi.engine.cases.Mood
import com.alibi.engine.cases.Speaker
import com.alibi.game.GameViewModel

/** The interrogation room: call in suspects, show evidence, watch them sweat. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomScreen(vm: GameViewModel) {
    val file = vm.file
    val room = vm.room
    val current = vm.current
    val suspect = file.suspects[current]
    val released = current in room.released

    Page {
        SceneTitle("Interrogation room", "One of them is lying", "Call in a suspect. Show them your evidence and watch how they react.")

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Noir.Night2)
                .background(Brush.radialGradient(listOf(Noir.Lamp.copy(alpha = 0.22f), Color.Transparent), center = Offset.Unspecified, radius = 600f))
                .border(1.dp, Noir.Night3, RoundedCornerShape(18.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SwingingLamp()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                file.suspects.forEachIndexed { i, s ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == current) Noir.LampSoft else Noir.Night)
                            .border(1.dp, if (i == current) Noir.Lamp else Noir.Night3, RoundedCornerShape(12.dp))
                            .clickable { vm.callIn(i) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(s.emoji, fontSize = 28.sp, modifier = Modifier.graphicsLayer { alpha = if (i in room.released) 0.5f else 1f })
                        Text(s.shortName, color = if (i == current) Noir.Fg else Noir.Muted, fontSize = 12.sp)
                    }
                }
            }
            SuspectFace(suspect.emoji, room.moods[current], Modifier.padding(top = 18.dp))
            Text(suspect.name, color = Noir.Fg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
            Chat(room.chats[current], typing = vm.typing == current)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SHOW EVIDENCE", color = Noir.Lamp, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 2.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                file.statements.forEachIndexed { i, st ->
                    val have = i in vm.wall.solved
                    val used = i in room.shown[current]
                    Text(
                        if (have) st.label else "🔒 Missing evidence",
                        color = Noir.PaperInk,
                        fontFamily = Noir.Typewriter,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .graphicsLayer { alpha = if (!have) 0.3f else if (used) 0.55f else 1f }
                            .clip(RoundedCornerShape(50))
                            .background(Noir.Paper)
                            .clickable(enabled = have && !used) { vm.showEvidence(i) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Notebook(room.notes)

        vm.commissionerSays?.let {
            Text(
                "🚨 Commissioner Rathore: \"$it\" You have ${room.warrantsLeft} warrant left.",
                color = Noir.Fg,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Noir.Bad.copy(alpha = 0.12f))
                    .border(1.dp, Noir.Bad.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        }

        LampButton(
            text = when {
                released -> "${suspect.name} was released"
                vm.arrestArmed -> "Tap again to arrest ${suspect.name}"
                else -> "🚔 Arrest ${suspect.name}"
            },
            onClick = vm::arrest,
            enabled = !released && vm.typing == null,
            color = Noir.Bad,
        )
        Text(
            "Arrest warrants left: ${"📄".repeat(room.warrantsLeft)} (${room.warrantsLeft})",
            color = Noir.Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SwingingLamp() {
    val swing = rememberInfiniteTransition(label = "lamp")
    val angle by swing.animateFloat(6f, -6f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "angle")
    Column(
        Modifier.graphicsLayer {
            rotationZ = angle
            transformOrigin = TransformOrigin(0.5f, 0f)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(2.dp).height(30.dp).background(Color(0xFF555555)))
        Box(
            Modifier
                .size(width = 36.dp, height = 16.dp)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(Noir.Lamp.copy(alpha = 0.55f), Color.Transparent)), radius = 60.dp.toPx(), center = Offset(size.width / 2, size.height))
                }
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(Color(0xFF3A3F46))
        )
    }
}

@Composable
private fun SuspectFace(emoji: String, mood: Mood, modifier: Modifier) {
    val anim = rememberInfiniteTransition(label = "face")
    val tremble by anim.animateFloat(-1.5f, 1.5f, infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse), label = "tremble")
    val drip by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "drip")
    Box(modifier.size(110.dp), contentAlignment = Alignment.Center) {
        Text(
            emoji,
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer { translationX = if (mood == Mood.NERVOUS) tremble * density else 0f },
        )
        when (mood) {
            Mood.NERVOUS -> Text(
                "💦",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        translationY = drip * 26f * density
                        alpha = 1f - drip
                    },
            )
            Mood.RELIEVED -> Text("😌", fontSize = 26.sp, modifier = Modifier.align(Alignment.BottomStart))
            Mood.CALM -> Unit
        }
    }
}

@Composable
private fun Chat(lines: List<ChatLine>, typing: Boolean) {
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size, typing) { scroll.animateScrollTo(scroll.maxValue) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(max = 280.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lines.forEach { ChatBubble(it) }
        if (typing) ChatBubble(ChatLine(Speaker.SUSPECT, "• • •"))
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    when (line.from) {
        Speaker.ROOM -> Text(
            line.text,
            color = Noir.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Speaker.SUSPECT -> Row(Modifier.fillMaxWidth()) {
            Text(
                line.text,
                color = Noir.Fg,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(Noir.Night3)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        Speaker.DETECTIVE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                line.text,
                color = Color(0xFF1A1408),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(Noir.Lamp)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
    }
}

/** The detective's notebook, written by hand on lined paper. */
@Composable
private fun Notebook(notes: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Noir.NotebookPaper)
            .drawBehind {
                val line = 28.dp.toPx()
                var y = line + 12.dp.toPx()
                while (y < size.height) {
                    drawLine(Noir.NotebookInk.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    y += line
                }
                drawLine(Noir.String.copy(alpha = 0.45f), Offset(22.dp.toPx(), 0f), Offset(22.dp.toPx(), size.height), 1.dp.toPx())
            }
            .padding(start = 34.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Text("Inspector's notebook", color = Noir.NotebookInk, fontFamily = Noir.Handwriting, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
        notes.forEach {
            Text("• $it", color = Noir.NotebookInk, fontFamily = Noir.Handwriting, fontSize = 19.sp, lineHeight = 28.sp, fontStyle = FontStyle.Normal)
        }
    }
}
