package com.amitabh.rungs

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

// Training rules from the product brief. Pure functions so they can be unit-tested.

/**
 * Rotation queue: the slot after the last completed session's; missed days change nothing.
 * A last session that isn't a slot of this plan (the plan was just changed) restarts at the first.
 */
fun nextSlot(split: Split, sessions: List<Session>): Int =
    sessions.lastOrNull()?.let { (split.slots.indexOf(it.days) + 1) % split.slots.size } ?: 0

fun slotLabel(days: List<Day>) = when (days.size) { 1 -> days[0].label; 2 -> "Upper"; else -> "Full body" }

/** Warm-up plus 3 sets per exercise at ~45 s of work and one rest each. */
fun estimatedMinutes(exercises: Int, restSeconds: Int) = 5 + exercises * 3 * (restSeconds + 45) / 60

/**
 * Body-weight trend in kg per week across the last 5 weeks of weigh-ins; null until they span 2 weeks.
 * ponytail: first-to-last difference, so one odd weigh-in skews it. Least-squares slope if that bites.
 */
fun weeklyChange(weights: List<Weight>, today: LocalDate): Double? {
    val recent = weights.filter { ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) <= 35 }
    if (recent.size < 2) return null
    val days = ChronoUnit.DAYS.between(LocalDate.parse(recent.first().date), LocalDate.parse(recent.last().date))
    return if (days < 14) null else (recent.last().kg - recent.first().kg) * 7 / days
}

/** The goal's weekly change for this body weight, in kg. */
fun targetChange(goal: BodyGoal, kg: Double): ClosedFloatingPointRange<Double> =
    goal.pctPerWeek.start * kg / 100..goal.pctPerWeek.endInclusive * kg / 100

/**
 * Calories per day to add (or, negative, cut) to bring the trend back inside the goal's range,
 * at ~7700 kcal per kg, rounded to 50. 0 means on track.
 */
fun calorieNudge(goal: BodyGoal, kg: Double, kgPerWeek: Double): Int {
    val want = targetChange(goal, kg)
    return ((kgPerWeek.coerceIn(want) - kgPerWeek) * 7700 / 7 / 50).roundToInt() * 50
}

/** Trained on each of the last 6 days and not yet today: suggest resting on the 7th. */
fun suggestRest(sessions: List<Session>, today: LocalDate): Boolean {
    val dates = sessions.mapTo(HashSet()) { it.localDate }
    return today !in dates && (1L..6L).all { today.minusDays(it) in dates }
}

/** Back after 7+ days off: the next session uses lowered targets. */
fun isComeback(sessions: List<Session>, today: LocalDate): Boolean =
    sessions.lastOrNull()?.let { ChronoUnit.DAYS.between(it.localDate, today) >= 7 } ?: false

/** Target range for a level, lowered ~20% on a comeback session. */
fun target(level: Level, comeback: Boolean = false): IntRange =
    if (comeback) (level.min * 0.8).roundToInt()..(level.max * 0.8).roundToInt() else level.min..level.max

fun sessionsThisWeek(sessions: List<Session>, today: LocalDate): Int {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return sessions.count { !it.localDate.isBefore(monday) && !it.localDate.isAfter(today) }
}

/** Sets of [movementId] from the most recent session that has any. */
fun lastSets(movementId: String, sessions: List<Session>): List<SetLog> =
    sessions.asReversed().firstNotNullOfOrNull { s -> s.sets.filter { it.movementId == movementId }.ifEmpty { null } }
        ?: emptyList()

/** Stepper step: 1 rep, or 5 seconds for holds. */
fun step(level: Level) = if (level.measure == Measure.SECONDS) 5 else 1

/**
 * Where the stepper starts for a set: last time's same set plus one step ("add one rep to any set"),
 * capped at the top of the range (past it, the answer is the next level), or the bottom of the
 * target with no history or on a comeback session.
 */
fun startValue(level: Level, last: List<SetLog>, setIndex: Int, comeback: Boolean): Int {
    val prev = last.getOrNull(setIndex)?.value
    return if (prev == null || comeback) target(level, comeback).first else minOf(prev + step(level), level.max)
}

enum class Suggestion { LEVEL_UP, ADD_LOAD }

/**
 * Brief rule 3: all 3 sets at the top of the range in the last two sessions with this movement.
 * Only sets at the current level, books and tempo count. At the top of a ladder (or a one-level
 * ladder like the backpack row) the suggestion is more books or a slower tempo instead.
 */
fun suggestion(m: Movement, p: Progress, sessions: List<Session>): Suggestion? {
    val recent = sessions.asReversed().asSequence()
        .map { s -> s.sets.filter { it.movementId == m.id } }
        .filter { it.isNotEmpty() }
        .take(2).toList()
    return when {
        recent.size < 2 || !recent.all { allAtTop(it, m, p) } -> null
        p.level < m.levels.lastIndex -> Suggestion.LEVEL_UP
        else -> Suggestion.ADD_LOAD
    }
}

/** The last session was at the top; one more like it triggers [suggestion]. */
fun oneSessionAway(m: Movement, p: Progress, sessions: List<Session>): Boolean =
    suggestion(m, p, sessions) == null && allAtTop(lastSets(m.id, sessions), m, p)

/** Sets that count for progression: same level, books and tempo as now, and not a test set. */
fun countingSets(sets: List<SetLog>, p: Progress) =
    sets.filter { !it.test && it.level == p.level && it.books == p.books && it.slowTempo == p.slowTempo }

private fun allAtTop(sets: List<SetLog>, m: Movement, p: Progress): Boolean {
    val main = countingSets(sets, p)
    return main.size >= 3 && main.all { it.value >= m.levels[p.level].max }
}

sealed interface TestStep {
    /** Done: start the movement at this level. */
    data class Start(val level: Int) : TestStep
    /** Do one more max set at this level. */
    data class TestAgain(val level: Int) : TestStep
}

/**
 * Strength test: above the range tests one level up, below it one level down, inside it starts here.
 * Moving back to an already-tested level settles on the lower of the two, so it can't loop.
 */
fun testStep(m: Movement, level: Int, value: Int, tested: Set<Int>): TestStep {
    val l = m.levels[level]
    val next = when {
        value > l.max && level < m.levels.lastIndex -> level + 1
        value < l.min && level > 0 -> level - 1
        else -> return TestStep.Start(level)
    }
    return if (next in tested) TestStep.Start(minOf(level, next)) else TestStep.TestAgain(next)
}

/** Working sets (no test sets) of [movementId] per session, newest first. */
fun history(movementId: String, sessions: List<Session>): List<Pair<Session, List<SetLog>>> =
    sessions.asReversed().mapNotNull { s ->
        s.sets.filter { it.movementId == movementId && !it.test }.ifEmpty { null }?.let { s to it }
    }

/** Adds a weigh-in; a second one on the same day replaces the first. Kept oldest first. */
fun withWeight(weights: List<Weight>, w: Weight): List<Weight> =
    (weights.filter { it.date != w.date } + w).sortedBy { it.date }

/** Data safety: nudge for a backup when there's training to lose and none in the last 14 days. */
fun backupDue(lastExport: String?, sessions: List<Session>, today: LocalDate): Boolean =
    sessions.isNotEmpty() && (lastExport == null || ChronoUnit.DAYS.between(LocalDate.parse(lastExport), today) >= 14)
