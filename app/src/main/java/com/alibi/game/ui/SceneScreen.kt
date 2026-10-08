package com.alibi.game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.engine.cases.SceneItem
import com.alibi.game.GameViewModel

/** The dark crime scene. Drag the torch around to find 3 pieces of evidence. */
@Composable
fun SceneScreen(vm: GameViewModel) {
    val scene = vm.file.scene
    val count = vm.found.count { it }
    Page {
        SceneTitle("Crime scene · ${scene.place}", "Search the room", "It's dark. Drag your torch around to find 3 pieces of evidence.")

        // The evidence bag fills up as you find things.
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
            Text("Evidence bag: $count / 3", color = Noir.Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }

        DarkRoom(vm)

        if (vm.allFound) {
            LampButton("Back to the station 🚓", onClick = vm::backToStation)
        }
    }
}

@Composable
private fun DarkRoom(vm: GameViewModel) {
    val scene = vm.file.scene
    var torch by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    val reach = with(LocalDensity.current) { 34.dp.toPx() }

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
                awaitEachGesture {
                    fun move(p: Offset) {
                        val x = p.x.coerceIn(0f, size.width.toFloat())
                        val y = p.y.coerceIn(0f, size.height.toFloat())
                        torch = Offset(x / size.width, y / size.height)
                        vm.torchAt(x, y, size.width.toFloat(), size.height.toFloat(), reach)
                    }
                    move(awaitFirstDown().position)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { move(it.position); it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val w = maxWidth
        val h = maxHeight
        // The kitchen counter / table line, for a sense of space.
        Box(
            Modifier
                .offset(y = h * 0.30f)
                .fillMaxWidth()
                .height(h * 0.14f)
                .background(Color(0xFF5A4A3A))
        )
        scene.decor.forEach { SceneEmoji(it, w, h, 34, scale = 1f) }
        scene.evidence.forEachIndexed { i, item ->
            val scale by animateFloatAsState(if (vm.found[i]) 0f else 1f, tween(500), label = "found$i")
            SceneEmoji(item, w, h, 30, scale)
        }

        // Darkness everywhere except the torch's circle of light.
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.radialGradient(
                    0f to Color(0x14FFECBE),
                    0.45f to Color.Transparent,
                    1f to Color(0xF7050608),
                    center = Offset(torch.x * size.width, torch.y * size.height),
                    radius = 120.dp.toPx(),
                )
            )
        }

        Text(
            vm.sceneMessage ?: "🔦 Drag your finger to move the torch.",
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
private fun SceneEmoji(item: SceneItem, w: Dp, h: Dp, sizeSp: Int, scale: Float) {
    val half = (sizeSp * 0.6f).dp
    Text(
        item.emoji,
        fontSize = sizeSp.sp,
        modifier = Modifier
            .offset(x = w * item.x - half, y = h * item.y - half)
            .scale(scale),
    )
}
