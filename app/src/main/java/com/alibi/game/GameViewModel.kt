package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibi.engine.board.BoardState
import com.alibi.engine.board.GuessResult
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.Interrogation
import com.alibi.engine.cases.Mood
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.FrontPage
import com.alibi.engine.score.Newspaper
import java.time.LocalDate
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { Id, Desk, Call, Scene, Wall, Room, Arrest, News }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    val sfx = Sfx(application).apply { enabled = store.soundOn }
    private val today = LocalDate.now().toEpochDay()

    val case: PlayableCase = CaseLibrary.forDay(today)
    val file: CaseFile get() = case.file

    var name by mutableStateOf(store.name)
        private set
    var screen by mutableStateOf(if (store.name.isBlank()) Screen.Id else Screen.Desk)
        private set
    var soundOn by mutableStateOf(store.soundOn)
        private set
    var solvedCount by mutableIntStateOf(store.solvedCount)
        private set
    var streak by mutableIntStateOf(store.streak(today))
        private set
    /** Today's newspaper, once the case is finished. */
    var todaysPage by mutableStateOf(store.pageFor(today))
        private set

    val careerRank: CareerRank get() = CareerRank.forSolved(solvedCount)

    // ---- Crime scene ----
    var found by mutableStateOf(List(3) { false })
        private set
    var sceneMessage by mutableStateOf<String?>(null)
        private set
    val allFound: Boolean get() = found.all { it }

    // ---- Evidence wall ----
    var board by mutableStateOf(BoardState.start(file.groups))
        private set
    var selection by mutableStateOf<List<String>>(emptyList())
        private set
    var hintsUsed by mutableIntStateOf(0)
        private set
    var panduLine by mutableStateOf("")
        private set
    /** Notes flying into the evidence envelope. */
    var filing by mutableStateOf<Set<String>>(emptySet())
        private set
    /** Bumps every time the string snaps, to restart the shake. */
    var snapTick by mutableIntStateOf(0)
        private set
    var snapping by mutableStateOf(false)
        private set
    /** The statement just unlocked, shown as a pop-up. */
    var newStatement by mutableStateOf<Int?>(null)
        private set
    private var checking = false

    // ---- Interrogation ----
    var room by mutableStateOf(Interrogation.start(file))
        private set
    var current by mutableIntStateOf(0)
        private set
    /** The suspect who is "typing" an answer right now. */
    var typing by mutableStateOf<Int?>(null)
        private set
    var arrestArmed by mutableStateOf(false)
        private set
    var commissionerSays by mutableStateOf<String?>(null)
        private set

    // ---- The end ----
    var page by mutableStateOf<FrontPage?>(null)
        private set
    var promotedTo by mutableStateOf<CareerRank?>(null)
        private set
    private var startedAt = 0L

    fun toggleSound() {
        soundOn = !soundOn
        sfx.enabled = soundOn
        store.soundOn = soundOn
    }

    // ---- ID card and desk ----

    fun reportForDuty(input: String) {
        val clean = input.trim().replace(Regex("\\s+"), " ").take(18)
        if (clean.isEmpty()) return
        name = clean
        store.name = clean
        screen = Screen.Desk
    }

    /** Starts today's case, or a practice replay if it's already finished. */
    fun takeTheCall() {
        found = List(3) { false }
        sceneMessage = null
        board = BoardState.start(file.groups)
        selection = emptyList()
        hintsUsed = 0
        filing = emptySet()
        snapping = false
        newStatement = null
        room = Interrogation.start(file)
        current = 0
        typing = null
        arrestArmed = false
        commissionerSays = null
        page = null
        promotedTo = null
        screen = Screen.Call
    }

    fun readTodaysPaper() {
        page = todaysPage
        promotedTo = null
        screen = Screen.News
    }

    fun backToDesk() {
        screen = Screen.Desk
    }

    // ---- Phone call ----

    fun onMyWay() {
        sfx.siren()
        startedAt = System.currentTimeMillis()
        screen = Screen.Scene
    }

    // ---- Crime scene ----

    /** Called as the torch moves. Evidence within [reachPx] of the torch's centre gets bagged. */
    fun torchAt(xPx: Float, yPx: Float, widthPx: Float, heightPx: Float, reachPx: Float) {
        file.scene.evidence.forEachIndexed { i, item ->
            if (found[i]) return@forEachIndexed
            if (hypot(item.x * widthPx - xPx, item.y * heightPx - yPx) < reachPx) {
                found = found.mapIndexed { j, f -> f || j == i }
                sceneMessage = "${item.emoji} ${item.text}"
                sfx.found()
            }
        }
    }

    fun backToStation() {
        sfx.siren()
        panduLine = "Inspector $name, I pinned everything we found. Tap 4 notes that belong together and I'll tie them with string!"
        screen = Screen.Wall
    }

    // ---- Evidence wall ----

    fun toggleNote(word: String) {
        if (checking || board.isOver) return
        selection = when {
            word in selection -> selection - word
            selection.size < 4 -> selection + word
            else -> selection
        }
        sfx.pin()
        if (selection.size == 4) {
            checking = true
            viewModelScope.launch {
                delay(650)
                checkSelection()
                checking = false
            }
        }
    }

    private suspend fun checkSelection() {
        val (next, result) = board.submit(selection.toSet())
        when (result) {
            is GuessResult.Correct -> {
                sfx.success()
                filing = selection.toSet()
                delay(600)
                board = next
                selection = emptyList()
                filing = emptySet()
                panduLine = if (next.isWon) "All evidence connected! Inspector, let's go meet the suspects." else "Shabash, Inspector! That's a match. Keep going!"
                newStatement = result.groupIndex
            }
            GuessResult.AlreadyTried -> {
                panduLine = "Inspector, we already tried those four together. 🤔"
                selection = emptyList()
            }
            GuessResult.OneAway, GuessResult.Wrong -> {
                sfx.snap()
                snapping = true
                snapTick++
                delay(520)
                board = next
                selection = emptyList()
                snapping = false
                panduLine = when {
                    next.isLost -> "Inspector, my chai is finished and so is my patience! 😅 Let's take what we have to the interrogation room."
                    result == GuessResult.OneAway -> "So close, Inspector! 3 of those 4 feel right. 🤏"
                    else -> "The string snapped! Those don't belong together."
                }
            }
            GuessResult.NotFour, GuessResult.GameOver -> selection = emptyList()
        }
    }

    fun untie() {
        if (!checking) selection = emptyList()
    }

    fun askPandu() {
        val open = file.groups.indices.firstOrNull { it !in board.solvedOrder } ?: return
        hintsUsed++
        panduLine = "${file.statements[open].hint} (hint used: −5 points)"
    }

    fun dismissStatement() {
        newStatement = null
    }

    fun toInterrogation() {
        room = room.callIn(current)
        screen = Screen.Room
    }

    // ---- Interrogation ----

    fun callIn(suspect: Int) {
        if (typing != null) return
        current = suspect
        arrestArmed = false
        room = room.callIn(suspect)
    }

    fun showEvidence(statement: Int) {
        if (typing != null || room.isOver) return
        val suspect = current
        if (statement in room.shown[suspect]) return
        room = room.present(suspect, statement)
        typing = suspect
        viewModelScope.launch {
            delay(750)
            room = room.respond(suspect, statement)
            typing = null
            when (room.moods[suspect]) {
                Mood.NERVOUS -> sfx.nervous()
                Mood.RELIEVED -> sfx.success()
                Mood.CALM -> Unit
            }
        }
    }

    fun arrest() {
        if (typing != null || room.isOver) return
        if (!arrestArmed) {
            arrestArmed = true
            return
        }
        arrestArmed = false
        val suspect = current
        room = room.arrest(suspect)
        if (room.isOver) {
            finishCase()
        } else {
            sfx.snap()
            commissionerSays = file.releaseLines[suspect]
        }
    }

    private fun finishCase() {
        val minutes = ((System.currentTimeMillis() - startedAt) / 60_000L).toInt()
        val front = Newspaper.write(file, name, minutes, board, hintsUsed, room)
        page = front
        val before = careerRank
        val firstFinish = todaysPage == null
        store.saveFinished(today, front, room.caught)
        if (firstFinish) {
            todaysPage = front
            solvedCount = store.solvedCount
            streak = store.streak(today)
            promotedTo = careerRank.takeIf { it != before }
        }
        screen = Screen.Arrest
    }

    // ---- Arrest and newspaper ----

    fun readNewspaper() {
        screen = Screen.News
    }

    override fun onCleared() {
        sfx.release()
    }
}
