package com.alibi.engine.cases

/** Something you can see about a suspect, like "🕶️ Wears sunglasses". */
data class Trait(val emoji: String, val text: String)

/**
 * A suspect. [replies] is what they say when shown each clue (by clue index).
 * [gotchas] are the clues that catch them lying, with what the detective says.
 */
data class Suspect(
    val name: String,
    val emoji: String,
    /** Where they say they were. */
    val says: String,
    val traits: List<Trait>,
    val alibi: String,
    val replies: List<String>,
    val gotchas: Map<Int, String> = emptyMap(),
) {
    val shortName: String get() = name.substringAfterLast(' ')
}

/** One row of the constable's briefing at the door, e.g. "🕛 Time: around midnight". */
data class BriefingLine(val emoji: String, val label: String, val value: String, val unknown: Boolean = false)

/** A marked spot to check during the examination. Positions are fractions of the picture. */
data class ExamSpot(val title: String, val text: String, val x: Float, val y: Float)

/**
 * The first close look: the body, or for a theft, the place things were taken from.
 * After checking every spot, the detective answers one question about what happened.
 */
data class Examination(
    val title: String,
    /** Null draws a chalk outline of a body; otherwise a big picture of this object. */
    val objectEmoji: String?,
    val spots: List<ExamSpot>,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val why: String,
) {
    init { require(answer in options.indices) }
}

/** Something in the room you can search. [miss] is what you find when nothing is hidden there. */
data class Hideout(val id: String, val emoji: String, val label: String, val x: Float, val y: Float, val miss: String = "")

/**
 * A hidden clue. The [riddle] says where to look, never what you'll find.
 * Once found, the detective decides who it points to; [found] marks the key words with **bold**.
 */
data class Clue(
    val at: String,
    val emoji: String,
    val name: String,
    val riddle: String,
    val found: String,
    val pointsTo: Int,
    val why: String,
    /** Short label for the evidence card shown in questioning. */
    val card: String,
)

data class CaseFile(
    val title: String,
    val headlineSubject: String,
    /** The Commissioner's two lines on the phone. `{name}` becomes the detective's name. */
    val call: List<String>,
    /** One line under "You've arrived". */
    val arrival: String,
    val briefing: List<BriefingLine>,
    val place: String,
    val examination: Examination,
    val hideouts: List<Hideout>,
    val clues: List<Clue>,
    val suspects: List<Suspect>,
    val culprit: Int,
    /** What the Commissioner says when an innocent suspect is arrested. */
    val releaseLines: Map<Int, String>,
    val confession: String,
    /** What happens if the culprit escapes. */
    val escapeStory: String,
) {
    init {
        require(suspects.size == 3) { "$title: a case has 3 suspects" }
        require(clues.size == 3) { "$title: a case has 3 clues" }
        require(culprit in suspects.indices)
        val ids = hideouts.map { it.id }
        require(ids.toSet().size == ids.size) { "$title: hideout ids must be unique" }
        require(clues.all { it.at in ids }) { "$title: every clue must be hidden in a hideout" }
        require(clues.map { it.at }.toSet().size == clues.size) { "$title: one clue per hideout" }
        require(clues.all { it.pointsTo in suspects.indices })
        require(suspects.all { it.replies.size == clues.size }) { "$title: every suspect needs a reply for every clue" }
        require(suspects[culprit].gotchas.isNotEmpty()) { "$title: the culprit must be catchable" }
        require(suspects.indices.filter { it != culprit }.all { suspects[it].gotchas.isEmpty() }) { "$title: only the culprit can be caught lying" }
        require(suspects.indices.filter { it != culprit }.all { it in releaseLines }) { "$title: every innocent needs a release line" }
    }

    fun callLines(name: String): List<String> = call.map { it.replace("{name}", name) }
}

class PlayableCase(val number: Int, val file: CaseFile)
