package com.alibi.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alibi.game.Feedback
import com.alibi.game.GameViewModel
import com.alibi.game.Screen

@Composable
fun AlibiApp(vm: GameViewModel = viewModel()) {
    BackHandler(enabled = vm.screen !in setOf(Screen.Name, Screen.Home)) { vm.goHome() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Noir.Bg)
            .safeDrawingPadding()
    ) {
        when (vm.screen) {
            Screen.Name -> NameScreen(vm)
            Screen.Home -> HomeScreen(vm)
            Screen.Call -> CallScreen(vm)
            Screen.Arrival -> ArrivalScreen(vm)
            Screen.Examine -> ExamineScreen(vm)
            Screen.Search -> SearchScreen(vm)
            Screen.Questioning -> QuestioningScreen(vm)
            Screen.Vote -> VoteScreen(vm)
            Screen.Reveal -> RevealScreen(vm)
            Screen.Result -> ResultScreen(vm)
        }
    }
}

/** The 5 parts of a case, shown as a progress bar at the top. */
private val STEPS = listOf("Arrival", "Examine", "Search", "Questioning", "Arrest")

/**
 * Every case screen has the same shape: progress at the top, the scene in the middle,
 * and one main action at the bottom.
 */
@Composable
fun CaseScreen(
    step: Int?,
    part: Float = 0f,
    onClose: (() -> Unit)? = null,
    feedback: Feedback? = null,
    scroll: Boolean = true,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (step != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "✕",
                    color = Noir.Dim,
                    fontSize = 20.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClose?.invoke() }.padding(8.dp),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    STEPS.indices.forEach { i ->
                        val fill by animateFloatAsState(
                            when {
                                i < step -> 1f
                                i == step -> part.coerceIn(0.08f, 1f)
                                else -> 0f
                            },
                            tween(300),
                            label = "step$i",
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Noir.Panel2)
                                .drawBehind {
                                    drawRoundRect(Noir.Amber, size = size.copy(width = size.width * fill), cornerRadius = CornerRadius(3.dp.toPx()))
                                }
                        )
                    }
                }
                Text(STEPS[step], color = Noir.Dim, fontSize = 12.sp)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    when (feedback) {
                        Feedback.RIGHT -> Noir.GreenBg
                        Feedback.WRONG -> Noir.RedBg
                        null -> Noir.Bg
                    }
                )
                .drawBehind { if (feedback == null) drawLine(Noir.Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = bottom,
        )
    }
}

@Composable
fun Title(text: String, sub: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, color = Noir.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
        if (sub != null) Text(sub, color = Noir.Dim, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

enum class Tone { AMBER, GREEN, RED }

/** The big chunky main button, pressed-in look like Duolingo's. */
@Composable
fun BigButton(text: String, onClick: () -> Unit, enabled: Boolean = true, tone: Tone = Tone.AMBER) {
    val (face, edge, ink) = when (tone) {
        Tone.AMBER -> Triple(Noir.Amber, Noir.AmberDark, Noir.AmberInk)
        Tone.GREEN -> Triple(Noir.Green, Noir.GreenDark, Color(0xFF0B2414))
        Tone.RED -> Triple(Noir.Red, Noir.RedDark, Color.White)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) edge else Noir.Panel2)
            .padding(bottom = if (enabled) 4.dp else 0.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) face else Noir.Panel2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) ink else Noir.Dim, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Noir.Dim,
        fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(10.dp),
        textAlign = TextAlign.Center,
    )
}

/** An A / B / C answer, like a quiz app. */
@Composable
fun OptionRow(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val (border, bg) = when (state) {
        OptionState.NORMAL -> Noir.Line to Noir.Panel
        OptionState.PICKED -> Noir.Amber to Color(0xFF2A2416)
        OptionState.RIGHT -> Noir.Green to Noir.GreenBg
        OptionState.WRONG -> Noir.Red to Noir.RedBg
        OptionState.RULED_OUT -> Noir.Line to Noir.Panel
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(BorderStroke(2.dp, border), RoundedCornerShape(14.dp))
            .clickable(enabled = state != OptionState.RULED_OUT, onClick = onClick)
            .padding(14.dp)
            .then(if (state == OptionState.RULED_OUT) Modifier.background(Color.Transparent) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(28.dp).border(2.dp, if (state == OptionState.PICKED) Noir.Amber else Noir.Line, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(letter, color = if (state == OptionState.PICKED) Noir.Amber else Noir.Dim, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Text(text, color = if (state == OptionState.RULED_OUT) Noir.Dim.copy(alpha = 0.5f) else Noir.Text, fontSize = 16.sp)
    }
}

enum class OptionState { NORMAL, PICKED, RIGHT, WRONG, RULED_OUT }

/** The green "Correct!" or red "Not quite" line above the bottom button. */
@Composable
fun FeedbackLine(feedback: Feedback, text: String) {
    val color = if (feedback == Feedback.RIGHT) Noir.Green else Noir.Red
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(color), contentAlignment = Alignment.Center) {
            Text(if (feedback == Feedback.RIGHT) "✓" else "✕", color = if (feedback == Feedback.RIGHT) Color(0xFF0B2414) else Color.White, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(if (feedback == Feedback.RIGHT) "Correct!" else "Not quite", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text, color = Noir.Text, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

/** A typed note on case-file paper, taped at the top. */
@Composable
fun PaperNote(label: String, text: String, trailing: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Noir.PaperEdge)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Noir.Paper)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text(label, color = Noir.Ink.copy(alpha = 0.65f), fontFamily = Noir.Typewriter, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, color = Noir.Ink, fontSize = 12.sp)
        }
        Text(text, color = Noir.Ink, fontFamily = Noir.Typewriter, fontSize = 15.sp, lineHeight = 21.sp)
    }
}
