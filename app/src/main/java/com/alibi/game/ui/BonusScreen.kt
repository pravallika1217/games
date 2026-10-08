package com.alibi.game.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import java.text.NumberFormat
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun BonusScreen(vm: GameViewModel) {
    val bonus = vm.case.file.bonus
    val outcome = vm.bonusOutcome
    val numbers = NumberFormat.getNumberInstance()

    // The slider moves on a log scale, so small and huge numbers are both easy to reach.
    var position by rememberSaveable { mutableFloatStateOf(0.5f) }
    val guess = niceRound(exp(ln(bonus.min) + position * (ln(bonus.max) - ln(bonus.min))))

    ScreenColumn {
        StepHeader("Bonus", "Make a smart guess 📏", "Nobody knows the exact answer. Think it through and slide to your guess!")

        Panel {
            Text(bonus.prompt, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "${numbers.format(outcome?.guess ?: guess)} ${bonus.unit}",
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(value = position, onValueChange = { position = it }, enabled = outcome == null)
        Row(Modifier.fillMaxWidth()) {
            Text(numbers.format(bonus.min), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(numbers.format(bonus.max), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }

        if (outcome == null) {
            Button(onClick = { vm.submitBonus(guess) }, modifier = Modifier.fillMaxWidth()) {
                Text("Lock it in! 🔒", Modifier.padding(vertical = 6.dp))
            }
        } else {
            Panel {
                Text("${outcome.verdict.emoji} ${outcome.verdict.label}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("The answer is about ${numbers.format(outcome.answer)} ${bonus.unit}.")
                Text("💡 ${bonus.explanation}", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
            Button(onClick = vm::finishCase, modifier = Modifier.fillMaxWidth()) {
                Text("See my detective rank 🏆", Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

/** Rounds to 2 significant digits, so the slider shows 85, 1,200 or 240,000 instead of 84.37. */
private fun niceRound(value: Double): Double {
    val magnitude = 10.0.pow(floor(log10(value)) - 1).coerceAtLeast(1.0)
    return (value / magnitude).roundToLong() * magnitude
}
