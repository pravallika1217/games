package com.alibi.engine.board

import kotlin.random.Random

/** One hidden group on the Step 1 word board. [level] 0–3 runs from easiest (yellow) to trickiest (purple). */
data class EvidenceGroup(val title: String, val items: List<String>, val level: Int) {
    init {
        require(items.size == 4) { "A group needs exactly 4 items" }
        require(level in 0..3) { "Level must be 0..3" }
    }
}

sealed interface GuessResult {
    data class Correct(val groupIndex: Int) : GuessResult
    data object OneAway : GuessResult
    data object Wrong : GuessResult
    data object AlreadyTried : GuessResult
    data object NotFour : GuessResult
    data object GameOver : GuessResult
}

/**
 * Step 1, the Connections-style word board. Immutable: every action returns a new state,
 * which keeps it easy to test and easy to show in Compose.
 */
data class BoardState(
    val groups: List<EvidenceGroup>,
    /** Unsolved tiles, in display order. */
    val tiles: List<String>,
    /** Group indexes, in the order they were solved. */
    val solvedOrder: List<Int> = emptyList(),
    val mistakes: Int = 0,
    val maxMistakes: Int = 4,
    /** For each guess, the level of every tile picked. Drives the share card. */
    val guessHistory: List<List<Int>> = emptyList(),
    private val tried: Set<Set<String>> = emptySet(),
) {
    val isWon: Boolean get() = solvedOrder.size == groups.size
    val isLost: Boolean get() = !isWon && mistakes >= maxMistakes
    val isOver: Boolean get() = isWon || isLost
    val mistakesLeft: Int get() = maxMistakes - mistakes

    fun groupOf(tile: String): Int = groups.indexOfFirst { tile in it.items }

    fun submit(selection: Set<String>): Pair<BoardState, GuessResult> {
        if (isOver) return this to GuessResult.GameOver
        if (selection.size != 4 || !tiles.containsAll(selection)) return this to GuessResult.NotFour
        if (selection in tried) return this to GuessResult.AlreadyTried

        val counts = selection.groupingBy { groupOf(it) }.eachCount()
        val history = guessHistory + listOf(selection.map { groups[groupOf(it)].level })
        val base = copy(guessHistory = history, tried = tried + setOf(selection))

        val solvedGroup = counts.entries.singleOrNull { it.value == 4 }?.key
        if (solvedGroup != null) {
            return base.copy(
                tiles = tiles - selection,
                solvedOrder = solvedOrder + solvedGroup,
            ) to GuessResult.Correct(solvedGroup)
        }
        val result = if (counts.values.max() == 3) GuessResult.OneAway else GuessResult.Wrong
        return base.copy(mistakes = mistakes + 1) to result
    }

    fun shuffled(random: Random = Random.Default): BoardState = copy(tiles = tiles.shuffled(random))

    companion object {
        fun start(groups: List<EvidenceGroup>, random: Random = Random.Default): BoardState {
            val all = groups.flatMap { it.items }
            require(all.toSet().size == all.size) { "Every tile on the board must be unique" }
            return BoardState(groups = groups, tiles = all.shuffled(random))
        }
    }
}
