package com.alibi.engine.board

import kotlin.random.Random

/** Four notes on the evidence wall that a witness asks for, e.g. "Sweets". */
data class EvidenceGroup(val title: String, val items: List<String>) {
    init { require(items.size == 4) { "A group needs exactly 4 items" } }
}

sealed interface TapResult {
    /** Right note: pinned with red string. */
    data object Pinned : TapResult
    /** Right note, and it was the 4th: the witness talks. */
    data class TaskDone(val groupIndex: Int) : TapResult
    /** Wrong note for this witness. Costs a cup of Pandu's chai. */
    data object Wrong : TapResult
    /** Wrong note, and the last cup of chai. The wall is over. */
    data object OutOfPatience : TapResult
    /** Already pinned, already shaken, or the wall is over. Nothing happens. */
    data object Ignored : TapResult
}

/**
 * The evidence wall. Witnesses come one at a time, and each tells you exactly what to find:
 * "Find the 4 SWEETS on the wall." Tap the right notes to pin them; wrong taps cost chai.
 *
 * Immutable: every action returns a new state, which keeps it easy to test and to show in Compose.
 */
data class WallState(
    val groups: List<EvidenceGroup>,
    /** Notes still on the wall, in display order. */
    val tiles: List<String>,
    /** Which witness is asking right now. Equal to `groups.size` when the wall is done. */
    val task: Int = 0,
    /** Notes pinned for the current witness, in tap order. */
    val picked: List<String> = emptyList(),
    val solved: List<Int> = emptyList(),
    val missed: List<Int> = emptyList(),
    val mistakes: Int = 0,
    val maxMistakes: Int = 4,
    /** Wrong notes already tapped for the current witness; tapping them again is free. */
    val shaken: Set<String> = emptySet(),
    val hintsUsed: Int = 0,
    /** Notes Pandu pointed at for the current witness. */
    val hinted: Set<String> = emptySet(),
) {
    val isOver: Boolean get() = task >= groups.size
    val current: EvidenceGroup? get() = groups.getOrNull(task)
    val mistakesLeft: Int get() = maxMistakes - mistakes

    fun tap(word: String): Pair<WallState, TapResult> {
        val group = current ?: return this to TapResult.Ignored
        if (word !in tiles || word in picked || word in shaken) return this to TapResult.Ignored

        if (word in group.items) {
            val nowPicked = picked + word
            if (nowPicked.size < group.items.size) return copy(picked = nowPicked) to TapResult.Pinned
            return copy(
                tiles = tiles - group.items.toSet(),
                task = task + 1,
                picked = emptyList(),
                solved = solved + task,
                shaken = emptySet(),
                hinted = emptySet(),
            ) to TapResult.TaskDone(task)
        }

        val next = copy(mistakes = mistakes + 1, shaken = shaken + word)
        if (next.mistakes < maxMistakes) return next to TapResult.Wrong
        return next.copy(
            task = groups.size,
            picked = emptyList(),
            missed = missed + (task until groups.size),
        ) to TapResult.OutOfPatience
    }

    /** Pandu points at one right note for the current witness. Returns null if there's nothing to point at. */
    fun hint(): WallState? {
        val group = current ?: return null
        val word = group.items.firstOrNull { it !in picked && it !in hinted } ?: return null
        return copy(hintsUsed = hintsUsed + 1, hinted = hinted + word)
    }

    fun shuffled(random: Random = Random.Default): WallState = copy(tiles = tiles.shuffled(random))

    companion object {
        /** [decoys] are trick notes that belong to no witness, so the last witness still has to think. */
        fun start(groups: List<EvidenceGroup>, decoys: List<String> = emptyList(), random: Random = Random.Default): WallState {
            val all = groups.flatMap { it.items } + decoys
            require(all.toSet().size == all.size) { "Every note on the wall must be unique" }
            return WallState(groups = groups, tiles = all.shuffled(random))
        }
    }
}
