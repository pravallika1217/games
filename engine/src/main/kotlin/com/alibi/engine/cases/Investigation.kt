package com.alibi.engine.cases

/** What happens when the detective searches a place in the room. */
sealed interface SearchResult {
    /** The clue for the current riddle is here. The detective now decides who it points to. */
    data class Found(val clue: Int) : SearchResult
    /** Wrong place. [text] is what's there instead. */
    data class Nothing(val text: String) : SearchResult
    /** Already searched, or every clue is found. */
    data object Ignored : SearchResult
}

/**
 * Searching the crime scene. One riddle at a time; each found clue is tied with red string
 * to the suspect the detective thinks it points to. Immutable: every action returns a new state.
 */
data class SearchState(
    val file: CaseFile,
    /** Clues bagged so far, in order. */
    val found: List<Int> = emptyList(),
    /** Wrong places searched for the current riddle. */
    val searched: Set<String> = emptySet(),
    val wrongSearches: Int = 0,
    /** Wrong "who does this point to?" answers. */
    val wrongAnswers: Int = 0,
    val hintsUsed: Int = 0,
    /** Red strings per suspect. */
    val strings: List<Int> = List(file.suspects.size) { 0 },
) {
    val current: Int? get() = found.size.takeIf { it < file.clues.size }
    val isDone: Boolean get() = current == null
    val wrongHere: Int get() = searched.size
    /** After 3 wrong searches on one riddle, Constable Pandu can help. */
    val canAskForHint: Boolean get() = !isDone && wrongHere >= 3

    fun search(id: String): Pair<SearchState, SearchResult> {
        val clue = current ?: return this to SearchResult.Ignored
        if (id == file.clues[clue].at) return this to SearchResult.Found(clue)
        val place = file.hideouts.first { it.id == id }
        val text = place.miss.ifBlank { "Nothing here. Read the note again." }
        if (id in searched) return this to SearchResult.Nothing(text)
        return copy(searched = searched + id, wrongSearches = wrongSearches + 1) to SearchResult.Nothing(text)
    }

    /** The detective says who the clue points to. Right: the clue is bagged and the string tied. */
    fun answer(clue: Int, suspect: Int): Pair<SearchState, Boolean> {
        if (clue != current) return this to false
        if (suspect != file.clues[clue].pointsTo) return copy(wrongAnswers = wrongAnswers + 1) to false
        return copy(
            found = found + clue,
            searched = emptySet(),
            strings = strings.mapIndexed { i, n -> if (i == suspect) n + 1 else n },
        ) to true
    }

    /** Returns the place Pandu points at, or null if there's no riddle left. */
    fun hint(): Pair<SearchState, String>? {
        val clue = current ?: return null
        return copy(hintsUsed = hintsUsed + 1) to file.clues[clue].at
    }
}

/** A line in questioning: either the suspect answering, or the detective catching a lie. */
data class Reply(val bySuspect: Boolean, val text: String)

/** Showing evidence to suspects. Catching a lie marks the suspect as lying. */
data class Questioning(
    val file: CaseFile,
    val shown: List<Set<Int>> = List(file.suspects.size) { emptySet() },
    val lying: Set<Int> = emptySet(),
) {
    fun show(suspect: Int, clue: Int): Pair<Questioning, Reply> {
        val s = file.suspects[suspect]
        val next = copy(shown = shown.mapIndexed { i, set -> if (i == suspect) set + clue else set })
        val gotcha = s.gotchas[clue]
        return if (gotcha != null) next.copy(lying = lying + suspect) to Reply(bySuspect = false, text = gotcha)
        else next to Reply(bySuspect = true, text = s.replies[clue])
    }
}

/** The arrest. Two warrants: one wrong arrest is forgiven, two and the culprit escapes. */
data class ArrestState(
    val file: CaseFile,
    val warrants: Int = 2,
    val released: List<Int> = emptyList(),
    val caught: Boolean = false,
) {
    val isOver: Boolean get() = caught || warrants == 0
    val escaped: Boolean get() = !caught && warrants == 0

    fun arrest(suspect: Int): ArrestState {
        if (isOver || suspect in released) return this
        if (suspect == file.culprit) return copy(caught = true)
        return copy(warrants = warrants - 1, released = released + suspect)
    }
}
