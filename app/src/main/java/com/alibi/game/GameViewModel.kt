package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.alibi.engine.board.BoardState
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.fermi.FermiOutcome
import com.alibi.engine.score.ScoreCard
import com.alibi.engine.score.Scoring
import java.time.LocalDate
import kotlin.random.Random

enum class Screen { Home, Words, Suspects, Bonus, Result }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    private val today = LocalDate.now().toEpochDay()

    val case: PlayableCase = CaseLibrary.forDay(today)

    var screen by mutableStateOf(Screen.Home)
        private set

    // Step 1: words
    var board by mutableStateOf(BoardState.start(case.file.groups, Random(case.number.toLong())))
        private set
    var selection by mutableStateOf<Set<String>>(emptySet())
        private set
    var boardMessage by mutableStateOf<String?>(null)
        private set
    /** The clue just unlocked, shown as a pop-up card. */
    var newClue by mutableStateOf<String?>(null)
        private set

    // Step 2: who did it?
    var catch by mutableStateOf(case.startCatch())
        private set

    // Bonus
    var bonusOutcome by mutableStateOf<FermiOutcome?>(null)
        private set

    // Result
    var scoreCard by mutableStateOf<ScoreCard?>(null)
        private set
    var summary by mutableStateOf(store.summaryFor(today))
        private set
    var streak by mutableStateOf(store.streak(today))
        private set

    fun isClueUnlocked(index: Int): Boolean = index in board.solvedOrder

    fun openCase() {
        screen = if (summary != null) Screen.Result else Screen.Words
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
            is GuessResult.Correct -> null
            GuessResult.OneAway -> "So close… just one word wrong! 🤏"
            GuessResult.Wrong -> "Not a group. Try again!"
            GuessResult.AlreadyTried -> "You already tried that."
            GuessResult.NotFour -> "Pick exactly 4 words."
            GuessResult.GameOver -> null
        }
        if (result is GuessResult.Correct) newClue = case.file.clues[result.groupIndex]
        if (result is GuessResult.Correct || next.isOver) selection = emptySet()
        if (next.isLost) boardMessage = "Out of tries! You keep the clues you found."
    }

    fun dismissClue() {
        newClue = null
    }

    fun goToSuspects() {
        screen = Screen.Suspects
    }

    fun toggleInnocent(suspect: Int) {
        catch = catch.toggleCleared(suspect)
    }

    fun accuse(suspect: Int) {
        catch = catch.accuse(suspect)
    }

    fun goToBonus() {
        screen = Screen.Bonus
    }

    fun submitBonus(guess: Double) {
        if (bonusOutcome == null) bonusOutcome = case.bonusOutcome(guess)
    }

    fun finishCase() {
        val card = Scoring.score(case.number, board, catch, bonusOutcome)
        val daySummary = DaySummary(card.total, card.rank.title, card.rank.emoji, card.shareText)
        store.save(today, daySummary)
        scoreCard = card
        summary = daySummary
        streak = store.streak(today)
        screen = Screen.Result
    }

    val nextCaseNumber: Int get() = case.number + 1
}
