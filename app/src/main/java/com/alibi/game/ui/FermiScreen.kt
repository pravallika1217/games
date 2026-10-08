package com.alibi.game.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import java.text.NumberFormat

@Composable
fun FermiScreen(vm: GameViewModel) {
    val fermi = vm.case.file.fermi
    val outcome = vm.fermiOutcome
    val numbers = NumberFormat.getNumberInstance()
    ScreenColumn {
        ActHeader("Act 3 of 3", "The Final Call", "Nobody knows the exact answer. Think it through step by step and make a smart guess.")

        Panel {
            Text("📏 Estimate", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(fermi.prompt, fontSize = 17.sp, lineHeight = 24.sp)
        }

        OutlinedTextField(
            value = vm.fermiInput,
            onValueChange = vm::updateFermiInput,
            label = { Text("Your guess") },
            suffix = { Text(fermi.unit) },
            singleLine = true,
            enabled = outcome == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.submitFermi() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (outcome == null) {
            Button(
                onClick = vm::submitFermi,
                enabled = (vm.fermiInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lock in my guess", Modifier.padding(vertical = 6.dp)) }
        } else {
            Panel {
                Text("${outcome.verdict.emoji} ${outcome.verdict.label}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("You said ${numbers.format(outcome.guess)}. The answer is about ${numbers.format(outcome.answer)} ${fermi.unit}.")
                Text("How to work it out: ${fermi.explanation}", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                Text("+${outcome.score} points", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Button(onClick = vm::finishCase, modifier = Modifier.fillMaxWidth()) {
                Text("See my detective rank →", Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
