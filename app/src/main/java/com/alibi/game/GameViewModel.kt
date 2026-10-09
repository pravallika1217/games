package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.alibi.engine.cases.ArrestState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.CaseLibrary
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.cases.Questioning
import com.alibi.engine.cases.Reply
import com.alibi.engine.cases.SearchResult
import com.alibi.engine.cases.SearchState
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.ResultCard
import com.alibi.engine.score.Scoring
import java.time.LocalDate

enum class Screen { Name, Home, Call, Arrival, Examine, Search, Questioning, Vote, Reveal, Result }

/** Right / wrong answer bar at the bottom of the screen, like Duolingo. */
enum class Feedback { RIGHT, WRONG }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    val sfx = Sfx(application).apply { enabled = store.soundOn }
    private val today = LocalDate.now().toEpochDay()

    val case: PlayableCase = CaseLibrary.forDay(today)
    val file: CaseFile get() = case.file

    var name by mutableStateOf(store.name)
        private set
    var screen by mutableStateOf(if (store.name.isBlank()) Screen.Name else Screen.Home)
        private set
    var soundOn by mutableStateOf(store.soundOn)
        private set
    var solvedCount by mutableIntStateOf(store.solvedCount)
        private set
    var streak by mutableIntStateOf(store.streak(today))
        private set
    /** Today's result, once the case is finished. */
    var todaysResult by mutableStateOf(store.resultFor(today))
        private set
    val careerRank: CareerRank get() = CareerRank.forSolved(solvedCount)

    // ---- Examination ----
    var seenSpots by mutableStateOf<Set<Int>>(emptySet())
        private set
    var examPick by mutableStateOf<Int?>(null)
        private set
    var examRuledOut by mutableStateOf<Set<Int>>(emptySet())
        private set
    var examFeedback by mutableStateOf<Feedback?>(null)
        private set
    private var examMistakes = 0

    // ---- Search ----
    var search by mutableStateOf(SearchState(file))
        private set
    /** What the last search found, shown above the room. */
    var searchMessage by mutableStateOf<String?>(null)
        private set
    var searchMessageTick by mutableIntStateOf(0)
        private set
    /** The place Pandu points at after a hint. */
    var hinted by mutableStateOf<String?>(null)
        private set
    /** The clue card open right now ("CLUE FOUND… who does this point to?"). */
    var openClue by mutableStateOf<Int?>(null)
        private set
    var clueRuledOut by mutableStateOf<Set<Int>>(emptySet())
        private set
    var clueSolved by mutableStateOf(false)
        private set
    /** Suspects who just got a new red string, to make their card glow. */
    var newString by mutableStateOf<Int?>(null)
        private set

    // ---- Questioning ----
    var questioning by mutableStateOf(Questioning(file))
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var lastReply by mutableStateOf<Reply?>(null)
        private set
    /** Bumps on every caught lie, to show the GOTCHA! stamp. */
    var gotchaTick by mutableIntStateOf(0)
        private set

    // ---- Arrest ----
    var arrest by mutableStateOf(ArrestState(file))
        private set
    var votePick by mutableStateOf<Int?>(null)
        private set
    var lastArrested by mutableStateOf<Int?>(null)
        private set
    var result by mutableStateOf<ResultCard?>(null)
        private set
    private var startedAt = 0L

    fun toggleSound() {
        soundOn = !soundOn
        sfx.enabled = soundOn
        store.soundOn = soundOn
    }

    // ---- Name and home ----

    fun saveName(input: String) {
        val clean = input.trim().replace(Regex("\\s+"), " ").take(16)
        if (clean.isEmpty()) return
        name = clean
        store.name = clean
        screen = Screen.Home
    }

    fun changeName() {
        screen = Screen.Name
    }

    fun goHome() {
        screen = Screen.Home
    }

    /** Starts today's case from the phone call (or a practice replay if it's already done). */
    fun play() {
        seenSpots = emptySet(); examPick = null; examRuledOut = emptySet(); examFeedback = null; examMistakes = 0
        search = SearchState(file); searchMessage = null; hinted = null; openClue = null; clueRuledOut = emptySet()
        clueSolved = false; newString = null
        questioning = Questioning(file); selected = null; lastReply = null
        arrest = ArrestState(file); votePick = null; lastArrested = null
        result = null
        sfx.pin()
        screen = Screen.Call
    }

    fun showTodaysResult() {
        result = todaysResult
        screen = Screen.Result
    }

    // ---- Call and arrival ----

    fun goToScene() {
        sfx.siren()
        startedAt = System.currentTimeMillis()
        screen = Screen.Arrival
    }

    fun examine() {
        sfx.pin()
        screen = Screen.Examine
    }

    // ---- Examination ----

    fun checkSpot(index: Int) {
        if (index in seenSpots) return
        seenSpots = seenSpots + index
        sfx.found()
    }

    val allSpotsSeen: Boolean get() = seenSpots.size == file.examination.spots.size

    fun pickExamOption(index: Int) {
        if (examFeedback == Feedback.RIGHT || index in examRuledOut) return
        examFeedback = null
        examPick = index
        sfx.pin()
    }

    fun checkExam() {
        val pick = examPick ?: return
        if (pick == file.examination.answer) {
            examFeedback = Feedback.RIGHT
            sfx.success()
        } else {
            examFeedback = Feedback.WRONG
            examMistakes++
            sfx.snap()
        }
    }

    fun retryExam() {
        examPick?.let { examRuledOut = examRuledOut + it }
        examPick = null
        examFeedback = null
    }

    fun startSearch() {
        screen = Screen.Search
    }

    // ---- Search ----

    fun searchAt(id: String) {
        if (openClue != null) return
        val (next, res) = search.search(id)
        search = next
        when (res) {
            is SearchResult.Found -> {
                sfx.found()
                openClue = res.clue
                clueRuledOut = emptySet()
                clueSolved = false
                hinted = null
            }
            is SearchResult.Nothing -> {
                sfx.snap()
                searchMessage = res.text
                searchMessageTick++
            }
            SearchResult.Ignored -> Unit
        }
    }

    fun askPandu() {
        val (next, place) = search.hint() ?: return
        search = next
        hinted = place
    }

    fun answerClue(suspect: Int) {
        val clue = openClue ?: return
        if (clueSolved || suspect in clueRuledOut) return
        val (next, right) = search.answer(clue, suspect)
        search = next
        if (right) {
            clueSolved = true
            newString = suspect
            sfx.success()
        } else {
            clueRuledOut = clueRuledOut + suspect
            sfx.snap()
        }
    }

    fun bagClue() {
        openClue = null
        clueSolved = false
        searchMessage = null
        sfx.pin()
    }

    fun startQuestioning() {
        screen = Screen.Questioning
    }

    // ---- Questioning ----

    fun selectSuspect(index: Int) {
        selected = index
        lastReply = null
        sfx.pin()
    }

    fun showClue(clue: Int) {
        val suspect = selected ?: return
        val (next, reply) = questioning.show(suspect, clue)
        questioning = next
        lastReply = reply
        if (!reply.bySuspect) {
            gotchaTick++
            sfx.nervous()
        } else {
            sfx.pin()
        }
    }

    fun goToVote() {
        votePick = null
        screen = Screen.Vote
    }

    // ---- Arrest ----

    fun pickForArrest(index: Int) {
        if (index in arrest.released) return
        votePick = index
        sfx.pin()
    }

    fun makeArrest() {
        val pick = votePick ?: return
        arrest = arrest.arrest(pick)
        lastArrested = pick
        if (arrest.caught) sfx.cuffs() else sfx.snap()
        sfx.stamp()
        if (arrest.isOver) finish()
        screen = Screen.Reveal
    }

    fun afterReveal() {
        screen = if (arrest.isOver) Screen.Result else Screen.Vote
        votePick = null
    }

    private fun finish() {
        val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        val card = Scoring.result(case.number, file, seconds, examMistakes, search, arrest)
        result = card
        if (todaysResult == null) {
            store.saveFinished(today, card)
            todaysResult = card
            solvedCount = store.solvedCount
            streak = store.streak(today)
        }
    }

    override fun onCleared() {
        sfx.release()
    }
}
