package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibi.engine.board.TapResult
import com.alibi.engine.board.WallState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.Interrogation
import com.alibi.engine.cases.Mood
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.FrontPage
import com.alibi.engine.score.Newspaper
import java.time.LocalDate
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
    /** The clue the detective just picked up, shown on a card before it goes in the bag. */
    var inspecting by mutableStateOf<Int?>(null)
        private set
    val allFound: Boolean get() = found.all { it }

    // ---- Evidence wall ----
    var wall by mutableStateOf(WallState.start(file.groups, file.decoys))
        private set
    var panduLine by mutableStateOf("")
        private set
    /** Notes flying into the evidence envelope. */
    var filing by mutableStateOf<Set<String>>(emptySet())
        private set
    /** The wrong note that is shaking, and a counter that restarts the shake. */
    var shakeWord by mutableStateOf<String?>(null)
        private set
    var shakeTick by mutableIntStateOf(0)
        private set
    /** The statement just unlocked, shown as a pop-up. */
    var newStatement by mutableStateOf<Int?>(null)
        private set
    private var filingNow = false

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
        inspecting = null
        wall = WallState.start(file.groups, file.decoys)
        filing = emptySet()
        shakeWord = null
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

    /** The detective taps a clue they can see in the torchlight. */
    fun pickUp(index: Int) {
        if (found[index] || inspecting != null) return
        inspecting = index
        sfx.found()
    }

    fun putInBag() {
        val i = inspecting ?: return
        found = found.mapIndexed { j, f -> f || j == i }
        inspecting = null
        sfx.pin()
    }

    fun backToStation() {
        sfx.siren()
        panduLine = "Inspector $name, the witnesses are here. Each one will tell you what to find on the wall."
        screen = Screen.Wall
    }

    // ---- Evidence wall ----

    fun tapNote(word: String) {
        if (filingNow || newStatement != null) return
        val (next, result) = wall.tap(word)
        when (result) {
            TapResult.Pinned -> {
                wall = next
                sfx.pin()
                val left = 4 - next.picked.size
                panduLine = "Yes! That's one. $left more to go."
            }
            is TapResult.TaskDone -> {
                sfx.success()
                filingNow = true
                wall = wall.copy(picked = wall.picked + word)
                filing = wall.picked.toSet()
                viewModelScope.launch {
                    delay(700)
                    wall = next
                    filing = emptySet()
                    filingNow = false
                    newStatement = result.groupIndex
                    panduLine = if (next.isOver) "All witnesses have talked! Inspector, let's go meet the suspects."
                    else "Shabash, Inspector! The next witness is waiting."
                }
            }
            TapResult.Wrong -> {
                wall = next
                sfx.snap()
                shakeWord = word
                shakeTick++
                panduLine = "Not that one, Inspector! ${word.uppercase()} doesn't fit. That cost me a cup of chai ☕"
            }
            TapResult.OutOfPatience -> {
                wall = next
                sfx.snap()
                shakeWord = word
                shakeTick++
                panduLine = "Inspector, my chai is finished and so is my patience! 😅 Let's take what we have to the interrogation room."
            }
            TapResult.Ignored -> Unit
        }
    }

    fun askPandu() {
        val next = wall.hint() ?: return
        wall = next
        panduLine = "Psst… look at the glowing note 👀 (hint: −5 points)"
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
        val front = Newspaper.write(file, name, minutes, wall, room)
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
