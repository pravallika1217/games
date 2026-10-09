package com.alibi.game

import android.content.Context
import com.alibi.engine.cases.CaseFile
import com.alibi.engine.cases.CaseJson
import com.alibi.engine.cases.CaseSchedule
import com.alibi.engine.cases.PlayableCase
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where cases come from, like Wordle or Connections: a schedule and one small JSON file per case.
 *
 * 1. Files downloaded earlier (saved on the phone, so it works offline).
 * 2. The files shipped inside the app (assets/cases, copied from content/cases).
 *
 * [refresh] downloads the newest schedule plus today's and tomorrow's cases from [baseUrl].
 * Every download is checked by the engine before it's saved, so a broken file never reaches
 * the player.
 */
class CaseRepository(private val context: Context, private val baseUrl: String = BuildConfig.CASES_URL) {
    private val cacheDir = File(context.filesDir, "cases").apply { mkdirs() }

    /** Today's case from what's on the phone. Fast; safe to call on the main thread. */
    fun caseFor(epochDay: Long): PlayableCase {
        val downloaded = cached("schedule.json")?.let { runCatching { CaseJson.schedule(it) }.getOrNull() }
        // A downloaded schedule may name a case whose file hasn't arrived yet. Then use the
        // schedule shipped in the app.
        downloaded?.let { schedule -> load(schedule, epochDay)?.let { return it } }
        return load(bundledSchedule(), epochDay) ?: error("The cases shipped in the app are broken")
    }

    /** Downloads the newest schedule and the next two days of cases. Returns true if anything new arrived. */
    fun refresh(epochDay: Long): Boolean {
        if (baseUrl.isBlank()) return false
        val text = download("schedule.json") ?: return false
        val schedule = runCatching { CaseJson.schedule(text) }.getOrNull() ?: return false
        var changed = save("schedule.json", text)
        val today = schedule.caseNumber(epochDay)
        listOf(today, today + 1).map(schedule::caseId).distinct().forEach { id ->
            val case = download("$id.json") ?: return@forEach
            if (runCatching { CaseJson.case(case) }.isSuccess) changed = save("$id.json", case) || changed
        }
        return changed
    }

    private fun load(schedule: CaseSchedule, epochDay: Long): PlayableCase? {
        val number = schedule.caseNumber(epochDay)
        val file = caseFile(schedule.caseId(number)) ?: return null
        return PlayableCase(number, file)
    }

    private fun caseFile(id: String): CaseFile? {
        cached("$id.json")?.let { text -> runCatching { CaseJson.case(text) }.getOrNull()?.let { return it } }
        return bundled("$id.json")?.let { runCatching { CaseJson.case(it) }.getOrNull() }
    }

    private fun bundledSchedule(): CaseSchedule = CaseJson.schedule(bundled("schedule.json")!!)

    private fun bundled(name: String): String? =
        runCatching { context.assets.open("cases/$name").bufferedReader().use { it.readText() } }.getOrNull()

    private fun cached(name: String): String? = File(cacheDir, name).takeIf { it.exists() }?.readText()

    private fun save(name: String, text: String): Boolean {
        val file = File(cacheDir, name)
        if (file.exists() && file.readText() == text) return false
        val tmp = File(cacheDir, "$name.tmp")
        tmp.writeText(text)
        return tmp.renameTo(file)
    }

    private fun download(name: String): String? = runCatching {
        val connection = URL(baseUrl.trimEnd('/') + "/" + name).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.useCaches = false
        try {
            if (connection.responseCode != 200) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
