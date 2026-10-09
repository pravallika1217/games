package com.alibi.engine.cases

/**
 * Following the leads. For each lead: answer the question (a wrong answer is a mistake, try
 * again), the clue appears, then pick the people it rules out or marks.
 * Immutable: every action returns a new state.
 */
data class Investigation(
    val file: CaseFile,
    /** Leads finished so far. */
    val done: Int = 0,
    /** The question for the current lead is answered and its clue is showing. */
    val clueOpen: Boolean = false,
    /** Wrong options picked for the current lead's question. */
    val wrongOptions: Set<Int> = emptySet(),
    val crossedOut: Set<String> = emptySet(),
    val marked: Set<String> = emptySet(),
    val wrongGuesses: Int = 0,
    val wrongPicks: Int = 0,
) {
    val current: Int? get() = done.takeIf { it < file.leads.size }
    val isDone: Boolean get() = current == null
    val mistakes: Int get() = wrongGuesses + wrongPicks
    val stillIn: List<Person> get() = file.people.filter { it.id !in crossedOut }

    /** Answers the current lead's question. Right opens the clue. */
    fun guess(option: Int): Pair<Investigation, Boolean> {
        val lead = current ?: return this to false
        if (clueOpen || option in wrongOptions) return this to false
        if (option == file.leads[lead].answer) return copy(clueOpen = true) to true
        return copy(wrongOptions = wrongOptions + option, wrongGuesses = wrongGuesses + 1) to false
    }

    /** Checks who the detective picked for the open clue. Right finishes the lead. */
    fun pick(ids: Set<String>): Pair<Investigation, Boolean> {
        val lead = current ?: return this to false
        if (!clueOpen) return this to false
        if (ids != file.expected(lead, stillIn)) return copy(wrongPicks = wrongPicks + 1) to false
        val l = file.leads[lead]
        return copy(
            done = done + 1,
            clueOpen = false,
            wrongOptions = emptySet(),
            crossedOut = if (l.task == Task.CROSS) crossedOut + ids else crossedOut,
            marked = if (l.task == Task.MARK) ids else marked,
        ) to true
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
    fun show(suspect: Int, lead: Int): Pair<Questioning, Reply> {
        val iv = file.suspects[suspect].interview!!
        val next = copy(shown = shown.mapIndexed { i, set -> if (i == suspect) set + lead else set })
        val gotcha = iv.gotcha(lead)
        return if (gotcha != null) next.copy(lying = lying + suspect) to Reply(bySuspect = false, text = gotcha)
        else next to Reply(bySuspect = true, text = iv.replies[lead] ?: "…")
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
        if (suspect == file.culpritIndex) return copy(caught = true)
        return copy(warrants = warrants - 1, released = released + suspect)
    }
}
