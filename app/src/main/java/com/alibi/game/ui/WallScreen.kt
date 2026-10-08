package com.alibi.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.PathEffect
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

/** The evidence wall: pinned notes on cork, tied together with red string. */
@Composable
fun WallScreen(vm: GameViewModel) {
    val board = vm.board
    val file = vm.file
    Page {
        SceneTitle("Police station · Evidence wall", "Connect the evidence", "Tap 4 notes that belong together. Pandu will tie them with red string.")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Pandu's patience", color = Noir.Muted, fontSize = 14.sp)
            Row {
                repeat(board.maxMistakes) { i ->
                    Text("☕", fontSize = 20.sp, modifier = Modifier.graphicsLayer { alpha = if (i < board.mistakes) 0.18f else 1f })
                }
            }
        }

        Corkboard(vm)

        // Envelopes for connected evidence, and the ones that were missed.
        file.groups.indices.forEach { gi ->
            when {
                gi in board.solvedOrder -> Envelope("EVIDENCE FILE", file.statements[gi].label, missed = false)
                board.isOver -> Envelope("MISSED", "${file.groups[gi].title}: ${file.groups[gi].items.joinToString(", ")}", missed = true)
            }
        }

        PanduSays(vm.panduLine)

        if (board.isOver) {
            LampButton("To the interrogation room 💡", onClick = vm::toInterrogation)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Ask Pandu for a hint", onClick = vm::askPandu, modifier = Modifier.weight(1f))
                GhostButton("Untie string", onClick = vm::untie, modifier = Modifier.weight(1f))
            }
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
                Text("NEW EVIDENCE", color = Noir.String, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 3.sp)
                Text("${file.groups[gi].title} → ${st.label}", color = Noir.PaperInk, fontFamily = Noir.Typewriter, fontSize = 20.sp, lineHeight = 26.sp)
                Text(st.text, color = Noir.PaperInk, fontSize = 16.sp, lineHeight = 24.sp)
                LampButton("Pin it to the file", onClick = vm::dismissStatement, color = Noir.PaperInk)
            }
        }
    }
}

@Composable
private fun Corkboard(vm: GameViewModel) {
    val board = vm.board
    val density = LocalDensity.current
    val pins = remember { mutableStateMapOf<String, Offset>() }
    var wallOrigin by remember { mutableStateOf(Offset.Zero) }
    // Each note keeps its own tilt and paper for the whole case.
    val looks = remember(board.groups) {
        board.groups.flatMap { it.items }.mapIndexed { i, w -> w to (((i * 37) % 9) - 4f to NOTE_COLORS[(i * 5) % 3]) }.toMap()
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(vm.snapTick) {
        if (vm.snapTick > 0) for (x in listOf(-6f, 6f, -4f, 3f, 0f)) shake.animateTo(x, tween(60))
    }

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
            board.tiles.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { word ->
                        val (tilt, paper) = looks.getValue(word)
                        Note(
                            word = word,
                            tilt = tilt,
                            paper = paper,
                            selected = word in vm.selection,
                            filing = word in vm.filing,
                            shakeX = if (vm.snapping && word in vm.selection) shake.value else 0f,
                            enabled = !board.isOver,
                            modifier = Modifier.weight(1f),
                            onPositioned = { c ->
                                val p = c.positionInRoot()
                                pins[word] = Offset(p.x + c.size.width / 2f, p.y + with(density) { 12.dp.toPx() })
                            },
                            onClick = { vm.toggleNote(word) },
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // The red string, drawn from pin to pin in the order the notes were picked.
        Canvas(Modifier.matchParentSize()) {
            val points = vm.selection.mapNotNull { pins[it] }.map { it - wallOrigin }
            if (points.size < 2) return@Canvas
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    quadraticTo((a.x + b.x) / 2, (a.y + b.y) / 2 + 22.dp.toPx(), b.x, b.y)
                }
            }
            drawPath(
                path,
                color = Noir.String.copy(alpha = if (vm.snapping) 0.5f else 1f),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (vm.snapping) PathEffect.dashPathEffect(floatArrayOf(6f, 14f)) else null,
                ),
            )
        }
    }
}

@Composable
private fun Note(
    word: String,
    tilt: Float,
    paper: Color,
    selected: Boolean,
    filing: Boolean,
    shakeX: Float,
    enabled: Boolean,
    modifier: Modifier,
    onPositioned: (LayoutCoordinates) -> Unit,
    onClick: () -> Unit,
) {
    val lift by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "lift")
    val gone by animateFloatAsState(if (filing) 1f else 0f, tween(550), label = "file")
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
                alpha = 1f - gone
            }
            .shadow((4 + 8 * lift).dp, RoundedCornerShape(2.dp))
            .background(paper, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 62.dp)
            .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = -12f * density }
                .size(12.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(if (selected) listOf(Color(0xFFFF8A80), Noir.String) else listOf(Color(0xFFDDDDDD), Color(0xFF777777))))
        )
        Text(
            word.uppercase(),
            color = Noir.PaperInk,
            fontFamily = Noir.Typewriter,
            fontSize = if (word.length > 8) 12.sp else 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )
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
        Text(text, color = if (missed) Noir.Muted else Noir.PaperInk, fontFamily = Noir.Typewriter, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
