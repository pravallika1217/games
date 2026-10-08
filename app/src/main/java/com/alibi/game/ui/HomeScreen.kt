package com.alibi.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel

@Composable
fun HomeScreen(vm: GameViewModel) {
    val case = vm.case
    ScreenColumn {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🕵️", fontSize = 56.sp)
            Text("ALIBI", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp, color = MaterialTheme.colorScheme.primary)
            Text("One mystery a day. Can you catch the liar?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Panel {
            Text("CASE #${case.number}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
            Text(case.file.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(case.file.intro, lineHeight = 21.sp)
        }
        Panel {
            Text("How to play", fontWeight = FontWeight.Bold)
            Text("🧩  Find word groups. Each one unlocks a clue.")
            Text("🔎  Read the alibis. Catch the one who's lying.")
            Text("🏆  Share your detective rank with friends!")
        }
        if (vm.streak > 0) {
            Text("🔥 ${vm.streak}-day streak", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
        Button(onClick = vm::openCase, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(if (vm.summary != null) "See today's result" else "Open the case file", Modifier.padding(vertical = 6.dp), fontSize = 16.sp)
        }
    }
}
