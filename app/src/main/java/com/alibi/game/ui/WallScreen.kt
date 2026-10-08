package com.alibi.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alibi.game.GameViewModel

private val NOTE_COLORS = listOf(Noir.Paper, Noir.Sticky, Noir.Memo)

/**
 * The evidence wall. Witnesses come one at a time and say exactly what to find
 * ("Find the 4 SWEETS"). Tap the right notes on the cork to pin them with red string.
 */
@Composable
fun WallScreen(vm: GameViewModel) {
    val wall = vm.wall
    val file = vm.file
    Page {
        SceneTitle("Police station · Evidence wall", "The witnesses are here")

        val task = wall.current
        if (task != null) {
            WitnessCard(
                witness = file.statements[wall.task].witness,
                ask = file.statements[wall.task].ask,
                found = wall.picked.size,
                number = wall.task + 1,
                total = wall.groups.size,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Wrong notes cost Pandu's chai", color = Noir.Muted, fontSize = 13.sp)
            Row {
                repeat(wall.maxMistakes) { i ->
                    Text("☕", fontSize = 20.sp, modifier = Modifier.graphicsLayer { alpha = if (i < wall.mistakes) 0.18f else 1f })
                }
            }
        }

        if (!wall.isOver || vm.filing.isNotEmpty()) Corkboard(vm)

        // Statements collected so far, and the ones that were missed.
        file.groups.indices.forEach { gi ->
            when (gi) {
                in wall.solved -> Envelope("STATEMENT", "${file.statements[gi].label}: ${file.statements[gi].text}", missed = false)
                in wall.missed -> Envelope("MISSED", "${file.statements[gi].witness} left without talking.", missed = true)
            }
        }

        PanduSays(vm.panduLine)

        if (wall.isOver && vm.filing.isEmpty()) {
            LampButton("To the interrogation room 💡", onClick = vm::toInterrogation)
        } else {
            GhostButton("👮 Ask Pandu for a hint", onClick = vm::askPandu, modifier = Modifier.fillMaxWidth())
        }
    }

    vm.newStatement?.let { gi ->
        val st = file.statements[gi]
        Dialog(onDismissRequest = vm::dismissStatement) {
            Column(
                Modifier
                    .graphicsLayer { rotationZ = -1f }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Noir.Paper)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("NEW STATEMENT", color = Noir.String, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 3.sp)
                Text(st.witness, color = Noir.PaperInk, fontFamily = Noir.Typewriter, fontSize = 20.sp, lineHeight = 26.sp)
                Text(st.text, color = Noir.PaperInk, fontSize = 17.sp, lineHeight = 25.sp)
                Text("Show this to the suspects in the interrogation room.", color = Noir.PaperInk.copy(alpha = 0.6f), fontSize = 13.sp)
                LampButton("Pin it to the file", onClick = vm::dismissStatement, color = Noir.PaperInk)
            }
        }
    }
}

/** The witness who is talking now, what they want you to find, and how many you've found. */
@Composable
private fun WitnessCard(witness: String, ask: String, found: Int, number: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Noir.Night2)
            .border(1.dp, Noir.Lamp, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("WITNESS $number OF $total", color = Noir.Lamp, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 2.sp)
        Text(witness, color = Noir.Fg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("\"$ask\"", color = Noir.Fg, fontSize = 16.sp, lineHeight = 23.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < found) Noir.String else Noir.Night3)
                )
            }
            Text("Found $found / 4", color = Noir.Muted, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun Corkboard(vm: GameViewModel) {
    val wall = vm.wall
    val density = LocalDensity.current
    val pins = remember { mutableStateMapOf<String, Offset>() }
    var wallOrigin by remember { mutableStateOf(Offset.Zero) }
    // Each note keeps its own tilt and paper for the whole case.
    val looks = remember(wall.groups) {
        wall.tiles.sorted().mapIndexed { i, w -> w to (((i * 37) % 9) - 4f to NOTE_COLORS[(i * 5) % 3]) }.toMap()
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(vm.shakeTick) {
        if (vm.shakeTick > 0) for (x in listOf(-7f, 7f, -5f, 4f, 0f)) shake.animateTo(x, tween(60))
    }
    val pulse = rememberInfiniteTransition(label = "hint")
    val glow by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "glow")

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Noir.CorkFrame)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.radialGradient(listOf(Noir.Cork, Color(0xFF8A6039)), radius = 900f))
            .padding(horizontal = 12.dp, vertical = 16.dp)
            // Measured inside the padding, where the string canvas below is drawn.
            .onGloballyPositioned { wallOrigin = it.positionInRoot() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            wall.tiles.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { word ->
                        val (tilt, paper) = looks.getValue(word)
                        Note(
                            word = word,
                            tilt = tilt,
                            paper = paper,
                            pinned = word in wall.picked,
                            filing = word in vm.filing,
                            wrong = word in wall.shaken,
                            glow = if (word in wall.hinted && word !in wall.picked) glow else 0f,
                            shakeX = if (word == vm.shakeWord) shake.value else 0f,
                            modifier = Modifier.weight(1f),
                            onPositioned = { c ->
                                val p = c.positionInRoot()
                                pins[word] = Offset(p.x + c.size.width / 2f, p.y + with(density) { 12.dp.toPx() })
                            },
                            onClick = { vm.tapNote(word) },
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Red string from pin to pin, in the order the notes were found.
        Canvas(Modifier.matchParentSize()) {
            val points = wall.picked.mapNotNull { pins[it] }.map { it - wallOrigin }
            if (points.size < 2) return@Canvas
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    quadraticTo((a.x + b.x) / 2, (a.y + b.y) / 2 + 22.dp.toPx(), b.x, b.y)
                }
            }
            drawPath(path, color = Noir.String, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun Note(
    word: String,
    tilt: Float,
    paper: Color,
    pinned: Boolean,
    filing: Boolean,
    wrong: Boolean,
    glow: Float,
    shakeX: Float,
    modifier: Modifier,
    onPositioned: (LayoutCoordinates) -> Unit,
    onClick: () -> Unit,
) {
    val lift by animateFloatAsState(if (pinned) 1f else 0f, tween(180), label = "lift")
    val gone by animateFloatAsState(if (filing) 1f else 0f, tween(650), label = "file")
    Box(
        modifier
            .onGloballyPositioned(onPositioned)
            .graphicsLayer {
                rotationZ = tilt + gone * 20f
                translationX = shakeX * density
                translationY = -5f * lift * density + gone * 120f * density
                val s = (1f + 0.05f * lift) * (1f - 0.8f * gone)
                scaleX = s
                scaleY = s
                alpha = (1f - gone) * if (wrong) 0.45f else 1f
            }
            .shadow((4 + 8 * lift).dp, RoundedCornerShape(2.dp))
            .background(paper, RoundedCornerShape(2.dp))
            .border(3.dp, Noir.Lamp.copy(alpha = glow), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 62.dp)
            .padding(start = 2.dp, end = 2.dp, top = 18.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = -12f * density }
                .size(12.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(if (pinned) listOf(Color(0xFFFF8A80), Noir.String) else listOf(Color(0xFFDDDDDD), Color(0xFF777777))))
        )
        Text(
            word.uppercase(),
            color = Noir.PaperInk,
            fontFamily = Noir.Typewriter,
            fontSize = if (word.length > 7) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )
        if (wrong) Text("✗", color = Noir.String, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun Envelope(top: String, text: String, missed: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (missed) Noir.Night2 else Noir.Envelope)
            .then(if (missed) Modifier.border(1.dp, Noir.Night3, RoundedCornerShape(6.dp)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(top, color = if (missed) Noir.Muted else Noir.PaperInk.copy(alpha = 0.7f), fontFamily = Noir.Typewriter, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(text, color = if (missed) Noir.Muted else Noir.PaperInk, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
