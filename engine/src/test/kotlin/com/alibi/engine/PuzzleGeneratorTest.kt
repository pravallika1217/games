package com.alibi.engine

import com.alibi.engine.logic.BruteForce
import com.alibi.engine.logic.Category
import com.alibi.engine.logic.Clue
import com.alibi.engine.logic.LogicGrid
import com.alibi.engine.logic.PuzzleGenerator
import com.alibi.engine.logic.suspect
import com.alibi.engine.logic.weapon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PuzzleGeneratorTest {

    @Test
    fun `every puzzle has exactly one answer and is solvable by logic`() {
        for (size in 3..5) {
            for (seed in 1L..60L) {
                val puzzle = PuzzleGenerator(size).generate(seed, bonusCount = size - 1)
                assertTrue(puzzle.clues.all { it.holds(puzzle.solution) }, "false clue, size $size seed $seed")
                assertEquals(1, BruteForce.countSolutions(size, puzzle.clues, stopAt = 2), "not unique, size $size seed $seed")
                assertTrue(LogicGrid.solve(size, puzzle.clues).second.solved, "needs guessing, size $size seed $seed")
            }
        }
    }

    @Test
    fun `every clue is needed`() {
        for (seed in 1L..40L) {
            val puzzle = PuzzleGenerator(4).generate(seed)
            for (clue in puzzle.clues) {
                assertFalse(LogicGrid.solve(4, puzzle.clues - clue).second.solved, "redundant clue at seed $seed: $clue")
            }
        }
    }

    @Test
    fun `bonus clues are true and never name the culprit`() {
        for (seed in 1L..100L) {
            val puzzle = PuzzleGenerator(4).generate(seed)
            assertEquals(4, puzzle.bonusClues.size)
            for (clue in puzzle.bonusClues) {
                assertTrue(clue.holds(puzzle.solution))
                clue as Clue.Together
                val owner = puzzle.solution.ownerOf(clue.a)
                assertTrue(owner != puzzle.culprit, "bonus clue gives away the culprit at seed $seed")
            }
        }
    }

    @Test
    fun `crime weapon and room belong to the culprit`() {
        for (seed in 1L..50L) {
            val p = PuzzleGenerator(4).generate(seed)
            assertEquals(p.crimeWeapon, p.solution.weaponOf[p.culprit])
            assertEquals(p.crimeRoom, p.solution.roomOf[p.culprit])
        }
    }

    @Test
    fun `same seed gives the same puzzle`() {
        assertEquals(PuzzleGenerator(4).generate(42L), PuzzleGenerator(4).generate(42L))
    }

    @Test
    fun `grid confirm rules out the rest of the row and column`() {
        val grid = LogicGrid(3)
        grid.apply(Clue.Together(suspect(0), weapon(1)))
        assertEquals(listOf(1), grid.candidates(suspect(0), Category.WEAPON))
        assertEquals(listOf(0), grid.candidates(weapon(1), Category.SUSPECT))
        assertEquals(listOf(0, 2), grid.candidates(suspect(1), Category.WEAPON))
    }
}
