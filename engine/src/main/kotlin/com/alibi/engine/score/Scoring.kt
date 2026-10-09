package com.alibi.engine.score

import com.alibi.engine.cases.ArrestState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.Investigation

/** Today's result, like Wordle's: stars, time, mistakes and a spoiler-free share text. */
data class ResultCard(
    val stars: Int,
    val title: String,
    val seconds: Int,
    val mistakes: Int,
    val caught: Boolean,
    val shareText: String,
) {
    val time: String get() = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/**
 * Three stars to start. Lose one for 3 or more mistakes, and one for a wrong arrest.
 * If the culprit escapes, no stars.
 */
object Scoring {
    fun mistakes(examMistakes: Int, investigation: Investigation): Int = examMistakes + investigation.mistakes

    fun result(
        caseNumber: Int,
        file: CaseFile,
        seconds: Int,
        examMistakes: Int,
        investigation: Investigation,
        arrest: ArrestState,
    ): ResultCard {
        val mistakes = mistakes(examMistakes, investigation)
        val stars = when {
            !arrest.caught -> 0
            else -> 3 - (if (mistakes >= 3) 1 else 0) - (if (arrest.released.isNotEmpty()) 1 else 0)
        }
        val title = when {
            !arrest.caught -> "The culprit got away"
            stars == 3 -> "Sherlock"
            stars == 2 -> "Sharp Inspector"
            else -> "Constable on Duty"
        }
        val card = ResultCard(stars, title, seconds.coerceAtLeast(1), mistakes, arrest.caught, "")
        val tries = when {
            !arrest.caught -> "got away"
            arrest.released.isEmpty() -> "1st try"
            else -> "2nd try"
        }
        val share = listOf(
            "ALIBI #$caseNumber 🕵️ ${"⭐".repeat(stars)}${"▫️".repeat(3 - stars)}",
            "⏱ ${card.time} · ❌ $mistakes · 🚔 $tries",
            "Can you crack \"${file.title}\"?",
        ).joinToString("\n")
        return card.copy(shareText = share)
    }
}

/** The detective's career, based on how many cases they've solved. */
enum class CareerRank(val title: String, val casesNeeded: Int) {
    ROOKIE("Rookie", 0),
    SUB_INSPECTOR("Sub-Inspector", 1),
    INSPECTOR("Inspector", 6),
    ACP("ACP", 15),
    COMMISSIONER("Commissioner", 30);

    val next: CareerRank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forSolved(solved: Int): CareerRank = entries.last { solved >= it.casesNeeded }
    }
}
