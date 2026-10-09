package com.alibi.game.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    BigButton("See who was there", onClick = vm::meetPeople, tone = Tone.GREEN)
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

/** Turns "a **key** word" into text with the key word in bold. */
fun bold(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
