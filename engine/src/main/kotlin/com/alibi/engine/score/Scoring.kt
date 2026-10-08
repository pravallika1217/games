package com.alibi.engine.score

import com.alibi.engine.board.WallState
import com.alibi.engine.cases.Interrogation

/** How well today's case went. */
data class Verdict(val points: Int, val stars: Int, val title: String)

/**
 * Start at 100. Each wrong note costs 5, each missed statement 10, each hint 5,
 * each wrong arrest 25. If the culprit escapes, the score can't go above 30.
 */
object Scoring {
    fun verdict(wall: WallState, room: Interrogation): Verdict {
        var points = 100 -
            wall.mistakes * 5 -
            (wall.groups.size - wall.solved.size) * 10 -
            wall.hintsUsed * 5 -
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
