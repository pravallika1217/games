package com.alibi.engine.cases

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Which case plays on which day. Case #1 plays on [launchDay], then the next one each day.
 * Only ever add to the end of [cases]: past days keep their case that way. When the list runs
 * out, it starts again from the top.
 */
@Serializable
data class CaseSchedule(
    /** Like "2026-10-08". */
    val launchDay: String,
    /** Case ids in play order. Each id has a file named `<id>.json` next to this one. */
    val cases: List<String>,
) {
    private val launchEpochDay: Long get() = LocalDate.parse(launchDay).toEpochDay()

    fun caseNumber(epochDay: Long): Int = (epochDay - launchEpochDay + 1).coerceAtLeast(1).toInt()

    fun caseId(number: Int): String = cases[Math.floorMod(number - 1, cases.size)]

    fun problems(): List<String> = buildList {
        if (runCatching { LocalDate.parse(launchDay) }.isFailure) add("schedule: launchDay must look like 2026-10-08")
        if (cases.isEmpty()) add("schedule: no cases")
    }
}

/** Reads case files and the schedule. Both are plain JSON, so they can live on any web host. */
object CaseJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and checks a case. Throws with every problem listed if it isn't playable. */
    fun case(text: String): CaseFile {
        val file = json.decodeFromString(CaseFile.serializer(), text)
        val problems = file.problems()
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return file
    }

    fun schedule(text: String): CaseSchedule {
        val schedule = json.decodeFromString(CaseSchedule.serializer(), text)
        val problems = schedule.problems()
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return schedule
    }
}
