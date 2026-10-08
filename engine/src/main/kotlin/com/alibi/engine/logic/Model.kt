package com.alibi.engine.logic

/** The three things every case asks: who, with what, and where. */
enum class Category { SUSPECT, WEAPON, ROOM }

/** One entry of a category, e.g. the second weapon. */
data class Item(val category: Category, val index: Int)

fun suspect(index: Int) = Item(Category.SUSPECT, index)
fun weapon(index: Int) = Item(Category.WEAPON, index)
fun room(index: Int) = Item(Category.ROOM, index)

/**
 * The hidden truth of a case: suspect `s` holds weapon `weaponOf[s]` and is in room `roomOf[s]`.
 * Every suspect has exactly one weapon and one room, and no two suspects share either.
 */
data class Solution(val weaponOf: List<Int>, val roomOf: List<Int>) {
    init {
        require(weaponOf.size == roomOf.size) { "Weapons and rooms must match suspects" }
        require(weaponOf.sorted() == weaponOf.indices.toList()) { "weaponOf must be a permutation" }
        require(roomOf.sorted() == roomOf.indices.toList()) { "roomOf must be a permutation" }
    }

    val size: Int get() = weaponOf.size

    /** The suspect an item belongs to. */
    fun ownerOf(item: Item): Int = when (item.category) {
        Category.SUSPECT -> item.index
        Category.WEAPON -> weaponOf.indexOf(item.index)
        Category.ROOM -> roomOf.indexOf(item.index)
    }

    /** True when both items belong to the same suspect. */
    fun linked(a: Item, b: Item): Boolean = ownerOf(a) == ownerOf(b)
}

/** A statement about the solution that the player reads and reasons with. */
sealed interface Clue {
    fun holds(solution: Solution): Boolean

    /** [a] and [b] belong together. */
    data class Together(val a: Item, val b: Item) : Clue {
        init { require(a.category != b.category) }
        override fun holds(solution: Solution) = solution.linked(a, b)
    }

    /** [a] and [b] do not belong together. */
    data class Apart(val a: Item, val b: Item) : Clue {
        init { require(a.category != b.category) }
        override fun holds(solution: Solution) = !solution.linked(a, b)
    }

    /** [target] belongs with either [first] or [second], which share a category. */
    data class EitherOr(val first: Item, val second: Item, val target: Item) : Clue {
        init {
            require(first.category == second.category && first.index != second.index)
            require(target.category != first.category)
        }
        override fun holds(solution: Solution) =
            solution.linked(first, target) || solution.linked(second, target)
    }
}
