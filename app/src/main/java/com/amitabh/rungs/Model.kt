package com.amitabh.rungs

import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class Day {
    PUSH, PULL, LEGS;

    val label get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class Measure { REPS, SECONDS }

/** One rung of a ladder. [min]..[max] is the target range; the user can raise it over time. */
@Serializable
data class Level(
    val name: String,
    val cue: String,
    val min: Int,
    val max: Int,
    val measure: Measure = Measure.REPS,
)

@Serializable
data class Movement(
    val id: String,
    val name: String,
    val day: Day,
    val levels: List<Level>,
    /** Estimated starting level; the strength test confirms or corrects it. */
    val startLevel: Int,
)

/** Where the user currently is on a movement's ladder. */
@Serializable
data class Progress(
    val level: Int,
    val books: Int = 0,
    val slowTempo: Boolean = false,
    val tested: Boolean = false,
)

/**
 * [value] is reps or seconds. [assist] = reps finished at the previous level (brief rule 4).
 * [test] = a strength-test max set; it never counts toward progression.
 */
@Serializable
data class SetLog(
    val movementId: String,
    val level: Int,
    val value: Int,
    val books: Int = 0,
    val slowTempo: Boolean = false,
    val assist: Int = 0,
    val test: Boolean = false,
)

@Serializable
data class Session(
    val day: Day,
    /** ISO date, e.g. 2026-11-10. Kept as text so backups stay readable. */
    val date: String,
    val startedAt: Long,
    val sets: List<SetLog> = emptyList(),
    /** First session back after 7+ days off: targets lowered ~20%. */
    val comeback: Boolean = false,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
}

@Serializable
data class Weight(val date: String, val kg: Double)

@Serializable
data class Profile(
    val goal: String = "Muscle size",
    val equipment: List<String> = listOf("Bed", "Table", "Chair", "Backpack with books"),
    val restSeconds: Int = 75,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val profile: Profile = Profile(),
    val movements: List<Movement> = DefaultLadders,
    val progress: Map<String, Progress> = DefaultLadders.associate { it.id to Progress(it.startLevel) },
    /** Completed sessions, oldest first. */
    val sessions: List<Session> = emptyList(),
    val weights: List<Weight> = emptyList(),
    /** Saved after every logged set so a killed app loses nothing. */
    val inProgress: Session? = null,
    val lastExport: String? = null,
) {
    fun progressOf(m: Movement) = progress[m.id] ?: Progress(m.startLevel)
}
