package com.alibi.engine

import com.alibi.engine.board.BoardState
import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.Accusation
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.fermi.FermiScorer
import com.alibi.engine.fermi.FermiVerdict
import com.alibi.engine.logic.DeductionNotes
import com.alibi.engine.logic.Mark
import com.alibi.engine.logic.room
import com.alibi.engine.logic.suspect
import com.alibi.engine.score.Rank
import com.alibi.engine.score.Scoring
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRulesTest {

    private val groups = listOf(
        EvidenceGroup("A", listOf("a1", "a2", "a3", "a4"), 0),
        EvidenceGroup("B", listOf("b1", "b2", "b3", "b4"), 1),
        EvidenceGroup("C", listOf("c1", "c2", "c3", "c4"), 2),
        EvidenceGroup("D", listOf("d1", "d2", "d3", "d4"), 3),
    )

    @Test
    fun `correct guess removes the group`() {
        val (board, result) = BoardState.start(groups, Random(1)).submit(setOf("b1", "b2", "b3", "b4"))
        assertEquals(GuessResult.Correct(1), result)
        assertEquals(12, board.tiles.size)
        assertEquals(listOf(1), board.solvedOrder)
        assertEquals(0, board.mistakes)
    }

    @Test
    fun `three of a group is one away and costs a mistake`() {
        val (board, result) = BoardState.start(groups).submit(setOf("a1", "a2", "a3", "b1"))
        assertEquals(GuessResult.OneAway, result)
        assertEquals(1, board.mistakes)
    }

    @Test
    fun `repeating a guess is free`() {
        val guess = setOf("a1", "a2", "b1", "b2")
        val (once, _) = BoardState.start(groups).submit(guess)
        val (twice, result) = once.submit(guess)
        assertEquals(GuessResult.AlreadyTried, result)
        assertEquals(1, twice.mistakes)
    }

    @Test
    fun `four mistakes ends the board`() {
        var board = BoardState.start(groups)
        listOf(
            setOf("a1", "b1", "c1", "d1"), setOf("a2", "b2", "c2", "d2"),
            setOf("a3", "b3", "c3", "d3"), setOf("a4", "b4", "c4", "d4"),
        ).forEach { board = board.submit(it).first }
        assertTrue(board.isLost)
        assertIs<GuessResult.GameOver>(board.submit(setOf("a1", "a2", "a3", "a4")).second)
    }

    @Test
    fun `fermi scoring rewards being close`() {
        assertEquals(100, FermiScorer.score(85.0, 85.0))
        assertEquals(70, FermiScorer.score(170.0, 85.0))
        assertEquals(70, FermiScorer.score(42.5, 85.0))
        assertEquals(0, FermiScorer.score(10_000.0, 85.0))
        assertEquals(0, FermiScorer.score(0.0, 85.0))
        assertEquals(FermiVerdict.BULLSEYE, FermiScorer.outcome(90.0, CaseLibrary.cases[0].fermi).verdict)
        assertEquals(FermiVerdict.WARM, FermiScorer.outcome(160.0, CaseLibrary.cases[0].fermi).verdict)
    }

    @Test
    fun `checking a cell crosses its row and column`() {
        val notes = DeductionNotes()
            .cycle(suspect(0), room(1), 3)
            .cycle(suspect(0), room(1), 3)
        assertEquals(Mark.CHECK, notes[suspect(0), room(1)])
        assertEquals(Mark.CROSS, notes[room(1), suspect(2)])
        assertEquals(Mark.CROSS, notes[suspect(0), room(0)])
        assertEquals(Mark.EMPTY, notes[suspect(1), room(0)])
        assertEquals(Mark.EMPTY, notes.cycle(suspect(0), room(1), 3)[suspect(0), room(1)])
    }

    @Test
    fun `every authored case is playable for months`() {
        for (day in 0L until 120L) {
            val case = CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY + day)
            assertEquals(day.toInt() + 1, case.number)
            assertEquals(4, case.bonusClues.size)
            assertTrue(case.clues.all { it.endsWith(".") && it.first().isUpperCase() })
            BoardState.start(case.file.groups) // validates unique tiles
            val p = case.puzzle
            assertTrue(case.accuse(Accusation(p.culprit, p.crimeWeapon, p.crimeRoom)).solved)
        }
    }

    @Test
    fun `perfect game makes Sherlock and share card hides answers`() {
        val case = CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY)
        var board = BoardState.start(case.file.groups)
        case.file.groups.forEach { board = board.submit(it.items.toSet()).first }
        val p = case.puzzle
        val accusation = case.accuse(Accusation(p.culprit, p.crimeWeapon, p.crimeRoom))
        val card = Scoring.score(case.number, board, accusation, case.fermiOutcome(case.file.fermi.answer))
        assertEquals(100, card.total)
        assertEquals(Rank.SHERLOCK, card.rank)
        assertTrue(case.theme.suspects.none { it.name in card.shareText })
        println(card.shareText)
        println(case.clues.joinToString("\n"))
        println(case.finalClue)
    }
}
