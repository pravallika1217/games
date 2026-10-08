package com.alibi.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.engine.board.EvidenceGroup
import com.alibi.game.GameViewModel

@Composable
fun WordsScreen(vm: GameViewModel) {
    val board = vm.board
    ScreenColumn {
        StepHeader("Step 1 of 2", "Find the clues 🧩", "Tap 4 words that belong together. Each group you find unlocks a clue!")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                "Clues found: " + board.groups.indices.joinToString(" ") { if (vm.isClueUnlocked(it)) "🔓" else "🔒" },
                fontWeight = FontWeight.SemiBold,
            )
        }

        board.solvedOrder.forEach { index -> GroupBanner(board.groups[index], found = true) }
        if (board.isLost) {
            board.groups.indices.filterNot { it in board.solvedOrder }.forEach { index ->
                GroupBanner(board.groups[index], found = false)
            }
        }

        if (!board.isOver) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                board.tiles.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tile ->
                            Tile(
                                text = tile,
                                selected = tile in vm.selection,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.toggleTile(tile) },
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tries left: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                repeat(board.maxMistakes) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (i < board.mistakesLeft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        vm.boardMessage?.let {
            Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        }

        if (!board.isOver) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::shuffle, modifier = Modifier.weight(1f)) { Text("Shuffle") }
                OutlinedButton(onClick = vm::deselectAll, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = vm::submitGuess, enabled = vm.selection.size == 4, modifier = Modifier.weight(1f)) {
                    Text("Submit")
                }
            }
        } else {
            Button(onClick = vm::goToSuspects, modifier = Modifier.fillMaxWidth()) {
                Text("Meet the suspects 🕵️", Modifier.padding(vertical = 6.dp))
            }
        }
    }

    vm.newClue?.let { clue ->
        AlertDialog(
            onDismissRequest = vm::dismissClue,
            confirmButton = { TextButton(onClick = vm::dismissClue) { Text("Got it!") } },
            title = { Text("📜 New clue!") },
            text = { Text(clue, fontSize = 17.sp, lineHeight = 24.sp) },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun Tile(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = if (text.length > 8) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp,
        )
    }
}

/** A group the player found, or (faded) one revealed after running out of tries. */
@Composable
private fun GroupBanner(group: EvidenceGroup, found: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AlibiColors.Levels[group.level].copy(alpha = if (found) 1f else 0.45f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(group.title.uppercase(), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(group.items.joinToString(", "), color = Color.Black, fontSize = 13.sp)
        Text(if (found) "🔓 Clue unlocked" else "🔒 Clue missed", color = Color.Black.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}
