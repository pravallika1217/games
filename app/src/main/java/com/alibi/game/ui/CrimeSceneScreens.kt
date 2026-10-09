package com.alibi.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alibi.engine.cases.Suspect
import com.alibi.game.Feedback
import com.alibi.game.GameViewModel

// ---------------------------------------------------------------- Arrival

/** Police tape and the constable's 4-line briefing at the door. */
@Composable
fun ArrivalScreen(vm: GameViewModel) {
    CaseScreen(step = 0, part = 1f, onClose = vm::goHome, bottom = {
        BigButton(vm.file.examination.title, onClick = vm::examine)
    }) {
        Title("You've arrived", vm.file.arrival)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .graphicsLayer { rotationZ = -2f; scaleX = 1.1f }
                .background(Noir.Tape)
                .drawBehind {
                    val stripe = 26.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(Color(0xFF1B1B1B), Offset(x, size.height), Offset(x + size.height, 0f), stripe / 2)
                        x += stripe * 2
                    }
                }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "POLICE LINE · DO NOT CROSS",
                color = Color(0xFF111111),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                fontSize = 13.sp,
                modifier = Modifier.background(Noir.Tape).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noir.Panel).border(1.dp, Noir.Line, RoundedCornerShape(16.dp))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(Noir.Panel2), contentAlignment = Alignment.Center) { Text("👮", fontSize = 24.sp) }
                Column {
                    Text("Constable Pandu", color = Noir.Text, fontWeight = FontWeight.Bold)
                    Text("\"Good evening, Inspector ${vm.name}. Here's what we know.\"", color = Noir.Dim, fontSize = 13.sp)
                }
            }
            vm.file.briefing.forEach { line ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.Line))
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(line.emoji, modifier = Modifier.width(34.dp))
                    Text(line.label, color = Noir.Dim, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Text(line.value, color = if (line.unknown) Noir.Amber else Noir.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Examination

/** Check the 3 marked spots, then answer one question about what happened. */
@Composable
fun ExamineScreen(vm: GameViewModel) {
    val exam = vm.file.examination
    val allSeen = vm.allSpotsSeen
    val ping = rememberInfiniteTransition(label = "ping")
    val ring by ping.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "ring")

    CaseScreen(
        step = 1,
        part = vm.seenSpots.size / 4f,
        onClose = vm::goHome,
        feedback = vm.examFeedback,
        bottom = {
            when {
                !allSeen -> BigButton("${vm.seenSpots.size} of ${exam.spots.size} checked", onClick = {}, enabled = false)
                vm.examFeedback == Feedback.RIGHT -> {
                    FeedbackLine(Feedback.RIGHT, exam.why)
                    BigButton("Search the ${vm.file.place}", onClick = vm::startSearch, tone = Tone.GREEN)
                }
                vm.examFeedback == Feedback.WRONG -> {
                    FeedbackLine(Feedback.WRONG, "Look at what you found again.")
                    BigButton("Try again", onClick = vm::retryExam, tone = Tone.RED)
                }
                else -> BigButton("Check", onClick = vm::checkExam, enabled = vm.examPick != null)
            }
        },
    ) {
        Title(exam.title, if (allSeen) "Now think. What happened?" else "Tap the 3 marked spots.")
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF5A4836), Color(0xFF33281E))))
        ) {
            val w = maxWidth
            val h = maxHeight
            if (exam.objectEmoji == null) {
                // Chalk outline of the body.
                Canvas(Modifier.matchParentSize().padding(horizontal = w * 0.14f, vertical = h * 0.08f)) {
                    val sx = size.width / 200f
                    val sy = size.height / 260f
                    val chalk = Color(0xD9FFFFFF)
                    val stroke = 8.dp.toPx()
                    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                        drawLine(chalk, Offset(x1 * sx, y1 * sy), Offset(x2 * sx, y2 * sy), stroke, StrokeCap.Round)
                    drawCircle(chalk, radius = 22f * sx, center = Offset(100f * sx, 42f * sy), style = Stroke(stroke))
                    line(100f, 64f, 100f, 150f)
                    line(100f, 82f, 52f, 120f)
                    line(100f, 82f, 150f, 104f)
                    line(100f, 150f, 68f, 228f)
                    line(100f, 150f, 136f, 222f)
                }
            } else {
                Text(exam.objectEmoji!!, fontSize = 150.sp, modifier = Modifier.align(Alignment.Center))
            }
            exam.spots.forEachIndexed { i, spot ->
                val seen = i in vm.seenSpots
                Box(
                    Modifier
                        .offset(x = w * spot.x - 26.dp, y = h * spot.y - 26.dp)
                        .size(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!seen) Box(
                        Modifier.size(52.dp).graphicsLayer { scaleX = 1f + ring * 0.6f; scaleY = 1f + ring * 0.6f; alpha = 1f - ring }
                            .border(3.dp, Noir.Amber, RoundedCornerShape(50))
                    )
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (seen) Noir.Panel2 else Noir.Amber)
                            .border(2.dp, if (seen) Noir.Green else Noir.Amber, RoundedCornerShape(50))
                            .clickable { vm.checkSpot(i) },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (seen) "✓" else "?", color = if (seen) Noir.Text else Color(0xFF111111), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
                }
            }
        }
        exam.spots.forEachIndexed { i, spot ->
            if (i in vm.seenSpots) {
                Text(
                    buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(spot.title + ": ") }; append(spot.text) },
                    color = Noir.Text,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Noir.Panel)
                        .drawBehind { drawRect(Noir.Amber, size = size.copy(width = 3.dp.toPx())) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
        if (allSeen) {
            Text(exam.question, color = Noir.Text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            exam.options.forEachIndexed { i, option ->
                val state = when {
                    i in vm.examRuledOut -> OptionState.RULED_OUT
                    vm.examPick == i && vm.examFeedback == Feedback.RIGHT -> OptionState.RIGHT
                    vm.examPick == i && vm.examFeedback == Feedback.WRONG -> OptionState.WRONG
                    vm.examPick == i -> OptionState.PICKED
                    else -> OptionState.NORMAL
                }
                OptionRow("ABC"[i].toString(), option, state) { vm.pickExamOption(i) }
            }
        }
    }
}

// ---------------------------------------------------------------- Search

/**
 * Riddle search. Suspects stay pinned at the top with their facts. The note says where to look;
 * tap a place in the room to search it.
 */
@Composable
fun SearchScreen(vm: GameViewModel) {
    val file = vm.file
    val search = vm.search
    val current = search.current
    CaseScreen(step = 2, part = search.found.size / 3f, onClose = vm::goHome, bottom = {
        if (current == null) BigButton("Question the suspects", onClick = vm::startQuestioning)
        else BigButton("Find clue ${current + 1} of 3", onClick = {}, enabled = false)
    }) {
        SuspectStrip(file.suspects, search.strings, vm.newString)
        if (current != null) {
            PaperNote(
                "FORENSICS NOTE · CLUE ${current + 1} OF 3",
                file.clues[current].riddle,
                trailing = file.clues.indices.joinToString(" ") { if (it in search.found) file.clues[it].emoji else "?" },
            )
        } else {
            PaperNote("FORENSICS NOTE", "All 3 clues are in your evidence bag. Look at the red strings: who do they point to?")
        }

        // What the last search turned up. Above the room, so it never covers anything.
        val flash = remember { Animatable(0f) }
        LaunchedEffect(vm.searchMessageTick) {
            if (vm.searchMessageTick > 0) { flash.snapTo(1f); flash.animateTo(0f, tween(500)) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(lerpColor(Noir.Panel, Noir.RedBg, flash.value))
                .border(1.dp, lerpColor(Noir.Line, Noir.Red, flash.value), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    current == null -> "✅ All 3 clues found."
                    vm.searchMessage != null -> "🔍 ${vm.searchMessage} (${search.wrongSearches} wrong)"
                    else -> "Tap a place in the ${file.place} to search it."
                },
                color = Noir.Text,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            if (search.canAskForHint && vm.hinted == null) {
                Text("Stuck? Ask Pandu", color = Noir.Amber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = vm::askPandu).padding(6.dp))
            }
        }

        Room(vm)
    }

    vm.openClue?.let { ClueCard(vm, it) }
}

@Composable
private fun SuspectStrip(suspects: List<Suspect>, strings: List<Int>, glowing: Int?) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suspects.forEachIndexed { i, s ->
            val glow = remember { Animatable(0f) }
            LaunchedEffect(glowing, strings[i]) { if (glowing == i) { glow.snapTo(1f); glow.animateTo(0f, tween(1400)) } }
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer { rotationZ = listOf(-2f, 1.5f, -1f)[i % 3] }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Noir.PaperEdge)
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Noir.Paper)
                    .border(3.dp, Noir.String.copy(alpha = glow.value), RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.emoji, fontSize = 22.sp)
                Text(s.shortName, color = Noir.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${s.traits.joinToString(" ") { it.emoji }} · ${s.says}", color = Noir.Ink.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                Text("🧶".repeat(strings[i]), fontSize = 11.sp, modifier = Modifier.heightIn(min = 14.dp))
            }
        }
    }
}

@Composable
private fun Room(vm: GameViewModel) {
    val file = vm.file
    val search = vm.search
    val pulse = rememberInfiniteTransition(label = "hint")
    val glow by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(10f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(0f to Color(0xFF34403F), 0.36f to Color(0xFF34403F), 0.36f to Color(0xFF4A3A2C), 1f to Color(0xFF4A3A2C)))
    ) {
        val w = maxWidth
        val h = maxHeight
        Box(Modifier.offset(y = h * 0.29f).fillMaxWidth().height(h * 0.12f).background(Color(0xFF5F4D3B)))
        file.hideouts.forEach { place ->
            val searched = place.id in search.searched
            val hinted = place.id == vm.hinted
            PlaceButton(
                emoji = place.emoji,
                label = place.label,
                x = w * place.x,
                y = h * place.y,
                faded = searched,
                scale = if (hinted) glow else 1f,
                glow = hinted,
                onClick = { vm.searchAt(place.id) },
            )
        }
    }
}

@Composable
private fun PlaceButton(emoji: String, label: String, x: Dp, y: Dp, faded: Boolean, scale: Float, glow: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .offset(x = x - 40.dp, y = y - 28.dp)
            .width(80.dp)
            .graphicsLayer { alpha = if (faded) 0.35f else 1f }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            emoji,
            fontSize = 30.sp,
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(if (glow) Modifier.clip(RoundedCornerShape(50)).background(Noir.Amber.copy(alpha = 0.35f)) else Modifier),
        )
        Text(
            label,
            color = Color(0xFFE8E0CF),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x73000000)).padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** "CLUE FOUND" card: what it is, then "Who does this point to?" with the suspects as answers. */
@Composable
private fun ClueCard(vm: GameViewModel, index: Int) {
    val clue = vm.file.clues[index]
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
            Text(clue.emoji, fontSize = 56.sp)
            Text(clue.name, color = Noir.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(bold(clue.found), color = Noir.Ink, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Noir.Ink.copy(alpha = 0.2f)))
            Text("🤔 Who does this point to?", color = Noir.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            vm.file.suspects.forEachIndexed { i, s ->
                val right = vm.clueSolved && i == clue.pointsTo
                val wrong = i in vm.clueRuledOut
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(when { right -> Color(0xFFDFF3E5); wrong -> Color(0xFFF8DCD9); else -> Color(0xFFFFFAF0) })
                        .border(2.dp, when { right -> Color(0xFF2C8A52); wrong -> Noir.String; else -> Noir.Ink.copy(alpha = 0.2f) }, RoundedCornerShape(10.dp))
                        .clickable(enabled = !vm.clueSolved && !wrong) { vm.answerClue(i) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(s.emoji, fontSize = 30.sp)
                    Column {
                        Text(s.name, color = Noir.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        s.traits.forEach { Text("${it.emoji} ${it.text}", color = Noir.Ink.copy(alpha = 0.75f), fontSize = 12.sp) }
                    }
                }
            }
            when {
                vm.clueSolved -> {
                    Text(
                        "✓ ${clue.why}\n🧶 Red string tied to ${vm.file.suspects[clue.pointsTo].name}.",
                        color = Color(0xFF1F5F39),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BigButton("Put it in the evidence bag", onClick = vm::bagClue)
                }
                vm.clueRuledOut.isNotEmpty() -> Text(
                    "Not quite. Read their details again.",
                    color = Color(0xFFB2382F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Turns "a **key** word" into text with the key word in bold. */
fun bold(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
