package com.alibi.engine.score

import com.alibi.engine.board.BoardState
import com.alibi.engine.cases.CatchState
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
    val catchPoints: Int,
    val bonusPoints: Int,
    val total: Int,
    val rank: Rank,
    val shareText: String,
)

/**
 * Words: up to 40. Catching the culprit: 50 first try, 25 second try. Bonus guess: up to 10.
 */
object Scoring {
    private val LEVEL_SQUARES = listOf("🟨", "🟩", "🟦", "🟪")

    fun boardPoints(board: BoardState): Int =
        (board.solvedOrder.size * 40 / board.groups.size - board.mistakes * 3).coerceIn(0, 40)

    fun catchPoints(catch: CatchState): Int = when {
        !catch.caught -> 0
        catch.wrongGuesses.isEmpty() -> 50
        else -> 25
    }

    fun bonusPoints(bonus: FermiOutcome?): Int = ((bonus?.score ?: 0) / 10.0).roundToInt()

    fun score(caseNumber: Int, board: BoardState, catch: CatchState, bonus: FermiOutcome?): ScoreCard {
        val boardPts = boardPoints(board)
        val catchPts = catchPoints(catch)
        val bonusPts = bonusPoints(bonus)
        val total = boardPts + catchPts + bonusPts
        val rank = Rank.forScore(total)
        return ScoreCard(boardPts, catchPts, bonusPts, total, rank, shareText(caseNumber, total, rank, board, catch, bonus))
    }

    /** A spoiler-free result card, like Wordle's grid. */
    fun shareText(
        caseNumber: Int,
        total: Int,
        rank: Rank,
        board: BoardState,
        catch: CatchState,
        bonus: FermiOutcome?,
    ): String = buildString {
        appendLine("ALIBI #$caseNumber ${rank.emoji} ${rank.title} ($total)")
        board.guessHistory.forEach { guess -> appendLine(guess.joinToString("") { LEVEL_SQUARES[it] }) }
        appendLine(
            when {
                catch.caught && catch.wrongGuesses.isEmpty() -> "🕵️ Caught on the 1st try"
                catch.caught -> "🕵️ Caught on the 2nd try"
                else -> "🏃 The culprit got away"
            }
        )
        append(if (bonus != null) "📏 Bonus ${bonus.verdict.emoji}" else "📏 Bonus skipped")
    }
}
