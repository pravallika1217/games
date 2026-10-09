package com.alibi.engine.cases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Something you can see about a person, like "🕶️ Sunglasses". Clues check these. */
@Serializable
data class Trait(val emoji: String, val text: String)

/** One row of the constable's briefing at the door, e.g. "🕛 Time: around midnight". */
@Serializable
data class BriefingLine(val emoji: String, val label: String, val value: String, val unknown: Boolean = false)

/** A marked spot to check during the examination. Positions are fractions of the picture. */
@Serializable
data class ExamSpot(val title: String, val text: String, val x: Float, val y: Float)

/**
 * The first close look: the body, or for a theft, the place things were taken from.
 * After checking every spot, the detective answers one question about what happened.
 */
@Serializable
data class Examination(
    val title: String,
    /** Null draws a chalk outline of a body; otherwise a big picture of this object. */
    val objectEmoji: String? = null,
    val spots: List<ExamSpot>,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val why: String,
)

/**
 * What a suspect says in questioning. Only the people left after the leads are questioned.
 * [replies] and [gotchas] go by lead: for each lead the suspect either explains (a reply)
 * or gets caught lying (a gotcha, only for the culprit).
 */
@Serializable
data class Interview(
    /** Where they say they were. */
    val says: String,
    val alibi: String,
    val replies: List<String?>,
    val gotchas: List<String?> = emptyList(),
    /** Why an innocent suspect is let go after a wrong arrest. */
    val release: String? = null,
) {
    fun gotcha(lead: Int): String? = gotchas.getOrNull(lead)
}

/** Someone who was at the scene. [group] (Family, Staff, Guest…) counts as a trait too. */
@Serializable
data class Person(
    val id: String,
    val name: String,
    val emoji: String,
    val group: String,
    val traits: List<Trait>,
    val interview: Interview? = null,
) {
    val shortName: String get() = name.substringAfterLast(' ')

    fun has(fact: String): Boolean =
        group.equals(fact, ignoreCase = true) || traits.any { it.text.equals(fact, ignoreCase = true) }
}

/** What the detective does with a found clue. */
@Serializable
enum class Task {
    /** Cross out everyone who does NOT have the fact. */
    @SerialName("cross") CROSS,
    /** Tap everyone who has the fact. They become the ones to watch in questioning. */
    @SerialName("mark") MARK,
}

/**
 * One step of the investigation. A lead starts from something already found ([from]),
 * asks the detective to think ([question]), and the right answer reveals the next clue.
 * The clue then rules people out by one fact on their cards ([fact]).
 */
@Serializable
data class Lead(
    val from: String,
    val question: String,
    val options: List<String>,
    val answer: Int,
    /** Why each wrong option is wrong. Null for the right one. */
    val wrongWhy: List<String?>,
    /** Shown after the right answer, just before the clue appears. */
    val reveal: String,
    val emoji: String,
    val name: String,
    /** What the clue is. Key words in **bold**. */
    val found: String,
    val task: Task,
    /** The fact the clue checks, written exactly as on the people's cards, e.g. "Family". */
    val fact: String,
    val ask: String,
    val why: String,
    /** Short label for the evidence card shown in questioning. */
    val card: String,
)

@Serializable
data class CaseFile(
    val id: String,
    val title: String,
    /** The Commissioner's two lines on the phone. `{name}` becomes the detective's name. */
    val call: List<String>,
    /** One line under "You've arrived". */
    val arrival: String,
    val briefing: List<BriefingLine>,
    val examination: Examination,
    /** Everyone who was there. The detective narrows them down to [suspects]. */
    val people: List<Person>,
    val leads: List<Lead>,
    /** The id of the person who did it. */
    val culprit: String,
    val confession: String,
    /** What happens if the culprit escapes. */
    val escapeStory: String,
) {
    /** The people left after every lead: the ones with an interview, in card order. */
    val suspects: List<Person> get() = people.filter { it.interview != null }
    val culpritIndex: Int get() = suspects.indexOfFirst { it.id == culprit }

    fun callLines(name: String): List<String> = call.map { it.replace("{name}", name) }

    fun person(id: String): Person = people.first { it.id == id }

    /**
     * Who the detective must pick for [lead], given who is still in. Cross: everyone without
     * the fact. Mark: everyone with it.
     */
    fun expected(lead: Int, stillIn: List<Person>): Set<String> {
        val l = leads[lead]
        return stillIn.filter { if (l.task == Task.CROSS) !it.has(l.fact) else it.has(l.fact) }.map { it.id }.toSet()
    }

    /**
     * Everything that makes a case fair and playable. Returns the problems, empty if none.
     * Run on every case before it reaches players: in the tests, and when the app downloads one.
     */
    fun problems(): List<String> = buildList {
        fun check(ok: Boolean, problem: String) { if (!ok) add("$id: $problem") }

        check(id.matches(Regex("[a-z0-9-]+")), "id must be lowercase letters, digits and dashes")
        check(call.isNotEmpty(), "the Commissioner needs something to say")
        check(briefing.isNotEmpty(), "the briefing is empty")
        val exam = examination
        check(exam.spots.size == 3, "the examination needs 3 spots")
        check(exam.spots.all { it.x in 0f..1f && it.y in 0f..1f }, "spot positions are fractions from 0 to 1")
        check(exam.answer in exam.options.indices, "the examination answer must be one of its options")

        check(people.size in 5..10, "a case has 5 to 10 people (8 is best)")
        check(people.map { it.id }.toSet().size == people.size, "people ids must be unique")
        check(people.all { it.traits.isNotEmpty() }, "every person needs facts on their card")
        check(leads.size in 1..4, "a case has 1 to 4 leads (3 is best)")

        var stillIn = people
        leads.forEachIndexed { i, l ->
            val n = i + 1
            check(l.options.size in 2..4, "lead $n needs 2 to 4 options")
            check(l.answer in l.options.indices, "lead $n: the answer must be one of its options")
            check(l.wrongWhy.size == l.options.size, "lead $n: wrongWhy needs one entry per option")
            check(l.wrongWhy.indices.all { (it == l.answer) == (l.wrongWhy[it] == null) }, "lead $n: explain every wrong option, and only those")
            check(people.any { it.has(l.fact) }, "lead $n: nobody's card says \"${l.fact}\"")
            val picks = expected(i, stillIn)
            when (l.task) {
                Task.CROSS -> {
                    check(picks.isNotEmpty(), "lead $n rules nobody out")
                    check(culprit !in picks, "lead $n rules out the culprit")
                    stillIn = stillIn.filter { it.id !in picks }
                }
                Task.MARK -> {
                    check(picks.isNotEmpty(), "lead $n marks nobody")
                    check(culprit in picks, "lead $n must mark the culprit")
                }
            }
        }
        check(stillIn.map { it.id } == suspects.map { it.id }, "the people left after the leads ${stillIn.map { it.id }} must be exactly the ones with an interview ${suspects.map { it.id }}")
        check(suspects.size in 2..4, "2 to 4 suspects should be left to question")

        val bad = suspects.find { it.id == culprit }
        check(bad != null, "the culprit must be one of the suspects")
        suspects.forEach { s ->
            val iv = s.interview!!
            check(iv.replies.size == leads.size, "${s.id} needs a reply slot for every lead")
            leads.indices.forEach { i ->
                check(iv.replies.getOrNull(i) != null || iv.gotcha(i) != null, "${s.id} says nothing about lead ${i + 1}")
            }
            if (s.id == culprit) {
                check(iv.gotchas.any { it != null }, "the culprit must be catchable with at least one gotcha")
            } else {
                check(iv.gotchas.all { it == null }, "only the culprit can be caught lying (${s.id})")
                check(!iv.release.isNullOrBlank(), "${s.id} needs a release line")
            }
        }
    }
}

/** Today's case and its number (#1 on launch day). */
class PlayableCase(val number: Int, val file: CaseFile)
