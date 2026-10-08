package com.alibi.game

import android.content.Context
import com.alibi.engine.score.FrontPage
import com.alibi.engine.score.Verdict

/** The detective's name, career, streak and today's newspaper. */
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

    fun pageFor(day: Long): FrontPage? {
        val headline = prefs.getString("headline_$day", null) ?: return null
        return FrontPage(
            headline = headline,
            subhead = prefs.getString("subhead_$day", "") ?: "",
            photo = prefs.getString("photo_$day", "") ?: "",
            caption = prefs.getString("caption_$day", "") ?: "",
            columns = listOf(prefs.getString("col1_$day", "") ?: "", prefs.getString("col2_$day", "") ?: ""),
            verdict = Verdict(
                points = prefs.getInt("points_$day", 0),
                stars = prefs.getInt("stars_$day", 0),
                title = prefs.getString("title_$day", "") ?: "",
            ),
            shareText = prefs.getString("share_$day", "") ?: "",
        )
    }

    /** Saves today's first finished case. Practice replays of the same day are not saved. */
    fun saveFinished(day: Long, page: FrontPage, caught: Boolean) {
        if (pageFor(day) != null) return
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        val streak = if (last == day - 1) prefs.getInt(KEY_STREAK, 0) + 1 else 1
        prefs.edit()
            .putString("headline_$day", page.headline)
            .putString("subhead_$day", page.subhead)
            .putString("photo_$day", page.photo)
            .putString("caption_$day", page.caption)
            .putString("col1_$day", page.columns.getOrElse(0) { "" })
            .putString("col2_$day", page.columns.getOrElse(1) { "" })
            .putInt("points_$day", page.verdict.points)
            .putInt("stars_$day", page.verdict.stars)
            .putString("title_$day", page.verdict.title)
            .putString("share_$day", page.shareText)
            .putLong(KEY_LAST_DAY, day)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_SOLVED, solvedCount + if (caught) 1 else 0)
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
