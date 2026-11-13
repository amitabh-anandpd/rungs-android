package com.amitabh.rungs

import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class Day {
    PUSH, PULL, LEGS;

    val label get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class Measure { REPS, SECONDS }

/** How the three categories are grouped into sessions. The queue walks [slots] in order. */
enum class Split(val label: String, val hint: String, val slots: List<List<Day>>) {
    PPL("Push / Pull / Legs", "Best if you train 5–6 days a week.", listOf(listOf(Day.PUSH), listOf(Day.PULL), listOf(Day.LEGS))),
    UPPER_LOWER("Upper / Lower", "Best if you train about 4 days a week.", listOf(listOf(Day.PUSH, Day.PULL), listOf(Day.LEGS))),
    FULL("Full body", "Best if you train 2–3 days a week.", listOf(Day.entries)),
}

/** Body type, as the thing it changes: how fast body weight should move, in % of body weight per week. */
enum class BodyGoal(val label: String, val pctPerWeek: ClosedFloatingPointRange<Double>, val tip: String) {
    GAIN("Thin: gain weight and muscle", 0.25..0.5, "Eat more than feels natural: three meals plus two snacks, and drink some of your calories (milk, shakes)."),
    MAINTAIN("Average: build muscle at the same weight", -0.15..0.15, "Keep eating as you do, with enough protein. Progress shows in your reps, not on the scale."),
    LOSE("Heavier: lose fat, keep muscle", -1.0..-0.5, "Eat a little less, keep protein high, and keep training hard so the weight lost is fat."),
}

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
    /** Categories trained: one on a Push/Pull/Legs plan, more on Upper/Lower and Full body. */
    val days: List<Day> = listOf(day),
    /** The exercises picked for this session. Empty in old sessions: everything for [day]. */
    val movementIds: List<String> = emptyList(),
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
    val split: Split = Split.PPL,
    val bodyGoal: BodyGoal = BodyGoal.GAIN,
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
    /** Exercises the user chose per plan. A plan without an entry uses [defaultPicks]. */
    val picks: Map<Split, Set<String>> = emptyMap(),
) {
    fun progressOf(m: Movement) = progress[m.id] ?: Progress(m.startLevel)

    fun picked(): Set<String> = picks[profile.split] ?: defaultPicks(profile.split)

    /** The exercises a session covering [days] would do, in ladder order. */
    fun sessionMovements(days: List<Day>): List<Movement> = picked().let { on -> movements.filter { it.day in days && it.id in on } }

    /** Ladders added in an app update join the saved ones. Saved ladders and edits to them are kept. */
    fun withNewLadders(): AppData {
        val order = DefaultLadders.map { it.id }
        val added = DefaultLadders.filter { d -> movements.none { it.id == d.id } }
        return if (added.isEmpty()) this
        else copy(movements = (movements + added).sortedBy { m -> order.indexOf(m.id).let { if (it < 0) order.size else it } })
    }
}
