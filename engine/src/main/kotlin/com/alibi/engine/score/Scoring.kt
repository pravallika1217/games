package com.alibi.engine.score

import com.alibi.engine.board.BoardState
import com.alibi.engine.cases.Interrogation

/** How well today's case went. */
data class Verdict(val points: Int, val stars: Int, val title: String)

/**
 * Start at 100. Each snapped string costs 5, each missed statement 10, each hint 5,
 * each wrong arrest 25. If the culprit escapes, the score can't go above 30.
 */
object Scoring {
    fun verdict(board: BoardState, hintsUsed: Int, room: Interrogation): Verdict {
        var points = 100 -
            board.mistakes * 5 -
            (board.groups.size - board.solvedOrder.size) * 10 -
            hintsUsed * 5 -
            room.released.size * 25
        if (!room.caught) points = minOf(points, 30)
        points = points.coerceIn(0, 100)
        return when {
            points >= 90 -> Verdict(points, 3, "Sherlock")
            points >= 70 -> Verdict(points, 2, "Sharp Inspector")
            points >= 45 -> Verdict(points, 1, "Constable on Duty")
            else -> Verdict(points, 0, "Desk Duty")
        }
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
