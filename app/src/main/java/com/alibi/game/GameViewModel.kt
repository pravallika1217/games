package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.alibi.engine.board.BoardState
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.Accusation
import com.alibi.engine.cases.AccusationResult
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.fermi.FermiOutcome
import com.alibi.engine.logic.Category
import com.alibi.engine.logic.DeductionNotes
import com.alibi.engine.logic.Item
import com.alibi.engine.score.ScoreCard
import com.alibi.engine.score.Scoring
import java.time.LocalDate
import kotlin.random.Random

enum class Screen { Home, Board, Deduction, Fermi, Result }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    private val today = LocalDate.now().toEpochDay()

    val case: PlayableCase = CaseLibrary.forDay(today)

    var screen by mutableStateOf(Screen.Home)
        private set

    // Act 1
    var board by mutableStateOf(BoardState.start(case.file.groups, Random(case.number.toLong())))
        private set
    var selection by mutableStateOf<Set<String>>(emptySet())
        private set
    var boardMessage by mutableStateOf<String?>(null)
        private set

    // Act 2
    var notes by mutableStateOf(DeductionNotes())
        private set
    var accused by mutableStateOf<Map<Category, Int>>(emptyMap())
        private set
    var accusation by mutableStateOf<AccusationResult?>(null)
        private set

    // Act 3
    var fermiInput by mutableStateOf("")
        private set
    var fermiOutcome by mutableStateOf<FermiOutcome?>(null)
        private set

    // Result
    var scoreCard by mutableStateOf<ScoreCard?>(null)
        private set
    var summary by mutableStateOf(store.summaryFor(today))
        private set
    var streak by mutableStateOf(store.streak(today))
        private set

    val bonusUnlocked: List<Boolean>
        get() = case.file.groups.indices.map { it in board.solvedOrder }

    fun openCase() {
        screen = if (summary != null) Screen.Result else Screen.Board
    }

    fun backHome() {
        screen = Screen.Home
    }

    fun toggleTile(tile: String) {
        if (board.isOver) return
        selection = when {
            tile in selection -> selection - tile
            selection.size < 4 -> selection + tile
            else -> selection
        }
        boardMessage = null
    }

    fun deselectAll() {
        selection = emptySet()
    }

    fun shuffle() {
        board = board.shuffled()
    }

    fun submitGuess() {
        val (next, result) = board.submit(selection)
        board = next
        boardMessage = when (result) {
            is GuessResult.Correct -> "🔓 Clue unlocked: ${case.bonusClues[result.groupIndex]}"
            GuessResult.OneAway -> "So close… one away!"
            GuessResult.Wrong -> "Not a group."
            GuessResult.AlreadyTried -> "You already tried that."
            GuessResult.NotFour -> "Pick exactly 4."
            GuessResult.GameOver -> null
        }
        if (result is GuessResult.Correct || next.isOver) selection = emptySet()
        if (next.isLost) boardMessage = "Out of mistakes. Only the clues you unlocked go with you."
    }

    fun goToDeduction() {
        screen = Screen.Deduction
    }

    fun cycleMark(a: Item, b: Item) {
        if (accusation != null) return
        notes = notes.cycle(a, b, case.theme.size)
    }

    fun pick(category: Category, index: Int) {
        if (accusation != null) return
        accused = accused + (category to index)
    }

    val canAccuse: Boolean
        get() = accusation == null && accused.size == Category.entries.size

    fun accuse() {
        if (!canAccuse) return
        accusation = case.accuse(
            Accusation(
                suspect = accused.getValue(Category.SUSPECT),
                weapon = accused.getValue(Category.WEAPON),
                room = accused.getValue(Category.ROOM),
            )
        )
    }

    fun goToFermi() {
        screen = Screen.Fermi
    }

    fun updateFermiInput(text: String) {
        if (fermiOutcome == null) fermiInput = text.filter { it.isDigit() || it == '.' }.take(12)
    }

    fun submitFermi() {
        val guess = fermiInput.toDoubleOrNull() ?: return
        if (guess <= 0 || fermiOutcome != null) return
        fermiOutcome = case.fermiOutcome(guess)
    }

    fun finishCase() {
        val result = accusation ?: return
        val fermi = fermiOutcome ?: return
        val card = Scoring.score(case.number, board, result, fermi)
        val daySummary = DaySummary(card.total, card.rank.title, card.rank.emoji, card.shareText)
        store.save(today, daySummary)
        scoreCard = card
        summary = daySummary
        streak = store.streak(today)
        screen = Screen.Result
    }

    val nextCaseNumber: Int get() = case.number + 1
}
