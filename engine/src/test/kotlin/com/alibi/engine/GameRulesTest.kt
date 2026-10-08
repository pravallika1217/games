package com.alibi.engine

import com.alibi.engine.board.BoardState
import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.CatchState
import com.alibi.engine.fermi.FermiScorer
import com.alibi.engine.fermi.FermiVerdict
import com.alibi.engine.score.Rank
import com.alibi.engine.score.Scoring
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRulesTest {

    private val groups = listOf(
        EvidenceGroup("A", listOf("a1", "a2", "a3", "a4"), 0),
        EvidenceGroup("B", listOf("b1", "b2", "b3", "b4"), 1),
        EvidenceGroup("C", listOf("c1", "c2", "c3", "c4"), 2),
    )

    @Test
    fun `correct guess removes the group`() {
        val (board, result) = BoardState.start(groups, Random(1)).submit(setOf("b1", "b2", "b3", "b4"))
        assertEquals(GuessResult.Correct(1), result)
        assertEquals(8, board.tiles.size)
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
            setOf("a1", "a2", "b1", "c1"), setOf("a1", "b2", "b3", "c2"),
            setOf("a2", "b4", "c3", "c4"), setOf("a3", "a4", "b1", "c1"),
        ).forEach { board = board.submit(it).first }
        assertTrue(board.isLost)
        assertIs<GuessResult.GameOver>(board.submit(setOf("a1", "a2", "a3", "a4")).second)
    }

    @Test
    fun `wrong accusation leaves one more try`() {
        var catch = CatchState(culprit = 2, suspectCount = 3)
        catch = catch.accuse(0)
        assertFalse(catch.isOver)
        assertEquals(1, catch.triesLeft)
        assertTrue(0 in catch.cleared)
        catch = catch.accuse(2)
        assertTrue(catch.caught)
        assertEquals(25, Scoring.catchPoints(catch))
    }

    @Test
    fun `two wrong accusations and the culprit gets away`() {
        val catch = CatchState(culprit = 2, suspectCount = 3).accuse(0).accuse(1)
        assertTrue(catch.isOver)
        assertFalse(catch.caught)
        assertEquals(0, Scoring.catchPoints(catch))
        assertEquals(catch, catch.accuse(2))
    }

    @Test
    fun `stamping two innocents leaves one suspect standing`() {
        val start = CatchState(culprit = 1, suspectCount = 3)
        assertNull(start.lastOneStanding)
        val notes = start.toggleCleared(0).toggleCleared(2)
        assertEquals(1, notes.lastOneStanding)
        assertNull(notes.toggleCleared(2).lastOneStanding)
    }

    @Test
    fun `bonus scoring rewards being close`() {
        assertEquals(100, FermiScorer.score(85.0, 85.0))
        assertEquals(70, FermiScorer.score(170.0, 85.0))
        assertEquals(70, FermiScorer.score(42.5, 85.0))
        assertEquals(0, FermiScorer.score(10_000.0, 85.0))
        val bonus = CaseLibrary.cases[0].bonus
        assertEquals(FermiVerdict.BULLSEYE, FermiScorer.outcome(90.0, bonus).verdict)
        assertEquals(FermiVerdict.WARM, FermiScorer.outcome(160.0, bonus).verdict)
    }

    @Test
    fun `every case is well formed and the schedule cycles`() {
        for (day in 0L until 30L) {
            val case = CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY + day)
            assertEquals(day.toInt() + 1, case.number)
            BoardState.start(case.file.groups) // checks all 12 words are unique
        }
        // The culprit shouldn't always sit in the same spot.
        assertTrue(CaseLibrary.cases.map { it.culprit }.toSet().size > 1)
    }

    @Test
    fun `perfect game is Sherlock and the share card has no spoilers`() {
        val case = CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY)
        var board = BoardState.start(case.file.groups)
        case.file.groups.forEach { board = board.submit(it.items.toSet()).first }
        val catch = case.startCatch().accuse(case.file.culprit)
        val card = Scoring.score(case.number, board, catch, case.bonusOutcome(case.file.bonus.answer))
        assertEquals(100, card.total)
        assertEquals(Rank.SHERLOCK, card.rank)
        assertTrue(case.file.suspects.none { it.name in card.shareText })
    }

    @Test
    fun `skipping the bonus still allows Sherlock`() {
        val case = CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY)
        var board = BoardState.start(case.file.groups)
        case.file.groups.forEach { board = board.submit(it.items.toSet()).first }
        val card = Scoring.score(case.number, board, case.startCatch().accuse(case.file.culprit), bonus = null)
        assertEquals(90, card.total)
        assertEquals(Rank.SHERLOCK, card.rank)
        assertTrue(card.shareText.endsWith("📏 Bonus skipped"))
    }
}
