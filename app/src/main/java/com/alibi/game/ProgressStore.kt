package com.alibi.game

import android.content.Context

/** Today's finished result and the player's daily streak. */
data class DaySummary(val total: Int, val rankTitle: String, val rankEmoji: String, val shareText: String)

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("alibi_progress", Context.MODE_PRIVATE)

    fun summaryFor(epochDay: Long): DaySummary? {
        val share = prefs.getString("share_$epochDay", null) ?: return null
        return DaySummary(
            total = prefs.getInt("total_$epochDay", 0),
            rankTitle = prefs.getString("rank_$epochDay", "") ?: "",
            rankEmoji = prefs.getString("emoji_$epochDay", "") ?: "",
            shareText = share,
        )
    }

    /** Current streak: still alive if the last solved case was today or yesterday. */
    fun streak(today: Long): Int {
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        return if (last == today || last == today - 1) prefs.getInt(KEY_STREAK, 0) else 0
    }

    fun save(epochDay: Long, summary: DaySummary) {
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        val streak = when (last) {
            epochDay -> prefs.getInt(KEY_STREAK, 1)
            epochDay - 1 -> prefs.getInt(KEY_STREAK, 0) + 1
            else -> 1
        }
        prefs.edit()
            .putInt("total_$epochDay", summary.total)
            .putString("rank_$epochDay", summary.rankTitle)
            .putString("emoji_$epochDay", summary.rankEmoji)
            .putString("share_$epochDay", summary.shareText)
            .putLong(KEY_LAST_DAY, epochDay)
            .putInt(KEY_STREAK, streak)
            .apply()
    }

    private companion object {
        const val KEY_LAST_DAY = "last_day"
        const val KEY_STREAK = "streak"
    }
}
