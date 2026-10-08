package com.alibi.engine.score

import com.alibi.engine.board.BoardState
import com.alibi.engine.cases.AccusationResult
import com.alibi.engine.fermi.FermiOutcome
import kotlin.math.roundToInt

enum class Rank(val title: String, val emoji: String, val minScore: Int) {
    SHERLOCK("Sherlock", "🥇", 90),
    INSPECTOR("Inspector", "🥈", 70),
    CONSTABLE("Constable", "🥉", 45),
    SUSPECT("Suspect", "🫠", 0);

    companion object {
        fun forScore(score: Int): Rank = entries.first { score >= it.minScore }
    }
}

data class ScoreCard(
    val boardPoints: Int,
    val logicPoints: Int,
    val fermiPoints: Int,
    val total: Int,
    val rank: Rank,
    val shareText: String,
)

/**
 * Combines the three acts. The deduction weighs most, since it's the heart of the case;
 * the board and the Fermi guess make sure everyone still walks away with something.
 */
object Scoring {
    private val LEVEL_SQUARES = listOf("🟨", "🟩", "🟦", "🟪")

    fun boardPoints(board: BoardState): Int =
        (board.solvedOrder.size * 25 - board.mistakes * 5).coerceIn(0, 100)

    fun logicPoints(result: AccusationResult): Int =
        (if (result.who) 50 else 0) + (if (result.what) 25 else 0) + (if (result.where) 25 else 0)

    fun score(caseNumber: Int, board: BoardState, accusation: AccusationResult, fermi: FermiOutcome): ScoreCard {
        val boardPts = boardPoints(board)
        val logicPts = logicPoints(accusation)
        val total = (boardPts * 0.3 + logicPts * 0.5 + fermi.score * 0.2).roundToInt()
        val rank = Rank.forScore(total)
        return ScoreCard(boardPts, logicPts, fermi.score, total, rank, shareText(caseNumber, total, rank, board, accusation, fermi))
    }

    /** A spoiler-free result card, like Wordle's grid. */
    fun shareText(
        caseNumber: Int,
        total: Int,
        rank: Rank,
        board: BoardState,
        accusation: AccusationResult,
        fermi: FermiOutcome,
    ): String = buildString {
        appendLine("ALIBI #$caseNumber ${rank.emoji} ${rank.title} ($total)")
        board.guessHistory.forEach { guess -> appendLine(guess.joinToString("") { LEVEL_SQUARES[it] }) }
        fun tick(ok: Boolean) = if (ok) "✅" else "❌"
        appendLine("🔎 Who ${tick(accusation.who)} What ${tick(accusation.what)} Where ${tick(accusation.where)}")
        append("📏 Fermi ${fermi.verdict.emoji}")
    }
}
