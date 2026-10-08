package com.alibi.engine.score

import com.alibi.engine.board.BoardState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.Interrogation

/** Tomorrow's front page, starring the detective. */
data class FrontPage(
    val headline: String,
    val subhead: String,
    val photo: String,
    val caption: String,
    val columns: List<String>,
    val verdict: Verdict,
    val shareText: String,
)

object Newspaper {
    fun write(
        file: CaseFile,
        detective: String,
        minutes: Int,
        board: BoardState,
        hintsUsed: Int,
        room: Interrogation,
    ): FrontPage {
        val verdict = Scoring.verdict(board, hintsUsed, room)
        val culprit = file.suspects[file.culprit]
        val mins = minutes.coerceAtLeast(1)
        val minWord = if (mins == 1) "minute" else "minutes"
        val found = board.solvedOrder.size
        return if (room.caught) {
            FrontPage(
                headline = "Inspector $detective cracks ${file.headlineSubject} in $mins $minWord!",
                subhead = "\"${file.caughtQuote},\" says the city's newest top cop.",
                photo = "${culprit.emoji}⛓️",
                caption = "${culprit.name} being taken away by police.",
                columns = listOf(
                    "${file.crime} Inspector $detective arrived at midnight and searched the scene with only a torch.",
                    "$found of ${board.groups.size} witness statements were connected on the evidence wall. " +
                        (if (room.released.isEmpty()) "The" else "After a wrong arrest, the") +
                        " Inspector caught ${culprit.name}, whose alibi fell apart under the interrogation lamp.",
                ),
                verdict = verdict,
                shareText = shareText(file, detective, mins, board, room, verdict),
            )
        } else {
            FrontPage(
                headline = "Culprit escapes! Inspector $detective vows to return",
                subhead = "Police make a last-minute arrest.",
                photo = "🚨${culprit.emoji}",
                caption = "${culprit.name}, caught two days later.",
                columns = listOf(
                    "${file.crime} Inspector $detective questioned three suspects through the night.",
                    "Two innocent people were arrested by mistake while ${culprit.name} slipped away. " +
                        "\"Tomorrow is a new case,\" the Inspector told reporters.",
                ),
                verdict = verdict,
                shareText = shareText(file, detective, mins, board, room, verdict),
            )
        }
    }

    /** Spoiler-free: it never names the culprit. */
    private fun shareText(
        file: CaseFile,
        detective: String,
        minutes: Int,
        board: BoardState,
        room: Interrogation,
        verdict: Verdict,
    ): String {
        val arrest = when {
            !room.caught -> "Escaped"
            room.released.isEmpty() -> "Caught on 1st try"
            else -> "Caught on 2nd try"
        }
        return listOf(
            "📰 THE DAILY DETECTIVE",
            if (room.caught) "Inspector $detective cracked \"${file.title}\" in $minutes min! ${"⭐".repeat(verdict.stars)}"
            else "The culprit escaped Inspector $detective in \"${file.title}\" 😱",
            "🧶 Evidence ${board.solvedOrder.size}/${board.groups.size} · ☕ Mistakes ${board.mistakes} · 🚔 $arrest",
            "Can you crack today's case? 🕵️ #ALIBI",
        ).joinToString("\n")
    }
}
