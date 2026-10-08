package com.alibi.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Handcuffs, a CASE CLOSED stamp, and the confession. */
@Composable
fun ArrestScreen(vm: GameViewModel) {
    val file = vm.file
    val culprit = file.suspects[file.culprit]
    val caught = vm.room.caught

    val cuffsY = remember { Animatable(-160f) }
    val stampScale = remember { Animatable(3f) }
    val stampAlpha = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var showConfession by remember { mutableStateOf(false) }
    var confessionDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (caught) {
            delay(300)
            launch { cuffsY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
            delay(250)
            vm.sfx.cuffs()
            delay(550)
        } else {
            delay(800)
        }
        vm.sfx.stamp()
        launch { stampAlpha.animateTo(1f, tween(120)) }
        launch { stampScale.animateTo(1f, tween(300)) }
        for (x in listOf(-6f, 5f, -3f, 0f)) shake.animateTo(x, tween(70))
        delay(500)
        showConfession = true
    }

    Page {
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shake.value * density }
                .clip(RoundedCornerShape(18.dp))
                .background(Noir.Night2)
                .background(Brush.radialGradient(listOf(Noir.Bad.copy(alpha = 0.25f), Color.Transparent), center = Offset.Unspecified, radius = 500f))
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(culprit.emoji, fontSize = 96.sp)
                if (caught) {
                    Text("⛓️", fontSize = 54.sp, modifier = Modifier.graphicsLayer { translationY = cuffsY.value * density })
                }
            }
            Text(
                if (caught) "CASE CLOSED" else "ESCAPED",
                color = if (caught) Noir.Bad else Noir.Muted,
                fontFamily = Noir.Typewriter,
                fontSize = 32.sp,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -14f
                        scaleX = stampScale.value
                        scaleY = stampScale.value
                        alpha = stampAlpha.value
                    }
                    .background(Color(0x590D0F13), RoundedCornerShape(8.dp))
                    .border(4.dp, if (caught) Noir.Bad else Noir.Muted, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }

        if (showConfession) {
            Text(
                (if (caught) "${culprit.name}'s confession" else "What happened next").uppercase(),
                color = Noir.Lamp,
                fontFamily = Noir.Typewriter,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Typewriter(
                text = if (caught) file.confession else file.escapeStory,
                style = TextStyle(fontFamily = Noir.Typewriter, fontSize = 17.sp, lineHeight = 27.sp, color = Noir.Fg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                onDone = { confessionDone = true },
            )
        }
        if (confessionDone) {
            LampButton("Read tomorrow's newspaper 📰", onClick = vm::readNewspaper)
        }
    }
}
