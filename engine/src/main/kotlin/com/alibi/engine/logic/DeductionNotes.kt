package com.alibi.engine.logic

enum class Mark { EMPTY, CROSS, CHECK }

/**
 * The marks a player puts on their Act 2 grid. Tapping a cell cycles empty → ✕ → ✓ → empty.
 * Placing a ✓ also crosses out the rest of that row and column, like on paper.
 */
data class DeductionNotes(private val marks: Map<Pair<Item, Item>, Mark> = emptyMap()) {

    operator fun get(a: Item, b: Item): Mark = marks[key(a, b)] ?: Mark.EMPTY

    fun cycle(a: Item, b: Item, size: Int): DeductionNotes = when (get(a, b)) {
        Mark.EMPTY -> set(a, b, Mark.CROSS)
        Mark.CROSS -> check(a, b, size)
        Mark.CHECK -> set(a, b, Mark.EMPTY)
    }

    private fun check(a: Item, b: Item, size: Int): DeductionNotes {
        val updated = marks.toMutableMap()
        updated[key(a, b)] = Mark.CHECK
        for (k in 0 until size) {
            val rowPeer = key(a, Item(b.category, k))
            val colPeer = key(Item(a.category, k), b)
            if (k != b.index && updated[rowPeer] == null) updated[rowPeer] = Mark.CROSS
            if (k != a.index && updated[colPeer] == null) updated[colPeer] = Mark.CROSS
        }
        return DeductionNotes(updated)
    }

    private fun set(a: Item, b: Item, mark: Mark): DeductionNotes =
        DeductionNotes(if (mark == Mark.EMPTY) marks - key(a, b) else marks + (key(a, b) to mark))

    private fun key(a: Item, b: Item) = if (a.category < b.category) a to b else b to a
}
