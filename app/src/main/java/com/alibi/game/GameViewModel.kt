package com.alibi.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibi.engine.cases.ArrestState
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.Investigation
import com.alibi.engine.cases.Person
import com.alibi.engine.cases.PlayableCase
import com.alibi.engine.cases.Questioning
import com.alibi.engine.cases.Reply
import com.alibi.engine.score.CareerRank
import com.alibi.engine.score.ResultCard
import com.alibi.engine.score.Scoring
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Name, Home, Call, Arrival, Examine, People, Leads, Questioning, Vote, Reveal, Result }

/** Right / wrong answer bar at the bottom of the screen, like Duolingo. */
enum class Feedback { RIGHT, WRONG }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    private val cases = CaseRepository(application)
    val sfx = Sfx(application).apply { enabled = store.soundOn }
    private val today = LocalDate.now().toEpochDay()

    /** Today's case. Starts with what's on the phone, and may update once the newest one downloads. */
    var case: PlayableCase by mutableStateOf(cases.caseFor(today))
        private set
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

    // ---- Leads ----
    var investigation by mutableStateOf(Investigation(file))
        private set
    /** The lead whose clue card is open, or null. */
    var cardLead by mutableStateOf<Int?>(null)
        private set
    /** Who was still in when the card opened: the people listed on it. */
    var cardPeople by mutableStateOf<List<Person>>(emptyList())
        private set
    var picks by mutableStateOf<Set<String>>(emptySet())
        private set
    var pickFeedback by mutableStateOf<Feedback?>(null)
        private set
    /** The last wrong answer to the current lead's question, to say why it's wrong. */
    var lastWrongOption by mutableStateOf<Int?>(null)
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

    init {
        viewModelScope.launch {
            val changed = withContext(Dispatchers.IO) { runCatching { cases.refresh(today) }.getOrDefault(false) }
            // Only swap the case before the player has started it.
            if (changed && screen in setOf(Screen.Name, Screen.Home)) case = cases.caseFor(today)
        }
    }

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
        investigation = Investigation(file); cardLead = null; cardPeople = emptyList(); picks = emptySet(); pickFeedback = null; lastWrongOption = null
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

    fun meetPeople() {
        sfx.pin()
        screen = Screen.People
    }

    fun startLeads() {
        sfx.pin()
        screen = Screen.Leads
    }

    // ---- Leads ----

    /** Answers the current lead's question. A wrong answer is a mistake; the right one reveals the clue. */
    fun answerLead(option: Int) {
        val (next, right) = investigation.guess(option)
        if (next == investigation) return
        investigation = next
        if (right) sfx.success() else {
            lastWrongOption = option
            sfx.snap()
        }
    }

    /** Called a moment after the right answer, so the player can read the reveal line first. */
    fun openClueCard() {
        val lead = investigation.current ?: return
        if (!investigation.clueOpen || cardLead != null) return
        cardLead = lead
        cardPeople = investigation.stillIn
        picks = emptySet()
        pickFeedback = null
        sfx.found()
    }

    fun togglePick(id: String) {
        if (pickFeedback == Feedback.RIGHT) return
        picks = if (id in picks) picks - id else picks + id
        if (pickFeedback == Feedback.WRONG) pickFeedback = null
        sfx.pin()
    }

    fun checkPicks() {
        val (next, right) = investigation.pick(picks)
        investigation = next
        if (right) {
            pickFeedback = Feedback.RIGHT
            sfx.success()
        } else {
            pickFeedback = Feedback.WRONG
            sfx.snap()
        }
    }

    fun bagClue() {
        cardLead = null
        lastWrongOption = null
        picks = emptySet()
        pickFeedback = null
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

    fun showEvidence(lead: Int) {
        val suspect = selected ?: return
        val (next, reply) = questioning.show(suspect, lead)
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
        val card = Scoring.result(case.number, file, seconds, examMistakes, investigation, arrest)
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
