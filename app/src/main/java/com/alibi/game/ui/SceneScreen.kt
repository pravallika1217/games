package com.alibi.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alibi.engine.cases.SceneItem
import com.alibi.game.GameViewModel
import kotlin.math.hypot

/** How far the torch's light reaches, and how close a tap must be to pick something up. */
private val LIGHT_RADIUS = 90.dp
private val TAP_REACH = 36.dp

/** The dark crime scene. Drag the torch around, and tap a clue when you see it sparkle. */
@Composable
fun SceneScreen(vm: GameViewModel) {
    val scene = vm.file.scene
    val count = vm.found.count { it }
    Page {
        SceneTitle("Crime scene · ${scene.place}", "Search the room", "It's dark. Drag your finger to move the torch. When you see a clue ✨, tap it to pick it up.")

        // The evidence bag fills up as you pick things up.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val bagged = scene.evidence.filterIndexed { i, _ -> vm.found[i] }
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Noir.Night2)
                        .border(1.dp, if (i < bagged.size) Noir.Lamp else Noir.Night3, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(bagged.getOrNull(i)?.emoji ?: "", fontSize = 24.sp) }
            }
            Text("🛍️ Evidence bag: $count / 3", color = Noir.Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }

        DarkRoom(vm)

        if (vm.allFound) {
            LampButton("Back to the station 🚓", onClick = vm::backToStation)
        }
    }

    vm.inspecting?.let { i ->
        val item = scene.evidence[i]
        Dialog(onDismissRequest = vm::putInBag) {
            Column(
                Modifier
                    .graphicsLayer { rotationZ = -1f }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Noir.Paper)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🔍 EVIDENCE #${vm.found.count { it } + 1}", color = Noir.String, fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 3.sp)
                Text(item.emoji, fontSize = 64.sp)
                Text(item.name, color = Noir.PaperInk, fontFamily = Noir.Typewriter, fontSize = 22.sp, textAlign = TextAlign.Center)
                Text(item.text, color = Noir.PaperInk, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
                LampButton("Put it in the bag 🛍️", onClick = vm::putInBag, color = Noir.PaperInk)
            }
        }
    }
}

@Composable
private fun DarkRoom(vm: GameViewModel) {
    val scene = vm.file.scene
    var torch by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    val density = LocalDensity.current
    val lightPx = with(density) { LIGHT_RADIUS.toPx() }
    val reachPx = with(density) { TAP_REACH.toPx() }
    val sparkle = rememberInfiniteTransition(label = "sparkle")
    val glow by sparkle.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF2F3A3A), 0.38f to Color(0xFF2F3A3A),
                    0.38f to Color(0xFF3B2F25), 1f to Color(0xFF3B2F25),
                )
            )
            .pointerInput(Unit) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = 0f
                    var last = down.position
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            // The torch follows your finger's movement, like holding a real torch.
                            val delta = change.position - last
                            last = change.position
                            moved += delta.getDistance()
                            torch = Offset(
                                (torch.x + delta.x / w).coerceIn(0f, 1f),
                                (torch.y + delta.y / h).coerceIn(0f, 1f),
                            )
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    // A tap (not a drag) picks up a clue, but only one you can see in the light.
                    if (moved < 12f) {
                        val tx = torch.x * w
                        val ty = torch.y * h
                        scene.evidence.forEachIndexed { i, item ->
                            if (vm.found[i]) return@forEachIndexed
                            val ix = item.x * w
                            val iy = item.y * h
                            val inLight = hypot(ix - tx, iy - ty) < lightPx * 0.8f
                            val tapped = hypot(ix - down.position.x, iy - down.position.y) < reachPx
                            if (inLight && tapped) vm.pickUp(i)
                        }
                    }
                }
            }
    ) {
        val w = maxWidth
        val h = maxHeight
        Box(Modifier.offset(y = h * 0.30f).fillMaxWidth().height(h * 0.14f).background(Color(0xFF5A4A3A)))
        scene.decor.forEach { SceneEmoji(it, w, h, 34) }
        scene.evidence.forEachIndexed { i, item ->
            if (!vm.found[i]) SceneEmoji(item, w, h, 30)
        }

        // Darkness everywhere except the torch's circle of light.
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(torch.x * size.width, torch.y * size.height)
            drawRect(
                Brush.radialGradient(
                    0f to Color(0x14FFECBE),
                    0.5f to Color.Transparent,
                    1f to Color(0xF7050608),
                    center = center,
                    radius = lightPx * 1.4f,
                )
            )
            // Clues inside the light sparkle, so you know you can tap them.
            scene.evidence.forEachIndexed { i, item ->
                if (vm.found[i]) return@forEachIndexed
                val p = Offset(item.x * size.width, item.y * size.height)
                if ((p - center).getDistance() < lightPx * 0.8f) {
                    drawCircle(Noir.Lamp.copy(alpha = 0.35f * glow), radius = 26.dp.toPx(), center = p)
                    drawCircle(Noir.Lamp.copy(alpha = 0.9f * glow), radius = 26.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
                }
            }
        }

        val anyLit = scene.evidence.withIndex().any { (i, item) ->
            !vm.found[i] && hypot((item.x - torch.x) * w.value, (item.y - torch.y) * h.value) < LIGHT_RADIUS.value * 0.8f
        }
        Text(
            when {
                vm.allFound -> "✅ All evidence bagged. Time to head back to the station."
                anyLit -> "✨ Something's there! Tap it to pick it up."
                else -> "🔦 Drag your finger to move the torch."
            },
            color = Noir.Fg,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE00D0F13))
                .border(1.dp, Noir.Night3, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SceneEmoji(item: SceneItem, w: Dp, h: Dp, sizeSp: Int) {
    val half = (sizeSp * 0.6f).dp
    Text(item.emoji, fontSize = sizeSp.sp, modifier = Modifier.offset(x = w * item.x - half, y = h * item.y - half))
}
