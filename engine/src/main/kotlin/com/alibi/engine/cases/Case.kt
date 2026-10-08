package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.fermi.FermiOutcome
import com.alibi.engine.fermi.FermiQuestion
import com.alibi.engine.fermi.FermiScorer

/** A suspect and the alibi they give you. One of them is lying. */
data class Suspect(val name: String, val emoji: String, val alibi: String)

/**
 * One daily case. Everything here is written by a person, so the story, the word groups and
 * the clues all fit together.
 */
data class CaseFile(
    val title: String,
    /** The opening scene, shown on the home screen. */
    val intro: String,
    /** What happened, shown at the top of the suspects screen. */
    val crime: String,
    val suspects: List<Suspect>,
    /** Index into [suspects]. */
    val culprit: Int,
    /** Word groups for Step 1. Solving group `i` unlocks `clues[i]`. */
    val groups: List<EvidenceGroup>,
    val clues: List<String>,
    /** What the culprit says when caught: the "why", with a twist. */
    val confession: String,
    /** Optional bonus question. */
    val bonus: FermiQuestion,
) {
    init {
        require(suspects.size == 3) { "A case has 3 suspects" }
        require(culprit in suspects.indices) { "Culprit must be one of the suspects" }
        require(groups.size == 3) { "A case has 3 word groups" }
        require(clues.size == groups.size) { "Every group needs a clue" }
    }
}

/**
 * Step 2: who did it? The player can stamp suspects INNOCENT while thinking, then
 * accuse one. Two tries are allowed, so one wrong guess isn't game over.
 */
data class CatchState(
    val culprit: Int,
    val suspectCount: Int,
    val cleared: Set<Int> = emptySet(),
    val wrongGuesses: List<Int> = emptyList(),
    val caught: Boolean = false,
    val maxTries: Int = 2,
) {
    val isOver: Boolean get() = caught || wrongGuesses.size >= maxTries
    val triesLeft: Int get() = maxTries - wrongGuesses.size

    /** Stamp or un-stamp a suspect as innocent. Purely the player's notes. */
    fun toggleCleared(suspect: Int): CatchState {
        if (isOver || suspect in wrongGuesses) return this
        return copy(cleared = if (suspect in cleared) cleared - suspect else cleared + suspect)
    }

    fun accuse(suspect: Int): CatchState {
        if (isOver || suspect in wrongGuesses) return this
        return if (suspect == culprit) copy(caught = true)
        else copy(wrongGuesses = wrongGuesses + suspect, cleared = cleared + suspect)
    }

    /** The one suspect left after stamping the others innocent, if there is exactly one. */
    val lastOneStanding: Int?
        get() = (0 until suspectCount).filter { it !in cleared }.singleOrNull()
}

class PlayableCase(val number: Int, val file: CaseFile) {
    fun startCatch() = CatchState(culprit = file.culprit, suspectCount = file.suspects.size)
    fun bonusOutcome(guess: Double): FermiOutcome = FermiScorer.outcome(guess, file.bonus)
}
