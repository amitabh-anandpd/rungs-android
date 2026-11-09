package com.amitabh.rungs

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

// Training rules from the product brief. Pure functions so they can be unit-tested.

/** Rotation queue: the day after the last completed session; missed days change nothing. */
fun nextDay(sessions: List<Session>): Day =
    sessions.lastOrNull()?.let { Day.entries[(it.day.ordinal + 1) % Day.entries.size] } ?: Day.PUSH

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

/** Sets that count for progression: same level, books and tempo as now. */
fun countingSets(sets: List<SetLog>, p: Progress) =
    sets.filter { it.level == p.level && it.books == p.books && it.slowTempo == p.slowTempo }

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
