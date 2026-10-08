package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.fermi.FermiOutcome
import com.alibi.engine.fermi.FermiQuestion
import com.alibi.engine.fermi.FermiScorer
import com.alibi.engine.logic.Category
import com.alibi.engine.logic.Clue
import com.alibi.engine.logic.Item
import com.alibi.engine.logic.LogicPuzzle
import com.alibi.engine.logic.PuzzleGenerator

/**
 * A suspect, weapon or room. [ref] is how it reads inside a sentence ("the Garland", "Room 204");
 * [at] is how a room reads as a location ("in the Kitchen", "on the Terrace").
 */
data class Entity(val name: String, val emoji: String, val ref: String, val at: String = "in $ref") {
    val label: String get() = "$emoji $name"
}

fun person(name: String, emoji: String) = Entity(name, emoji, name)
fun thing(name: String, emoji: String, ref: String = "the $name") = Entity(name, emoji, ref)
fun place(name: String, emoji: String, ref: String = "the $name", preposition: String = "in") =
    Entity(name, emoji, ref, "$preposition $ref")

data class CaseTheme(val suspects: List<Entity>, val weapons: List<Entity>, val rooms: List<Entity>) {
    init { require(suspects.size == weapons.size && weapons.size == rooms.size) }

    val size: Int get() = suspects.size

    fun entities(category: Category): List<Entity> = when (category) {
        Category.SUSPECT -> suspects
        Category.WEAPON -> weapons
        Category.ROOM -> rooms
    }

    fun entity(item: Item): Entity = entities(item.category)[item.index]
}

/**
 * What a writer authors for one day: the story, the 16 board words and the Fermi question.
 * The logic puzzle is generated from the theme, so nobody has to hand-write logic grids.
 */
data class CaseFile(
    val title: String,
    val intro: String,
    val theme: CaseTheme,
    /** Group `i` unlocks bonus clue `i` in Act 2. */
    val groups: List<EvidenceGroup>,
    val fermi: FermiQuestion,
    /** Final clue when the crime room is revealed. `{room}` becomes e.g. "in the Kitchen". */
    val roomReveal: String = "The body was found {room}.",
    /** Final clue when the weapon is revealed. `{weapon}` is filled in. */
    val weaponReveal: String = "{weapon} was found next to the body.",
    val logicSeed: Long,
) {
    init { require(groups.size == 4) { "A case needs 4 evidence groups" } }
}

data class Accusation(val suspect: Int, val weapon: Int, val room: Int)

data class AccusationResult(
    val who: Boolean,
    val what: Boolean,
    val where: Boolean,
    val culprit: Int,
    val weapon: Int,
    val room: Int,
) {
    val solved: Boolean get() = who && what && where
}

/** A case ready to play: the authored file plus its generated puzzle, with every clue written out. */
class PlayableCase(val number: Int, val file: CaseFile, val puzzle: LogicPuzzle) {
    val theme: CaseTheme get() = file.theme
    val clues: List<String> = puzzle.clues.map { ClueWriter.write(it, theme) }
    val bonusClues: List<String> = puzzle.bonusClues.map { ClueWriter.write(it, theme) }
    val finalClue: String =
        if (puzzle.revealsRoom) file.roomReveal.replace("{room}", theme.rooms[puzzle.crimeRoom].at)
        else ClueWriter.capitalize(file.weaponReveal.replace("{weapon}", theme.weapons[puzzle.crimeWeapon].ref))

    fun accuse(accusation: Accusation) = AccusationResult(
        who = accusation.suspect == puzzle.culprit,
        what = accusation.weapon == puzzle.crimeWeapon,
        where = accusation.room == puzzle.crimeRoom,
        culprit = puzzle.culprit,
        weapon = puzzle.crimeWeapon,
        room = puzzle.crimeRoom,
    )

    fun fermiOutcome(guess: Double): FermiOutcome = FermiScorer.outcome(guess, file.fermi)

    companion object {
        fun build(number: Int, file: CaseFile, seed: Long = file.logicSeed): PlayableCase {
            val puzzle = PuzzleGenerator(file.theme.size).generate(seed, bonusCount = file.groups.size)
            return PlayableCase(number, file, puzzle)
        }
    }
}

/** Turns clues into sentences a player reads. */
object ClueWriter {
    fun write(clue: Clue, theme: CaseTheme): String {
        fun ref(item: Item) = theme.entity(item).ref
        fun at(item: Item) = theme.entity(item).at
        val sentence = when (clue) {
            is Clue.Together -> {
                val (a, b) = ordered(clue.a, clue.b)
                when (a.category to b.category) {
                    Category.SUSPECT to Category.WEAPON -> "${ref(a)} was holding ${ref(b)}."
                    else -> "${ref(a)} was ${at(b)}."
                }
            }
            is Clue.Apart -> {
                val (a, b) = ordered(clue.a, clue.b)
                when (a.category to b.category) {
                    Category.SUSPECT to Category.WEAPON -> "${ref(a)} never touched ${ref(b)}."
                    Category.SUSPECT to Category.ROOM -> "${ref(a)} was never seen ${at(b)}."
                    else -> "${ref(a)} was not ${at(b)}."
                }
            }
            is Clue.EitherOr -> {
                val first = ref(clue.first)
                val second = ref(clue.second)
                val target = ref(clue.target)
                when (clue.first.category to clue.target.category) {
                    Category.SUSPECT to Category.WEAPON -> "$target belonged to either $first or $second."
                    Category.SUSPECT to Category.ROOM -> "Either $first or $second was ${at(clue.target)}."
                    Category.ROOM to Category.WEAPON -> "$target was either ${at(clue.first)} or ${at(clue.second)}."
                    else -> "$target goes with either $first or $second."
                }
            }
        }
        return capitalize(sentence)
    }

    fun capitalize(text: String) = text.replaceFirstChar { it.uppercaseChar() }

    private fun ordered(a: Item, b: Item) = if (a.category < b.category) a to b else b to a
}
