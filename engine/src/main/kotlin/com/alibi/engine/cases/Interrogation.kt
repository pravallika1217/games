package com.alibi.engine.cases

enum class Speaker { SUSPECT, DETECTIVE, ROOM }

data class ChatLine(val from: Speaker, val text: String)

/**
 * The interrogation room. Immutable: every action returns a new state.
 *
 * Showing evidence is two steps, [present] then [respond], so the app can show a
 * "typing…" pause in between, like a real conversation.
 */
data class Interrogation(
    val file: CaseFile,
    val chats: List<List<ChatLine>>,
    val moods: List<Mood>,
    /** Statements already shown to each suspect. */
    val shown: List<Set<Int>>,
    val notes: List<String>,
    val warrantsLeft: Int = 2,
    /** Innocent suspects arrested by mistake, then released. */
    val released: List<Int> = emptyList(),
    val caught: Boolean = false,
) {
    val isOver: Boolean get() = caught || warrantsLeft == 0

    /** Brings a suspect to the table. The first time, they sit down and give their alibi. */
    fun callIn(suspect: Int): Interrogation {
        if (chats[suspect].isNotEmpty()) return this
        val s = file.suspects[suspect]
        return withChat(
            suspect,
            ChatLine(Speaker.ROOM, "${s.name} sits down under the lamp."),
            ChatLine(Speaker.SUSPECT, s.alibi),
        )
    }

    /** The detective slides a statement across the table. */
    fun present(suspect: Int, statement: Int): Interrogation {
        if (isOver || statement in shown[suspect]) return this
        val st = file.statements[statement]
        return withChat(suspect, ChatLine(Speaker.DETECTIVE, "${st.label}: ${st.text}"))
            .copy(shown = shown.replaceAt(suspect, shown[suspect] + statement))
    }

    /** The suspect reacts to the statement they were just shown. */
    fun respond(suspect: Int, statement: Int): Interrogation {
        val reaction = file.reactions[statement]?.get(suspect)
        if (reaction == null) {
            val talk = file.suspects[suspect].smallTalk
            return withChat(suspect, ChatLine(Speaker.SUSPECT, talk[(shown[suspect].size - 1).mod(talk.size)]))
        }
        return withChat(suspect, ChatLine(Speaker.SUSPECT, reaction.line)).copy(
            moods = moods.replaceAt(suspect, reaction.mood),
            notes = if (reaction.note in notes) notes else notes + reaction.note,
        )
    }

    fun arrest(suspect: Int): Interrogation {
        if (isOver || suspect in released) return this
        if (suspect == file.culprit) return copy(caught = true)
        return copy(
            warrantsLeft = warrantsLeft - 1,
            released = released + suspect,
            moods = moods.replaceAt(suspect, Mood.RELIEVED),
            notes = notes + "${file.suspects[suspect].name}: wrongly arrested, released ✗",
        )
    }

    private fun withChat(suspect: Int, vararg lines: ChatLine) =
        copy(chats = chats.replaceAt(suspect, chats[suspect] + lines))

    companion object {
        fun start(file: CaseFile) = Interrogation(
            file = file,
            chats = file.suspects.map { emptyList() },
            moods = file.suspects.map { Mood.CALM },
            shown = file.suspects.map { emptySet() },
            notes = listOf("Case: ${file.crime}"),
        )
    }
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> = mapIndexed { i, v -> if (i == index) value else v }
