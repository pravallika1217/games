package com.alibi.engine

import com.alibi.engine.cases.ArrestState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.CaseJson
import com.alibi.engine.cases.CaseSchedule
import com.alibi.engine.cases.Investigation
import com.alibi.engine.cases.Questioning
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.Scoring
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRulesTest {

    /** The folder that gets published: content/cases. */
    private val folder = File(System.getProperty("alibi.cases") ?: "../content/cases")
    private val schedule = CaseJson.schedule(File(folder, "schedule.json").readText())
    private fun load(id: String): CaseFile = CaseJson.case(File(folder, "$id.json").readText())
    private val sangeet = load("sangeet")

    /** Follows every lead without a mistake. */
    private fun solved(file: CaseFile): Investigation {
        var s = Investigation(file)
        file.leads.indices.forEach { i ->
            s = s.guess(file.leads[i].answer).first
            s = s.pick(file.expected(i, s.stillIn)).first
        }
        return s
    }

    // ---- The case files ----

    @Test
    fun `every scheduled case loads and is fair`() {
        schedule.cases.forEach { id ->
            val file = load(id)
            assertEquals(id, file.id, "$id.json has the wrong id inside")
            assertEquals(emptyList(), file.problems())
        }
    }

    @Test
    fun `every case file in the folder is on the schedule`() {
        val files = folder.listFiles()!!.map { it.name }.filter { it != "schedule.json" && it.endsWith(".json") }
        assertEquals(files.map { it.removeSuffix(".json") }.sorted(), schedule.cases.distinct().sorted())
    }

    @Test
    fun `a broken case is refused with a clear reason`() {
        val text = File(folder, "sangeet.json").readText().replace("\"culprit\": \"vikram\"", "\"culprit\": \"rocky\"")
        val error = assertFailsWith<IllegalArgumentException> { CaseJson.case(text) }
        assertTrue("the culprit must be one of the suspects" in error.message!!, error.message)
    }

    @Test
    fun `the schedule gives one case a day and starts again when it runs out`() {
        val launch = LocalDate.parse(schedule.launchDay).toEpochDay()
        assertEquals(1, schedule.caseNumber(launch))
        assertEquals(1, schedule.caseNumber(launch - 5))
        assertEquals(2, schedule.caseNumber(launch + 1))
        assertEquals(schedule.cases[0], schedule.caseId(1))
        assertEquals(schedule.cases[0], schedule.caseId(schedule.cases.size + 1))
    }

    @Test
    fun `a newer schedule from the web can add cases without changing past days`() {
        val bigger = CaseSchedule(schedule.launchDay, schedule.cases + "next-case")
        schedule.cases.indices.forEach { assertEquals(schedule.caseId(it + 1), bigger.caseId(it + 1)) }
        assertEquals("next-case", bigger.caseId(schedule.cases.size + 1))
    }

    // ---- Following the leads ----

    @Test
    fun `a wrong answer is a mistake, the right one opens the clue`() {
        val (wrong, ok) = Investigation(sangeet).guess(0)
        assertFalse(ok)
        assertEquals(1, wrong.wrongGuesses)
        assertFalse(wrong.clueOpen)
        assertEquals(1, wrong.guess(0).first.wrongGuesses, "the same wrong answer only counts once")
        val (right, ok2) = wrong.guess(1)
        assertTrue(ok2)
        assertTrue(right.clueOpen)
    }

    @Test
    fun `the laddu crosses out everyone who isn't family`() {
        val open = Investigation(sangeet).guess(1).first
        assertEquals(setOf("rocky", "cook", "photo", "pandit"), sangeet.expected(0, open.stillIn))
        val (wrong, ok) = open.pick(setOf("rocky", "cook", "photo"))
        assertFalse(ok)
        assertEquals(1, wrong.wrongPicks)
        val done = wrong.pick(setOf("rocky", "cook", "photo", "pandit")).first
        assertEquals(1, done.done)
        assertEquals(listOf("aunty", "priya", "arjun", "vikram"), done.stillIn.map { it.id })
    }

    @Test
    fun `the sunglasses mark people but cross nobody out`() {
        var s = Investigation(sangeet)
        s = s.guess(1).first.pick(sangeet.expected(0, s.stillIn)).first
        s = s.guess(1).first
        s = s.pick(setOf("arjun", "vikram")).first
        assertEquals(setOf("arjun", "vikram"), s.marked)
        assertEquals(4, s.stillIn.size)
    }

    @Test
    fun `following every lead leaves exactly the suspects`() {
        listOf("sangeet", "hostel", "night-express").map(::load).forEach { file ->
            val s = solved(file)
            assertTrue(s.isDone)
            assertEquals(file.suspects.map { it.id }, s.stillIn.map { it.id })
            assertTrue(file.culprit in s.marked, "${file.id}: the culprit should be marked")
        }
    }

    // ---- Questioning and arrest ----

    @Test
    fun `showing the right evidence to the culprit catches the lie`() {
        val culprit = sangeet.culpritIndex
        val (q, reply) = Questioning(sangeet).show(culprit, 1)
        assertFalse(reply.bySuspect)
        assertEquals(setOf(culprit), q.lying)
    }

    @Test
    fun `innocents answer every piece of evidence and are never caught`() {
        listOf("sangeet", "hostel", "night-express").map(::load).forEach { file ->
            var q = Questioning(file)
            file.suspects.indices.filter { it != file.culpritIndex }.forEach { s ->
                file.leads.indices.forEach { c ->
                    val (next, reply) = q.show(s, c)
                    assertTrue(reply.bySuspect)
                    q = next
                }
            }
            assertTrue(q.lying.isEmpty())
        }
    }

    @Test
    fun `one wrong arrest is forgiven, two and the culprit escapes`() {
        val innocents = sangeet.suspects.indices.filter { it != sangeet.culpritIndex }
        val once = ArrestState(sangeet).arrest(innocents[0])
        assertFalse(once.isOver)
        assertEquals(1, once.warrants)
        assertTrue(once.arrest(sangeet.culpritIndex).caught)
        val twice = once.arrest(innocents[1])
        assertTrue(twice.escaped)
    }

    // ---- Scoring ----

    @Test
    fun `a clean case is three stars and the share text has no spoilers`() {
        val r = Scoring.result(1, sangeet, 200, 0, solved(sangeet), ArrestState(sangeet).arrest(sangeet.culpritIndex))
        assertEquals(3, r.stars)
        sangeet.suspects.forEach { assertFalse(it.name in r.shareText) }
    }

    @Test
    fun `mistakes and a wrong arrest cost stars, escaping costs all`() {
        val messy = Investigation(sangeet).guess(0).first.guess(2).first.guess(3).first
        val caught = ArrestState(sangeet).arrest(sangeet.culpritIndex)
        assertEquals(2, Scoring.result(1, sangeet, 100, 0, messy, caught).stars)
        val innocent = sangeet.suspects.indices.first { it != sangeet.culpritIndex }
        val second = ArrestState(sangeet).arrest(innocent).arrest(sangeet.culpritIndex)
        assertEquals(1, Scoring.result(1, sangeet, 100, 0, messy, second).stars)
        val others = sangeet.suspects.indices.filter { it != sangeet.culpritIndex }
        val escaped = ArrestState(sangeet).arrest(others[0]).arrest(others[1])
        assertEquals(0, Scoring.result(1, sangeet, 100, 0, solved(sangeet), escaped).stars)
    }

    @Test
    fun `career ranks go up with solved cases`() {
        assertEquals(CareerRank.ROOKIE, CareerRank.forSolved(0))
        assertEquals(CareerRank.SUB_INSPECTOR, CareerRank.forSolved(1))
        assertEquals(CareerRank.COMMISSIONER, CareerRank.forSolved(99))
    }
}
