package com.alibi.game.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel

@Composable
fun ResultScreen(vm: GameViewModel) {
    val summary = vm.summary ?: return
    val card = vm.scoreCard
    val context = LocalContext.current
    ScreenColumn {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(summary.rankEmoji, fontSize = 64.sp)
            Text(summary.rankTitle, fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("${summary.total} / 100", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vm.streak > 0) Text("🔥 ${vm.streak}-day streak", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
        }

        // The breakdown is only known right after playing; a reopened app just shows the share card.
        if (card != null) {
            Panel {
                ScoreRow("🧩 Clues found", card.boardPoints, 40)
                ScoreRow("🔎 Culprit caught", card.catchPoints, 50)
                ScoreRow("📏 Bonus guess", card.bonusPoints, 10)
            }
        }

        Panel {
            Text("Your share card", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(summary.shareText, lineHeight = 22.sp)
        }

        Button(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, summary.shareText + "\n\nCan you crack today's case? 🕵️")
                }
                context.startActivity(Intent.createChooser(send, "Share your result"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share with friends", Modifier.padding(vertical = 6.dp)) }

        OutlinedButton(onClick = vm::backHome, modifier = Modifier.fillMaxWidth()) { Text("Back to the case file") }

        Text(
            "Case #${vm.nextCaseNumber} opens tomorrow. Don't break the streak.",
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ScoreRow(label: String, points: Int, outOf: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$points / $outOf", fontWeight = FontWeight.Bold)
    }
}
