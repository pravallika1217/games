package com.alibi.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alibi.engine.cases.Person
import com.alibi.engine.cases.Task
import com.alibi.game.Feedback
import com.alibi.game.GameViewModel
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- People

/** Everyone who was there. The killer is one of them; the clues will rule people out. */
@Composable
fun PeopleScreen(vm: GameViewModel) {
    val people = vm.file.people
    CaseScreen(step = 2, part = 0f, onClose = vm::goHome, bottom = {
        BigButton("Follow the clues", onClick = vm::startLeads)
    }) {
        Title("Who was there?", "These ${people.size} people were around tonight. The culprit is one of them.")
        people.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { PersonTile(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PersonTile(p: Person, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Noir.Panel)
            .border(1.dp, Noir.Line, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(p.emoji, fontSize = 30.sp)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(p.name, color = Noir.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp)
            GroupBadge(p.group)
            p.traits.forEach { Text("${it.emoji} ${it.text}", color = Noir.Dim, fontSize = 12.sp, lineHeight = 15.sp) }
        }
    }
}

@Composable
private fun GroupBadge(group: String, onPaper: Boolean = false) {
    Text(
        group.uppercase(),
        color = if (onPaper) Noir.Paper else Noir.AmberInk,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (onPaper) Noir.Ink else Noir.Amber).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

// ---------------------------------------------------------------- Leads

/**
 * One lead at a time. Think about what you already found, pick an answer, and the right one
 * reveals the next clue. The faces at the top get crossed out as the clues rule people out.
 */
@Composable
fun LeadsScreen(vm: GameViewModel) {
    val file = vm.file
    val inv = vm.investigation
    val current = inv.current

    // After the right answer, let the player read the reveal line, then show the clue.
    LaunchedEffect(current, inv.clueOpen) {
        if (inv.clueOpen && vm.cardLead == null) {
            delay(1100)
            vm.openClueCard()
        }
    }

    CaseScreen(step = 2, part = (inv.done + 1f) / (file.leads.size + 1f), onClose = vm::goHome, bottom = {
        if (current == null) BigButton("Question the suspects", onClick = vm::startQuestioning)
        else Text("❌ Mistakes: ${inv.mistakes}", color = Noir.Dim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }) {
        Lineup(vm)
        if (current != null) {
            val lead = file.leads[current]
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = -0.6f }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Noir.PaperEdge)
                    .padding(bottom = 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Noir.Paper)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row {
                    Text("🔎 LEAD ${current + 1} OF ${file.leads.size}", color = Noir.Ink.copy(alpha = 0.65f), fontFamily = Noir.Typewriter, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Text(file.leads.indices.joinToString(" ") { if (it < inv.done) file.leads[it].emoji else "?" }, color = Noir.Ink, fontSize = 12.sp)
                }
                Text("From: ${lead.from}", color = Color(0xFF6B4A12), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("🤔 ${lead.question}", color = Noir.Ink, fontFamily = Noir.Typewriter, fontSize = 16.sp, lineHeight = 22.sp)
                lead.options.indices.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { i ->
                            val wrong = i in inv.wrongOptions
                            val right = inv.clueOpen && i == lead.answer
                            Text(
                                lead.options[i],
                                color = Noir.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = if (wrong) 0.55f else 1f }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(when { right -> Color(0xFFDFF3E5); wrong -> Color(0xFFF8DCD9); else -> Color(0xFFFFFAF0) })
                                    .border(2.dp, when { right -> Color(0xFF2C8A52); wrong -> Noir.String; else -> Noir.Ink.copy(alpha = 0.25f) }, RoundedCornerShape(10.dp))
                                    .clickable(enabled = !inv.clueOpen && !wrong) { vm.answerLead(i) }
                                    .padding(horizontal = 8.dp, vertical = 11.dp),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                when {
                    inv.clueOpen -> Text("✓ ${lead.reveal}", color = Color(0xFF1F5F39), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    vm.lastWrongOption != null -> Text("✕ ${lead.wrongWhy[vm.lastWrongOption!!]}", color = Color(0xFFB2382F), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            Text("Think it through. A wrong guess costs you a point.", color = Noir.Dim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                "🎯 You've narrowed it down to ${inv.stillIn.size} suspects. Time to question them.",
                color = Noir.Text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Noir.GreenBg)
                    .border(1.dp, Noir.Green, RoundedCornerShape(14.dp)).padding(14.dp),
            )
            file.leads.forEach { EvidenceCard(it.emoji, it.card) }
        }
    }

    vm.cardLead?.let { ClueCard(vm, it) }
}

/** Everyone's face in a row. Crossed out faces get a red ✕; marked ones glow amber. */
@Composable
private fun Lineup(vm: GameViewModel) {
    val inv = vm.investigation
    val people = vm.file.people
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text("People at the scene", color = Noir.Dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("${inv.stillIn.size} of ${people.size} left", color = Noir.Dim, fontSize = 13.sp)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        people.forEach { p ->
            val out = p.id in inv.crossedOut
            val marked = p.id in inv.marked
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (marked) Color(0xFF2A2416) else Noir.Panel)
                    .border(2.dp, if (marked) Noir.Amber else Noir.Line, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(p.emoji, fontSize = 20.sp, modifier = Modifier.graphicsLayer { alpha = if (out) 0.3f else 1f })
                if (out) Text("✕", color = Noir.Red, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun EvidenceCard(emoji: String, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Noir.PaperEdge)
            .padding(bottom = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Noir.Paper)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, fontSize = 24.sp)
        Column {
            Text("EVIDENCE", color = Noir.Ink.copy(alpha = 0.6f), fontFamily = Noir.Typewriter, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(text, color = Noir.Ink, fontSize = 14.sp)
        }
    }
}

/** "CLUE FOUND": what it is, then cross out (or tap) the people it points to, and Check. */
@Composable
private fun ClueCard(vm: GameViewModel, index: Int) {
    val lead = vm.file.leads[index]
    val cross = lead.task == Task.CROSS
    val solved = vm.pickFeedback == Feedback.RIGHT
    Dialog(onDismissRequest = {}) {
        Column(
            Modifier
                .graphicsLayer { rotationZ = -1f }
                .clip(RoundedCornerShape(8.dp))
                .background(Noir.Paper)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("CLUE FOUND", color = Color(0xFFB2382F), fontFamily = Noir.Typewriter, fontSize = 12.sp, letterSpacing = 2.sp)
            Text(lead.emoji, fontSize = 56.sp)
            Text(lead.name, color = Noir.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(bold(lead.found), color = Noir.Ink, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Noir.Ink.copy(alpha = 0.2f)))
            Text("${if (cross) "❌" else "👆"} ${lead.ask}", color = Noir.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            vm.cardPeople.forEach { p ->
                val picked = p.id in vm.picks
                val (bg, edge) = when {
                    picked && cross -> Color(0xFFF8DCD9) to Noir.String
                    picked -> Color(0xFFFBEFD2) to Noir.AmberDark
                    else -> Color(0xFFFFFAF0) to Noir.Ink.copy(alpha = 0.2f)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(2.dp, edge, RoundedCornerShape(10.dp))
                        .clickable(enabled = !solved) { vm.togglePick(p.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(p.emoji, fontSize = 28.sp, modifier = Modifier.graphicsLayer { alpha = if (picked && cross) 0.4f else 1f })
                        if (picked && cross) Text("✕", color = Noir.String, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(p.name, color = Noir.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            GroupBadge(p.group, onPaper = true)
                        }
                        Text(p.traits.joinToString(" · ") { "${it.emoji} ${it.text}" }, color = Noir.Ink.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                }
            }
            when (vm.pickFeedback) {
                Feedback.RIGHT -> {
                    Text(
                        "✓ ${lead.why}" + if (cross) "\n👥 ${vm.investigation.stillIn.size} people left." else "",
                        color = Color(0xFF1F5F39),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BigButton("Put it in the evidence bag", onClick = vm::bagClue)
                }
                Feedback.WRONG -> {
                    Text("Not quite. Read each person's card again.", color = Color(0xFFB2382F), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                    BigButton("Check again", onClick = vm::checkPicks, enabled = vm.picks.isNotEmpty())
                }
                null -> BigButton("Check", onClick = vm::checkPicks, enabled = vm.picks.isNotEmpty())
            }
        }
    }
}
