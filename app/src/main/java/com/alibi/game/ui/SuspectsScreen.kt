package com.alibi.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.engine.cases.Suspect
import com.alibi.game.GameViewModel

@Composable
fun SuspectsScreen(vm: GameViewModel) {
    val file = vm.case.file
    val catch = vm.catch
    ScreenColumn {
        StepHeader("Step 2 of 2", "Who did it? 🔎", "Everyone has an alibi. One of them is lying. Use your clues to find out who!")

        Panel {
            Text("🩸 What happened", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(file.crime, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
        }

        Panel {
            Text("📜 Your clues", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            file.clues.forEachIndexed { i, clue ->
                if (vm.isClueUnlocked(i)) Text(clue, lineHeight = 21.sp)
                else Text("🔒 Missed clue", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!catch.isOver) {
            Text(
                "Tip: tap a suspect to stamp them INNOCENT. Then accuse the one who's left!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }

        file.suspects.forEachIndexed { index, suspect ->
            SuspectCard(
                suspect = suspect,
                innocent = index in catch.cleared,
                wronglyAccused = index in catch.wrongGuesses,
                isCulprit = catch.isOver && index == file.culprit,
                active = !catch.isOver,
                highlighted = !catch.isOver && catch.lastOneStanding == index,
                onTap = { vm.toggleInnocent(index) },
                onAccuse = { vm.accuse(index) },
            )
        }

        if (!catch.isOver && catch.wrongGuesses.isNotEmpty()) {
            val wrong = file.suspects[catch.wrongGuesses.last()]
            Text(
                "😮 ${wrong.name} has a solid alibi! You have ${catch.triesLeft} try left.",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (catch.isOver) {
            val culprit = file.suspects[file.culprit]
            Panel {
                Text(
                    if (catch.caught) "🎉 CASE CLOSED! You caught ${culprit.name}!" else "🏃 They got away! It was ${culprit.name}.",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (catch.caught) AlibiColors.Good else AlibiColors.Bad,
                )
                Text("💬 The confession", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(file.confession, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
            }
            Button(onClick = vm::goToBonus, modifier = Modifier.fillMaxWidth()) {
                Text("Bonus question 📏 (+10 points)", Modifier.padding(vertical = 6.dp))
            }
            OutlinedButton(onClick = vm::finishCase, modifier = Modifier.fillMaxWidth()) { Text("Skip, show my result") }
        }
    }
}

@Composable
private fun SuspectCard(
    suspect: Suspect,
    innocent: Boolean,
    wronglyAccused: Boolean,
    isCulprit: Boolean,
    active: Boolean,
    highlighted: Boolean,
    onTap: () -> Unit,
    onAccuse: () -> Unit,
) {
    val borderColor = when {
        isCulprit -> AlibiColors.Bad
        highlighted -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = active && !wronglyAccused, onClick = onTap)
    ) {
        Column(
            Modifier
                .padding(14.dp)
                .alpha(if (innocent) 0.45f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(suspect.emoji, fontSize = 40.sp)
                Text(suspect.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            // The alibi, as a speech bubble.
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("\"${suspect.alibi}\"", fontStyle = FontStyle.Italic, lineHeight = 20.sp)
            }
            if (active && !innocent) {
                Button(
                    onClick = onAccuse,
                    colors = ButtonDefaults.buttonColors(containerColor = AlibiColors.Bad, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("You did it! 👉") }
            }
        }

        val stamp = when {
            isCulprit -> "GUILTY"
            innocent -> "INNOCENT"
            else -> null
        }
        if (stamp != null) {
            val color = if (isCulprit) AlibiColors.Bad else AlibiColors.Good
            Text(
                stamp,
                Modifier
                    .align(Alignment.Center)
                    .rotate(-14f)
                    .border(3.dp, color, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = 4.sp,
            )
        }
    }
}
