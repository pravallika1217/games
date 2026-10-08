package com.alibi.engine

import com.alibi.engine.board.BoardState
import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.Interrogation
import com.alibi.engine.cases.Mood
import com.alibi.engine.cases.Speaker
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.Newspaper
import com.alibi.engine.score.Scoring
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRulesTest {

    private val groups = listOf(
        EvidenceGroup("A", listOf("a1", "a2", "a3", "a4"), 0),
        EvidenceGroup("B", listOf("b1", "b2", "b3", "b4"), 1),
        EvidenceGroup("C", listOf("c1", "c2", "c3", "c4"), 2),
    )
    private val sangeet = CaseLibrary.cases[0]

    private fun solvedBoard(groups: List<EvidenceGroup>): BoardState {
        var board = BoardState.start(groups)
        groups.forEach { board = board.submit(it.items.toSet()).first }
        return board
    }

    // ---- Evidence wall ----

    @Test
    fun `correct guess removes the group`() {
        val (board, result) = BoardState.start(groups, Random(1)).submit(setOf("b1", "b2", "b3", "b4"))
        assertEquals(GuessResult.Correct(1), result)
        assertEquals(8, board.tiles.size)
        assertEquals(listOf(1), board.solvedOrder)
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
    fun `four mistakes ends the wall`() {
        var board = BoardState.start(groups)
        listOf(
            setOf("a1", "a2", "b1", "c1"), setOf("a1", "b2", "b3", "c2"),
            setOf("a2", "b4", "c3", "c4"), setOf("a3", "a4", "b1", "c1"),
        ).forEach { board = board.submit(it).first }
        assertTrue(board.isLost)
        assertIs<GuessResult.GameOver>(board.submit(setOf("a1", "a2", "a3", "a4")).second)
    }

    // ---- Interrogation ----

    @Test
    fun `calling a suspect in gives their alibi once`() {
        val room = Interrogation.start(sangeet).callIn(1).callIn(1)
        assertEquals(listOf(Speaker.ROOM, Speaker.SUSPECT), room.chats[1].map { it.from })
        assertEquals(sangeet.suspects[1].alibi, room.chats[1].last().text)
    }

    @Test
    fun `the culprit gets nervous at the right statement`() {
        val room = Interrogation.start(sangeet).callIn(2).present(2, 0).respond(2, 0)
        assertEquals(Mood.NERVOUS, room.moods[2])
        assertTrue(room.notes.any { "sunglasses" in it })
        assertEquals(Speaker.DETECTIVE, room.chats[2][2].from)
    }

    @Test
    fun `unrelated evidence gets small talk and no mood change`() {
        val room = Interrogation.start(sangeet).callIn(0).present(0, 0).respond(0, 0)
        assertEquals(Mood.CALM, room.moods[0])
        assertEquals(sangeet.suspects[0].smallTalk[0], room.chats[0].last().text)
    }

    @Test
    fun `showing the same statement twice does nothing`() {
        val once = Interrogation.start(sangeet).callIn(0).present(0, 1)
        assertEquals(once, once.present(0, 1))
    }

    @Test
    fun `wrong arrest uses a warrant and two wrong arrests end the case`() {
        var room = Interrogation.start(sangeet).arrest(0)
        assertEquals(1, room.warrantsLeft)
        assertFalse(room.isOver)
        assertEquals(room, room.arrest(0)) // already released
        room = room.arrest(1)
        assertTrue(room.isOver)
        assertFalse(room.caught)
        assertEquals(room, room.arrest(2))
    }

    @Test
    fun `arresting the culprit closes the case`() {
        val room = Interrogation.start(sangeet).arrest(sangeet.culprit)
        assertTrue(room.caught)
        assertTrue(room.isOver)
    }

    // ---- Scoring and newspaper ----

    @Test
    fun `perfect case is Sherlock`() {
        val v = Scoring.verdict(solvedBoard(sangeet.groups), 0, Interrogation.start(sangeet).arrest(sangeet.culprit))
        assertEquals(100, v.points)
        assertEquals(3, v.stars)
    }

    @Test
    fun `mistakes hints and wrong arrests cost points`() {
        var board = BoardState.start(sangeet.groups)
        board = board.submit(setOf("Jalebi", "Kite", "Ear", "Laddu")).first
        sangeet.groups.forEach { board = board.submit(it.items.toSet()).first }
        val room = Interrogation.start(sangeet).arrest(0).arrest(sangeet.culprit)
        assertEquals(100 - 5 - 5 - 25, Scoring.verdict(board, hintsUsed = 1, room = room).points)
    }

    @Test
    fun `escape caps the score`() {
        val room = Interrogation.start(sangeet).arrest(0).arrest(1)
        val v = Scoring.verdict(solvedBoard(sangeet.groups), 0, room)
        assertTrue(v.points <= 30)
        assertEquals(0, v.stars)
    }

    @Test
    fun `newspaper stars the detective and the share text has no spoilers`() {
        for (file in CaseLibrary.cases) {
            val page = Newspaper.write(file, "Pravallika", 4, solvedBoard(file.groups), 0, Interrogation.start(file).arrest(file.culprit))
            assertTrue("Pravallika" in page.headline)
            assertTrue("4 minutes" in page.headline)
            file.suspects.forEach { assertFalse(it.name in page.shareText, "share text names ${it.name}") }
        }
    }

    @Test
    fun `career ranks go up with solved cases`() {
        assertEquals(CareerRank.ROOKIE, CareerRank.forSolved(0))
        assertEquals(CareerRank.SUB_INSPECTOR, CareerRank.forSolved(1))
        assertEquals(CareerRank.INSPECTOR, CareerRank.forSolved(6))
        assertEquals(CareerRank.COMMISSIONER, CareerRank.forSolved(99))
        assertEquals(null, CareerRank.COMMISSIONER.next)
    }

    // ---- Case content ----

    @Test
    fun `every case can be solved by interrogation`() {
        for (file in CaseLibrary.cases) {
            BoardState.start(file.groups) // all 12 notes are unique
            val nervous = file.reactions.values.flatMap { it.entries }.filter { it.value.mood == Mood.NERVOUS }.map { it.key }
            assertEquals(listOf(file.culprit), nervous, "${file.title}: only the culprit should get nervous")
            val cleared = file.reactions.values.flatMap { it.entries }.filter { it.value.mood == Mood.RELIEVED }.map { it.key }.toSet()
            assertEquals(file.suspects.indices.toSet() - file.culprit, cleared, "${file.title}: every innocent needs an alibi check")
            assertTrue("{name}" in file.call)
            file.scene.evidence.forEach { assertTrue(it.text.isNotBlank() && it.x in 0f..1f && it.y in 0f..1f) }
        }
        assertTrue(CaseLibrary.cases.map { it.culprit }.toSet().size > 1, "the culprit shouldn't always sit in the same chair")
    }

    @Test
    fun `the schedule gives one case a day`() {
        for (day in 0L until 10L) {
            assertEquals(day.toInt() + 1, CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY + day).number)
        }
    }
}
