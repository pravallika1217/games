package com.alibi.engine

import com.alibi.engine.cases.ArrestState
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.Questioning
import com.alibi.engine.cases.SearchResult
import com.alibi.engine.cases.SearchState
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.Scoring
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRulesTest {

    private val sangeet = CaseLibrary.cases[0]

    /** Finds every clue without a mistake. */
    private fun solvedSearch(file: com.alibi.engine.cases.CaseFile): SearchState {
        var s = SearchState(file)
        file.clues.forEachIndexed { i, c ->
            assertEquals(SearchResult.Found(i), s.search(c.at).second)
            s = s.answer(i, c.pointsTo).first
        }
        return s
    }

    // ---- Search ----

    @Test
    fun `the riddle's place reveals the clue, other places don't`() {
        val s = SearchState(sangeet)
        assertEquals(SearchResult.Found(0), s.search("bin").second)
        val (next, result) = s.search("stove")
        assertEquals(SearchResult.Nothing("Burnt jalebi. The cook was busy."), result)
        assertEquals(1, next.wrongSearches)
    }

    @Test
    fun `searching the same wrong place twice counts once`() {
        val once = SearchState(sangeet).search("stove").first
        assertEquals(1, once.search("stove").first.wrongSearches)
    }

    @Test
    fun `a clue's place is empty until its riddle comes up`() {
        val (s, result) = SearchState(sangeet).search("door")
        assertIs<SearchResult.Nothing>(result)
        assertEquals(1, s.wrongSearches)
    }

    @Test
    fun `right answer bags the clue and ties a string`() {
        val (s, ok) = SearchState(sangeet).answer(0, 0)
        assertTrue(ok)
        assertEquals(listOf(0), s.found)
        assertEquals(listOf(1, 0, 0), s.strings)
        assertEquals(1, s.current)
    }

    @Test
    fun `wrong answer counts a mistake and keeps the clue open`() {
        val (s, ok) = SearchState(sangeet).answer(0, 2)
        assertFalse(ok)
        assertEquals(1, s.wrongAnswers)
        assertEquals(0, s.current)
    }

    @Test
    fun `pandu helps only after 3 wrong searches`() {
        var s = SearchState(sangeet)
        listOf("stove", "fridge").forEach { s = s.search(it).first }
        assertFalse(s.canAskForHint)
        s = s.search("tea").first
        assertTrue(s.canAskForHint)
        val (helped, place) = s.hint()!!
        assertEquals("bin", place)
        assertEquals(1, helped.hintsUsed)
    }

    @Test
    fun `after all clues the strings point mostly at the culprit`() {
        for (file in CaseLibrary.cases) {
            val s = solvedSearch(file)
            assertTrue(s.isDone)
            assertNull(s.hint())
            assertEquals(s.strings.max(), s.strings[file.culprit], "${file.title}: strings should point at the culprit")
        }
    }

    // ---- Questioning and arrest ----

    @Test
    fun `showing the right clue to the culprit catches the lie`() {
        val (q, reply) = Questioning(sangeet).show(2, 1)
        assertFalse(reply.bySuspect)
        assertEquals(setOf(2), q.lying)
    }

    @Test
    fun `innocents answer every clue and are never caught`() {
        for (file in CaseLibrary.cases) {
            file.suspects.indices.filter { it != file.culprit }.forEach { i ->
                file.clues.indices.forEach { c ->
                    val (q, reply) = Questioning(file).show(i, c)
                    assertTrue(reply.bySuspect && reply.text.isNotBlank(), "${file.title}: ${file.suspects[i].name} has no reply to clue $c")
                    assertTrue(q.lying.isEmpty())
                }
            }
            // The culprit either answers or gets caught, never says nothing.
            file.clues.indices.forEach { c -> assertTrue(Questioning(file).show(file.culprit, c).second.text.isNotBlank()) }
        }
    }

    @Test
    fun `one wrong arrest is forgiven, two and the culprit escapes`() {
        var a = ArrestState(sangeet).arrest(0)
        assertFalse(a.isOver)
        assertEquals(a, a.arrest(0))
        a = a.arrest(1)
        assertTrue(a.escaped)
        assertEquals(a, a.arrest(2))
        assertTrue(ArrestState(sangeet).arrest(2).caught)
    }

    // ---- Scoring ----

    @Test
    fun `a clean case is three stars and the share text has no spoilers`() {
        val r = Scoring.result(1, sangeet, 125, 0, solvedSearch(sangeet), ArrestState(sangeet).arrest(2))
        assertEquals(3, r.stars)
        assertEquals("2:05", r.time)
        sangeet.suspects.forEach { assertFalse(it.name in r.shareText) }
    }

    @Test
    fun `mistakes and a wrong arrest cost stars, escaping costs all`() {
        var s = SearchState(sangeet)
        listOf("stove", "fridge", "tea").forEach { s = s.search(it).first }
        val sloppy = Scoring.result(1, sangeet, 60, 1, s, ArrestState(sangeet).arrest(0).arrest(2))
        assertEquals(4, sloppy.mistakes)
        assertEquals(1, sloppy.stars)
        assertEquals(0, Scoring.result(1, sangeet, 60, 0, s, ArrestState(sangeet).arrest(0).arrest(1)).stars)
    }

    @Test
    fun `career ranks go up with solved cases`() {
        assertEquals(CareerRank.ROOKIE, CareerRank.forSolved(0))
        assertEquals(CareerRank.SUB_INSPECTOR, CareerRank.forSolved(1))
        assertEquals(CareerRank.COMMISSIONER, CareerRank.forSolved(99))
    }

    // ---- Content ----

    @Test
    fun `every case is well formed`() {
        for (file in CaseLibrary.cases) {
            assertTrue(file.callLines("Pravallika").all { "{name}" !in it })
            assertTrue(file.clues.all { "**" in it.found }, "${file.title}: mark the key words in bold")
            assertEquals(3, file.examination.spots.size)
            file.hideouts.forEach { assertTrue(it.x in 0f..1f && it.y in 0f..1f) }
        }
        assertTrue(CaseLibrary.cases.map { it.culprit }.toSet().size > 1, "the culprit shouldn't always sit in the same chair")
    }

    @Test
    fun `the schedule gives one case a day`() {
        for (day in 0L until 6L) assertEquals(day.toInt() + 1, CaseLibrary.forDay(CaseLibrary.LAUNCH_DAY + day).number)
    }
}
