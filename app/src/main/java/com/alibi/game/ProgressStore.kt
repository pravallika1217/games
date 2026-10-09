package com.alibi.game

import android.content.Context
import com.alibi.engine.score.ResultCard

/** The detective's name, career, streak and today's result. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("alibi_progress", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    val solvedCount: Int get() = prefs.getInt(KEY_SOLVED, 0)

    /** Current streak: still alive if the last finished case was today or yesterday. */
    fun streak(today: Long): Int {
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        return if (last == today || last == today - 1) prefs.getInt(KEY_STREAK, 0) else 0
    }

    fun resultFor(day: Long): ResultCard? {
        val share = prefs.getString("share_$day", null) ?: return null
        return ResultCard(
            stars = prefs.getInt("stars_$day", 0),
            title = prefs.getString("title_$day", "") ?: "",
            seconds = prefs.getInt("seconds_$day", 0),
            mistakes = prefs.getInt("mistakes_$day", 0),
            caught = prefs.getBoolean("caught_$day", false),
            shareText = share,
        )
    }

    /** Saves today's first finished case. Practice replays of the same day are not saved. */
    fun saveFinished(day: Long, result: ResultCard) {
        if (resultFor(day) != null) return
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        val streak = if (last == day - 1) prefs.getInt(KEY_STREAK, 0) + 1 else 1
        prefs.edit()
            .putInt("stars_$day", result.stars)
            .putString("title_$day", result.title)
            .putInt("seconds_$day", result.seconds)
            .putInt("mistakes_$day", result.mistakes)
            .putBoolean("caught_$day", result.caught)
            .putString("share_$day", result.shareText)
            .putLong(KEY_LAST_DAY, day)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_SOLVED, solvedCount + if (result.caught) 1 else 0)
            .apply()
    }

    private companion object {
        const val KEY_NAME = "detective_name"
        const val KEY_SOUND = "sound_on"
        const val KEY_SOLVED = "cases_solved"
        const val KEY_LAST_DAY = "last_day"
        const val KEY_STREAK = "streak"
    }
}
