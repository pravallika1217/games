package com.alibi.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import kotlinx.coroutines.delay

/** The Commissioner's midnight call. */
@Composable
fun CallScreen(vm: GameViewModel) {
    var answered by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // Keep ringing until the detective picks up.
    LaunchedEffect(answered) {
        while (!answered) {
            vm.sfx.ring()
            delay(2200)
        }
    }

    Page {
        SceneTitle("11:58 PM", if (answered) "On call" else "Your phone is ringing…")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Noir.Night2, Noir.Night)))
                .border(1.dp, Noir.Night3, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (answered) Text("👮‍♂️", fontSize = 56.sp) else RingingPhone(size = 96)
            Text("Commissioner Rathore", color = Noir.Fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(if (answered) "On call · 00:01" else "Incoming call…", color = Noir.Muted, fontSize = 14.sp)
            if (answered) {
                Typewriter(
                    text = vm.file.callText(vm.name),
                    style = TextStyle(fontFamily = Noir.Typewriter, fontSize = 17.sp, lineHeight = 26.sp, color = Noir.Fg),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                    onDone = { finished = true },
                )
            }
        }
        if (!answered) {
            LampButton("📞 Answer", onClick = { answered = true }, color = Noir.Good)
        } else if (finished) {
            LampButton("On my way 🚓", onClick = vm::onMyWay)
        } else {
            Text("Tap the message to skip", color = Noir.Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}
