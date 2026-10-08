package com.alibi.engine

import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.board.TapResult
import com.alibi.engine.board.WallState
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
import kotlin.test.assertTrue

class GameRulesTest {

    private val groups = listOf(
        EvidenceGroup("A", listOf("a1", "a2", "a3", "a4")),
        EvidenceGroup("B", listOf("b1", "b2", "b3", "b4")),
        EvidenceGroup("C", listOf("c1", "c2", "c3", "c4")),
    )
    private val sangeet = CaseLibrary.cases[0]

    private fun solvedWall(groups: List<EvidenceGroup>): WallState {
        var wall = WallState.start(groups)
        groups.forEach { g -> g.items.forEach { wall = wall.tap(it).first } }
        return wall
    }

    // ---- Evidence wall ----

    @Test
    fun `witnesses ask in order and the 4th right note finishes the task`() {
        var wall = WallState.start(groups, random = Random(1))
        assertEquals("A", wall.current?.title)
        listOf("a1", "a2", "a3").forEach { assertEquals(TapResult.Pinned, wall.tap(it).also { r -> wall = r.first }.second) }
        val (done, result) = wall.tap("a4")
        assertEquals(TapResult.TaskDone(0), result)
        assertEquals("B", done.current?.title)
        assertEquals(8, done.tiles.size)
        assertEquals(listOf(0), done.solved)
    }

    @Test
    fun `a wrong note costs chai once`() {
        val (once, result) = WallState.start(groups).tap("b1")
        assertEquals(TapResult.Wrong, result)
        assertEquals(1, once.mistakes)
        val (twice, again) = once.tap("b1")
        assertEquals(TapResult.Ignored, again)
        assertEquals(1, twice.mistakes)
    }

    @Test
    fun `tapping a pinned note does nothing`() {
        val (once, _) = WallState.start(groups).tap("a1")
        assertEquals(TapResult.Ignored, once.tap("a1").second)
    }

    @Test
    fun `running out of chai ends the wall and misses the rest`() {
        var wall = WallState.start(groups).tap("a1").first
        listOf("b1", "b2", "c1").forEach { wall = wall.tap(it).first }
        val (over, result) = wall.tap("c2")
        assertEquals(TapResult.OutOfPatience, result)
        assertTrue(over.isOver)
        assertEquals(listOf(0, 1, 2), over.missed)
        assertEquals(TapResult.Ignored, over.tap("a2").second)
    }

    @Test
    fun `decoys are on the wall but never right`() {
        val wall = WallState.start(groups, listOf("x1", "x2"))
        assertEquals(14, wall.tiles.size)
        assertEquals(TapResult.Wrong, wall.tap("x1").second)
    }

    @Test
    fun `pandu's hint points at a right note`() {
        val wall = WallState.start(groups).tap("a1").first.hint()!!
        assertEquals(1, wall.hintsUsed)
        assertEquals(setOf("a2"), wall.hinted)
        assertTrue(wall.hinted.all { it in groups[0].items })
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
        val v = Scoring.verdict(solvedWall(sangeet.groups), Interrogation.start(sangeet).arrest(sangeet.culprit))
        assertEquals(100, v.points)
        assertEquals(3, v.stars)
    }

    @Test
    fun `mistakes hints and wrong arrests cost points`() {
        var wall = WallState.start(sangeet.groups).tap("Kite").first.hint()!!
        sangeet.groups.forEach { g -> g.items.forEach { wall = wall.tap(it).first } }
        val room = Interrogation.start(sangeet).arrest(0).arrest(sangeet.culprit)
        assertEquals(100 - 5 - 5 - 25, Scoring.verdict(wall, room).points)
    }

    @Test
    fun `escape caps the score`() {
        val room = Interrogation.start(sangeet).arrest(0).arrest(1)
        val v = Scoring.verdict(solvedWall(sangeet.groups), room)
        assertTrue(v.points <= 30)
        assertEquals(0, v.stars)
    }

    @Test
    fun `newspaper stars the detective and the share text has no spoilers`() {
        for (file in CaseLibrary.cases) {
            val page = Newspaper.write(file, "Pravallika", 4, solvedWall(file.groups), Interrogation.start(file).arrest(file.culprit))
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
            WallState.start(file.groups, file.decoys) // all 16 notes are unique
            assertEquals(4, file.decoys.size)
            file.scene.evidence.forEach { assertTrue(it.name.isNotBlank()) }
            file.statements.forEach { assertTrue("Find 4" in it.ask || "Find the 4" in it.ask, it.witness) }
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
