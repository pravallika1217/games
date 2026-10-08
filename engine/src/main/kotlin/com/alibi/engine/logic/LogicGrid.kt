package com.alibi.engine.logic

/**
 * The possibility grid a careful human keeps while solving: for every pair of items from two
 * categories, can they still belong together?
 *
 * [propagate] only uses deductions a person can make on paper (single candidates and
 * "A goes with B, B can't go with C, so A can't go with C"). A puzzle that [propagate] solves
 * is therefore solvable by pure logic, with no guessing.
 */
class LogicGrid private constructor(
    val size: Int,
    // cells[pair][low][high], pair 0 = suspect×weapon, 1 = suspect×room, 2 = weapon×room.
    private val cells: Array<Array<BooleanArray>>,
) {
    constructor(size: Int) : this(size, Array(3) { Array(size) { BooleanArray(size) { true } } })

    fun copy() = LogicGrid(size, Array(3) { p -> Array(size) { r -> cells[p][r].copyOf() } })

    fun isPossible(a: Item, b: Item): Boolean {
        val (pair, low, high) = slot(a, b)
        return cells[pair][low][high]
    }

    /** Rules out [a] with [b]. Returns true if this was new information. */
    fun eliminate(a: Item, b: Item): Boolean {
        val (pair, low, high) = slot(a, b)
        if (!cells[pair][low][high]) return false
        cells[pair][low][high] = false
        return true
    }

    /** Marks [a] with [b] as certain, ruling out every alternative for both. */
    fun confirm(a: Item, b: Item): Boolean {
        var changed = false
        for (k in 0 until size) {
            if (k != b.index) changed = eliminate(a, Item(b.category, k)) or changed
            if (k != a.index) changed = eliminate(Item(a.category, k), b) or changed
        }
        return changed
    }

    fun candidates(item: Item, other: Category): List<Int> =
        (0 until size).filter { isPossible(item, Item(other, it)) }

    val isSolved: Boolean
        get() = PAIRS.all { (a, b) -> (0 until size).all { candidates(Item(a, it), b).size == 1 } }

    val hasContradiction: Boolean
        get() = PAIRS.any { (a, b) ->
            (0 until size).any { candidates(Item(a, it), b).isEmpty() || candidates(Item(b, it), a).isEmpty() }
        }

    /** Writes a clue's direct consequences into the grid. Returns true if anything changed. */
    fun apply(clue: Clue): Boolean = when (clue) {
        is Clue.Together -> confirm(clue.a, clue.b)
        is Clue.Apart -> eliminate(clue.a, clue.b)
        is Clue.EitherOr -> {
            var changed = false
            for (k in 0 until size) {
                if (k != clue.first.index && k != clue.second.index) {
                    changed = eliminate(Item(clue.first.category, k), clue.target) or changed
                }
            }
            changed
        }
    }

    /**
     * Applies human-style deductions until nothing changes.
     * The number of rounds needed is a good measure of how hard the puzzle feels.
     */
    fun propagate(): Propagation {
        var rounds = 0
        while (!hasContradiction) {
            val singles = applySingles()
            val chains = applyChains()
            if (!singles && !chains) break
            rounds++
        }
        return Propagation(rounds = rounds, solved = isSolved && !hasContradiction)
    }

    /** If an item has only one possible partner in a category, it must be that one. */
    private fun applySingles(): Boolean {
        var changed = false
        for ((a, b) in PAIRS) {
            for ((from, to) in listOf(a to b, b to a)) {
                for (i in 0 until size) {
                    val item = Item(from, i)
                    val options = candidates(item, to)
                    if (options.size == 1) changed = confirm(item, Item(to, options.single())) or changed
                }
            }
        }
        return changed
    }

    /** x can only go with z if some y could go with both of them. */
    private fun applyChains(): Boolean {
        var changed = false
        for (middle in Category.entries) {
            val (a, c) = Category.entries.filter { it != middle }
            for (i in 0 until size) for (k in 0 until size) {
                val x = Item(a, i)
                val z = Item(c, k)
                if (!isPossible(x, z)) continue
                val bridged = (0 until size).any { j ->
                    val y = Item(middle, j)
                    isPossible(x, y) && isPossible(y, z)
                }
                if (!bridged) changed = eliminate(x, z) or changed
            }
        }
        return changed
    }

    private fun slot(a: Item, b: Item): Triple<Int, Int, Int> {
        require(a.category != b.category) { "Items must be from different categories" }
        val (low, high) = if (a.category < b.category) a to b else b to a
        val pair = when {
            low.category == Category.SUSPECT && high.category == Category.WEAPON -> 0
            low.category == Category.SUSPECT -> 1
            else -> 2
        }
        return Triple(pair, low.index, high.index)
    }

    data class Propagation(val rounds: Int, val solved: Boolean)

    companion object {
        val PAIRS = listOf(
            Category.SUSPECT to Category.WEAPON,
            Category.SUSPECT to Category.ROOM,
            Category.WEAPON to Category.ROOM,
        )

        /** Runs a fresh grid over [clues]. */
        fun solve(size: Int, clues: List<Clue>): Pair<LogicGrid, Propagation> {
            val grid = LogicGrid(size)
            clues.forEach { grid.apply(it) }
            return grid to grid.propagate()
        }
    }
}

/** Checks every possible solution. Used to prove a puzzle has exactly one answer. */
object BruteForce {
    fun countSolutions(size: Int, clues: List<Clue>, stopAt: Int = Int.MAX_VALUE): Int {
        val perms = permutations((0 until size).toList())
        var count = 0
        for (weapons in perms) for (rooms in perms) {
            val solution = Solution(weapons, rooms)
            if (clues.all { it.holds(solution) }) {
                count++
                if (count >= stopAt) return count
            }
        }
        return count
    }

    private fun permutations(items: List<Int>): List<List<Int>> =
        if (items.size <= 1) listOf(items)
        else items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
}
