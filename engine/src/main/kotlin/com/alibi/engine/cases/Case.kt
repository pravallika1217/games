package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup

/** A suspect, the alibi they give you, and what they say when your evidence means nothing to them. */
data class Suspect(
    val name: String,
    val emoji: String,
    val alibi: String,
    val smallTalk: List<String>,
) {
    init { require(smallTalk.isNotEmpty()) { "$name needs at least one small-talk line" } }

    /** "Cousin Vikram" → "Vikram", for tight spaces like the chair labels. */
    val shortName: String get() = name.substringAfterLast(' ')
}

enum class Mood { CALM, NERVOUS, RELIEVED }

/** How a suspect reacts when shown a statement, and what the detective writes down. */
data class Reaction(val mood: Mood, val line: String, val note: String)

/** A witness statement, unlocked by connecting one group on the evidence wall. */
data class Statement(val label: String, val text: String, val hint: String)

/** Something in the dark crime scene. Positions are fractions of the scene's width and height. */
data class SceneItem(val emoji: String, val x: Float, val y: Float, val text: String = "")

data class CrimeScene(
    val place: String,
    /** The 3 pieces of evidence the torch can find. */
    val evidence: List<SceneItem>,
    /** Background objects, there to make the search feel real. */
    val decor: List<SceneItem>,
) {
    init { require(evidence.size == 3) { "A crime scene hides 3 pieces of evidence" } }
}

/** One daily case, written by a person so the story, the wall and the suspects all fit together. */
data class CaseFile(
    val title: String,
    /** Used in the newspaper headline, e.g. "Sangeet murder". */
    val headlineSubject: String,
    /** The Commissioner's midnight call. `{name}` becomes the detective's name. */
    val call: String,
    /** One line about what happened, the first note in the notebook. */
    val crime: String,
    val scene: CrimeScene,
    /** Evidence wall groups. Connecting group `i` unlocks `statements[i]`. */
    val groups: List<EvidenceGroup>,
    val statements: List<Statement>,
    val suspects: List<Suspect>,
    /** Index into [suspects]. */
    val culprit: Int,
    /** `reactions[statement][suspect]`: only the reactions that matter. */
    val reactions: Map<Int, Map<Int, Reaction>>,
    /** What the Commissioner says when you arrest an innocent suspect. */
    val releaseLines: Map<Int, String>,
    val confession: String,
    /** What happens if the culprit escapes, ending in their confession. */
    val escapeStory: String,
    /** The detective's quote in the newspaper, e.g. "The sunglasses gave him away". */
    val caughtQuote: String,
) {
    init {
        require(suspects.size == 3) { "A case has 3 suspects" }
        require(culprit in suspects.indices) { "Culprit must be one of the suspects" }
        require(groups.size == 3) { "A case has 3 evidence groups" }
        require(statements.size == groups.size) { "Every group needs a statement" }
        require(reactions.keys.all { it in statements.indices }) { "Reaction for an unknown statement" }
        require(reactions.values.all { r -> r.keys.all { it in suspects.indices } }) { "Reaction for an unknown suspect" }
        require(suspects.indices.filter { it != culprit }.all { it in releaseLines }) { "Every innocent suspect needs a release line" }
    }

    fun callText(name: String): String = call.replace("{name}", name)
}

class PlayableCase(val number: Int, val file: CaseFile)
