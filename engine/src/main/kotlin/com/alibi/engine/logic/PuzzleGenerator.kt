package com.alibi.engine.logic

import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * A generated Act 2 puzzle.
 *
 * [clues] alone are enough to solve it by logic. [bonusClues] are extra true facts handed out
 * for each Act 1 group the player solves; they never name the culprit, they just make the
 * grid quicker to fill.
 */
data class LogicPuzzle(
    val size: Int,
    val solution: Solution,
    val clues: List<Clue>,
    val bonusClues: List<Clue>,
    /** True: the case reveals the crime room. False: it reveals the weapon. */
    val revealsRoom: Boolean,
    val culprit: Int,
    val crimeWeapon: Int,
    val crimeRoom: Int,
    val difficulty: Difficulty,
)

/**
 * Builds puzzles that always have exactly one answer and can always be solved by logic alone.
 *
 * 1. Pick a random hidden solution.
 * 2. Add true clues (mostly "not" and "either/or" clues, which are more fun) until the
 *    human-style solver in [LogicGrid] can finish the grid.
 * 3. Remove every clue that isn't needed, so each one matters.
 */
class PuzzleGenerator(private val size: Int = 4) {
    init { require(size in 3..5) { "Grids between 3 and 5 are supported" } }

    fun generate(seed: Long, bonusCount: Int = 4): LogicPuzzle {
        val random = Random(seed)
        val solution = Solution(
            weaponOf = (0 until size).shuffled(random),
            roomOf = (0 until size).shuffled(random),
        )

        var grid = LogicGrid(size)
        val chosen = mutableListOf<Clue>()
        for (clue in candidateClues(solution, random)) {
            val trial = grid.copy()
            if (!trial.apply(clue)) continue
            trial.propagate()
            chosen += clue
            grid = trial
            if (grid.isSolved) break
        }
        check(grid.isSolved) { "Clue pool could not determine the solution" }

        var clues = chosen.toList()
        for (clue in chosen.shuffled(random)) {
            val without = clues - clue
            if (LogicGrid.solve(size, without).second.solved) clues = without
        }

        val crimeRoom = random.nextInt(size)
        val culprit = solution.ownerOf(room(crimeRoom))
        val crimeWeapon = solution.weaponOf[culprit]

        val rounds = LogicGrid.solve(size, clues).second.rounds
        return LogicPuzzle(
            size = size,
            solution = solution,
            clues = clues.shuffled(random),
            bonusClues = bonusFacts(solution, culprit, clues, random).take(bonusCount),
            revealsRoom = random.nextBoolean(),
            culprit = culprit,
            crimeWeapon = crimeWeapon,
            crimeRoom = crimeRoom,
            difficulty = when {
                rounds <= 2 -> Difficulty.EASY
                rounds <= 4 -> Difficulty.MEDIUM
                else -> Difficulty.HARD
            },
        )
    }

    private fun candidateClues(solution: Solution, random: Random): List<Clue> {
        val together = mutableListOf<Clue>()
        val apart = mutableListOf<Clue>()
        for ((a, b) in LogicGrid.PAIRS) {
            for (i in 0 until size) for (j in 0 until size) {
                val x = Item(a, i)
                val y = Item(b, j)
                if (solution.linked(x, y)) together += Clue.Together(x, y) else apart += Clue.Apart(x, y)
            }
        }

        // "Either A or B" clues: suspects for a weapon or room, and rooms for a weapon.
        val eitherOr = mutableListOf<Clue>()
        for ((options, target) in listOf(
            Category.SUSPECT to Category.WEAPON,
            Category.SUSPECT to Category.ROOM,
            Category.ROOM to Category.WEAPON,
        )) {
            for (t in 0 until size) {
                val targetItem = Item(target, t)
                val owner = solution.ownerOf(targetItem)
                val truth = when (options) {
                    Category.SUSPECT -> owner
                    Category.WEAPON -> solution.weaponOf[owner]
                    Category.ROOM -> solution.roomOf[owner]
                }
                val decoy = (0 until size).filter { it != truth }.random(random)
                val (first, second) = listOf(truth, decoy).shuffled(random)
                eitherOr += Clue.EitherOr(Item(options, first), Item(options, second), targetItem)
            }
        }

        // One early positive clue gives players a foothold; the rest are kept as a fallback.
        val shuffledTogether = together.shuffled(random)
        return listOf(shuffledTogether.first()) +
            (apart + eitherOr).shuffled(random) +
            shuffledTogether.drop(1)
    }

    /** True facts that don't involve the culprit, so a bonus clue never gives the answer away. */
    private fun bonusFacts(solution: Solution, culprit: Int, used: List<Clue>, random: Random): List<Clue> {
        val facts = (0 until size).filter { it != culprit }.flatMap { s ->
            listOf(
                Clue.Together(suspect(s), weapon(solution.weaponOf[s])),
                Clue.Together(suspect(s), room(solution.roomOf[s])),
                Clue.Together(weapon(solution.weaponOf[s]), room(solution.roomOf[s])),
            )
        }
        return (facts - used.toSet()).shuffled(random)
    }
}
