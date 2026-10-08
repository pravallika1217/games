package com.alibi.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.engine.cases.CaseTheme
import com.alibi.engine.logic.Category
import com.alibi.engine.logic.DeductionNotes
import com.alibi.engine.logic.Item
import com.alibi.engine.logic.Mark
import com.alibi.game.GameViewModel

private val CATEGORY_LABELS = mapOf(
    Category.SUSPECT to "Who",
    Category.WEAPON to "With what",
    Category.ROOM to "Where",
)

@Composable
fun DeductionScreen(vm: GameViewModel) {
    val case = vm.case
    val theme = case.theme
    ScreenColumn {
        ActHeader("Act 2 of 3", "The Deduction", "Each suspect had one item and was in one place. Tap the grid: ✕ rules out, ✓ confirms.")

        Panel {
            Text("🩸 The crime", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(case.finalClue, fontWeight = FontWeight.SemiBold)
        }

        Panel {
            Text("🗂️ Witness statements", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            case.clues.forEachIndexed { i, clue -> Text("${i + 1}. $clue", lineHeight = 20.sp) }
            Text("Unlocked on the board", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            case.bonusClues.forEachIndexed { i, clue ->
                if (vm.bonusUnlocked[i]) Text("🔓 $clue", lineHeight = 20.sp)
                else Text("🔒 Locked", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Panel {
            Text("📇 Cast", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Category.entries.forEach { category ->
                Text(theme.entities(category).joinToString("   ") { it.label }, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        NotesGrid("Who had what", theme, Category.SUSPECT, Category.WEAPON, vm.notes, vm::cycleMark)
        NotesGrid("Who was where", theme, Category.SUSPECT, Category.ROOM, vm.notes, vm::cycleMark)
        NotesGrid("What was where", theme, Category.WEAPON, Category.ROOM, vm.notes, vm::cycleMark)

        Panel {
            Text("⚖️ Your accusation", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Category.entries.forEach { category ->
                Text(CATEGORY_LABELS.getValue(category), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    theme.entities(category).forEachIndexed { index, entity ->
                        Choice(entity.label, selected = vm.accused[category] == index) { vm.pick(category, index) }
                    }
                }
            }
        }

        val result = vm.accusation
        if (result == null) {
            Button(onClick = vm::accuse, enabled = vm.canAccuse, modifier = Modifier.fillMaxWidth()) {
                Text("Make the accusation", Modifier.padding(vertical = 6.dp))
            }
        } else {
            Panel {
                Text(
                    if (result.solved) "🎉 Case closed! You caught them." else "😬 Not quite. Here's what really happened:",
                    fontWeight = FontWeight.Bold,
                )
                Verdict("Who", result.who, theme.suspects[result.culprit].label)
                Verdict("With what", result.what, theme.weapons[result.weapon].label)
                Verdict("Where", result.where, theme.rooms[result.room].label)
            }
            Button(onClick = vm::goToFermi, modifier = Modifier.fillMaxWidth()) {
                Text("Act 3: The Final Call →", Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun NotesGrid(
    title: String,
    theme: CaseTheme,
    rows: Category,
    columns: Category,
    notes: DeductionNotes,
    onCycle: (Item, Item) -> Unit,
) {
    val cell = 46.dp
    Panel {
        Text(title, fontWeight = FontWeight.Bold)
        Row {
            Box(Modifier.size(cell))
            theme.entities(columns).forEach { Box(Modifier.size(cell), Alignment.Center) { Text(it.emoji, fontSize = 22.sp) } }
        }
        theme.entities(rows).forEachIndexed { r, rowEntity ->
            Row {
                Box(Modifier.size(cell), Alignment.Center) { Text(rowEntity.emoji, fontSize = 22.sp) }
                theme.entities(columns).indices.forEach { c ->
                    val a = Item(rows, r)
                    val b = Item(columns, c)
                    val mark = notes[a, b]
                    Box(
                        Modifier
                            .size(cell)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (mark) {
                                    Mark.CHECK -> AlibiColors.Good.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { onCycle(a, b) },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (mark) {
                            Mark.CROSS -> Text("✕", color = AlibiColors.Bad, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Mark.CHECK -> Text("✓", color = AlibiColors.Good, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Mark.EMPTY -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@Composable
private fun Verdict(label: String, correct: Boolean, truth: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${if (correct) "✅" else "❌"} $label")
        Column(horizontalAlignment = Alignment.End) { Text(truth, fontWeight = FontWeight.SemiBold) }
    }
}
