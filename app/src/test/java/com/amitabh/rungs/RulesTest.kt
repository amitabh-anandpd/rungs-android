package com.amitabh.rungs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val today = LocalDate.of(2026, 11, 10) // a Tuesday
    private fun s(day: Day, daysAgo: Long, vararg sets: SetLog) =
        Session(day, today.minusDays(daysAgo).toString(), 0, sets.toList())

    private val pushup = DefaultLadders.first { it.id == "pushup" }
    private val bprow = DefaultLadders.first { it.id == "bprow" }
    private fun sets(id: String, level: Int, vararg v: Int, books: Int = 0) =
        v.map { SetLog(id, level, it, books) }.toTypedArray()

    @Test fun rotationCyclesAndStartsWithPush() {
        assertEquals(0, nextSlot(Split.PPL, emptyList()))
        assertEquals(1, nextSlot(Split.PPL, listOf(s(Day.PUSH, 5))))
        assertEquals(0, nextSlot(Split.PPL, listOf(s(Day.PUSH, 5), s(Day.PULL, 4), s(Day.LEGS, 1))))
    }

    @Test fun otherPlansRotateTheirOwnDays() {
        val upper = Session(Day.PUSH, today.toString(), 0, days = listOf(Day.PUSH, Day.PULL))
        assertEquals(1, nextSlot(Split.UPPER_LOWER, listOf(upper)))
        assertEquals(0, nextSlot(Split.UPPER_LOWER, listOf(upper, s(Day.LEGS, 0))))
        assertEquals(0, nextSlot(Split.FULL, listOf(Session(Day.PUSH, today.toString(), 0, days = Day.entries))))
        // Plan just changed: the last session isn't one of the new plan's days, so start from the top.
        assertEquals(0, nextSlot(Split.UPPER_LOWER, listOf(s(Day.PULL, 1))))
        assertEquals("Upper", slotLabel(Split.UPPER_LOWER.slots[0]))
        assertEquals("Full body", slotLabel(Split.FULL.slots[0]))
    }

    @Test fun defaultPlanNeedsNoTableAndShrinksOnCombinedDays() {
        val ppl = AppData()
        assertFalse("tablerow" in ppl.picked())
        assertEquals(listOf("bprow", "pullover", "bpcurl", "reardelt"), ppl.sessionMovements(listOf(Day.PULL)).map { it.id })
        val full = AppData(profile = Profile(split = Split.FULL))
        assertEquals(listOf("pushup", "pike", "bprow", "pullover", "squat", "hip"), full.sessionMovements(Day.entries).map { it.id })
        val ul = AppData(profile = Profile(split = Split.UPPER_LOWER))
        assertEquals(6, ul.sessionMovements(listOf(Day.PUSH, Day.PULL)).size)
        assertEquals(5, ul.sessionMovements(listOf(Day.LEGS)).size)
        // The user's own picks win, per plan.
        val mine = ppl.copy(picks = mapOf(Split.PPL to setOf("bprow", "doorrow")))
        assertEquals(listOf("bprow", "doorrow"), mine.sessionMovements(listOf(Day.PULL)).map { it.id })
        assertEquals(full.picked(), mine.copy(profile = Profile(split = Split.FULL)).picked())
    }

    @Test fun oldSaveGetsNewLaddersAndKeepsItsOwn() {
        val edited = DefaultLadders.first { it.id == "pushup" }.copy(name = "My push-up")
        val old = AppData(movements = listOf(DefaultLadders.first { it.id == "tablerow" }, edited, DefaultLadders.first { it.id == "squat" }))
        val json = RungsJson.encodeToString(old)
        val d = Store.import(json.byteInputStream())
        assertEquals(DefaultLadders.map { it.id }, d.movements.map { it.id })
        assertEquals("My push-up", d.movements.first().name)
        assertEquals(d, d.withNewLadders())
        // A session saved before plans existed still decodes, as a one-category day.
        val legacy = RungsJson.decodeFromString<Session>("""{"day":"PULL","date":"2026-11-10","startedAt":0}""")
        assertEquals(listOf(Day.PULL), legacy.days)
        assertTrue(legacy.movementIds.isEmpty())
    }

    @Test fun weightTrendAndCalorieNudge() {
        fun w(daysAgo: Long, kg: Double) = Weight(today.minusDays(daysAgo).toString(), kg)
        assertNull(weeklyChange(listOf(w(0, 62.0)), today))
        assertNull(weeklyChange(listOf(w(7, 62.0), w(0, 62.5)), today))            // under two weeks
        assertEquals(0.1, weeklyChange(listOf(w(90, 50.0), w(21, 62.0), w(0, 62.3)), today)!!, 1e-9) // old weigh-in ignored
        // 62 kg gaining: 0.155..0.31 kg a week.
        assertEquals(0.155, targetChange(BodyGoal.GAIN, 62.0).start, 1e-9)
        assertEquals(50, calorieNudge(BodyGoal.GAIN, 62.0, 0.1))    // 0.055 kg/week short = ~60 kcal/day
        assertEquals(150, calorieNudge(BodyGoal.GAIN, 62.0, 0.0))
        assertEquals(0, calorieNudge(BodyGoal.GAIN, 62.0, 0.2))
        assertEquals(-200, calorieNudge(BodyGoal.GAIN, 62.0, 0.5))
        assertEquals(-450, calorieNudge(BodyGoal.LOSE, 90.0, -0.05))
    }

    @Test fun restSuggestedOnlyAfterSixDaysInARow() {
        val six = (1L..6L).map { s(Day.PUSH, it) }
        assertTrue(suggestRest(six, today))
        assertFalse(suggestRest(six.drop(1), today))              // gap 6 days ago
        assertFalse(suggestRest(six + s(Day.PUSH, 0), today))      // already trained today
    }

    @Test fun comebackAfterSevenDaysLowersTargets() {
        assertFalse(isComeback(emptyList(), today))
        assertFalse(isComeback(listOf(s(Day.PUSH, 6)), today))
        assertTrue(isComeback(listOf(s(Day.PUSH, 7)), today))
        assertEquals(6..12, target(Level("x", "", 8, 15), comeback = true))
        assertEquals(8..15, target(Level("x", "", 8, 15)))
    }

    @Test fun weekCountStartsMonday() {
        assertEquals(2, sessionsThisWeek(listOf(s(Day.PUSH, 2), s(Day.PULL, 1), s(Day.LEGS, 0)), today))
    }

    @Test fun levelUpNeedsTwoSessionsAtTopOfRange() {
        val p = Progress(level = 4)
        val top = s(Day.PUSH, 3, *sets("pushup", 4, 15, 15, 15))
        val short = s(Day.PUSH, 3, *sets("pushup", 4, 15, 15, 14))
        assertNull(suggestion(pushup, p, listOf(top)))
        assertTrue(oneSessionAway(pushup, p, listOf(top)))
        assertNull(suggestion(pushup, p, listOf(short, top)))
        assertFalse(oneSessionAway(pushup, p, listOf(top, short)))
        assertFalse(oneSessionAway(pushup, p, listOf(top, top))) // already ready, not "one away"
        assertEquals(Suggestion.LEVEL_UP, suggestion(pushup, p, listOf(top, top)))
        // Sessions without the movement in between don't break "in a row".
        assertEquals(Suggestion.LEVEL_UP, suggestion(pushup, p, listOf(top, s(Day.PULL, 2), top)))
        // Sets at the old level don't count after moving up.
        assertNull(suggestion(pushup, Progress(level = 5), listOf(top, top)))
    }

    @Test fun topOfLadderSuggestsLoadAndBooksReset() {
        val p = Progress(level = 0, books = 2)
        val atTwo = s(Day.PULL, 1, *sets("bprow", 0, 15, 15, 15, books = 2))
        val atOne = s(Day.PULL, 1, *sets("bprow", 0, 15, 15, 15, books = 1))
        assertEquals(Suggestion.ADD_LOAD, suggestion(bprow, p, listOf(atTwo, atTwo)))
        assertNull(suggestion(bprow, p, listOf(atOne, atTwo)))
    }

    @Test fun slowTempoSwitchResetsLevelUpCount() {
        val normal = s(Day.PULL, 2, *sets("bprow", 0, 15, 15, 15))
        assertEquals(Suggestion.ADD_LOAD, suggestion(bprow, Progress(0), listOf(normal, normal)))
        assertNull(suggestion(bprow, Progress(0, slowTempo = true), listOf(normal, normal)))
    }

    @Test fun lastSetsComesFromMostRecentSessionWithTheMovement() {
        val older = s(Day.PUSH, 4, *sets("pushup", 4, 10, 10, 9))
        val newer = s(Day.PUSH, 1, *sets("pushup", 4, 12, 11, 10))
        assertEquals(listOf(12, 11, 10), lastSets("pushup", listOf(older, newer, s(Day.PULL, 0))).map { it.value })
        assertTrue(lastSets("dip", listOf(older, newer)).isEmpty())
    }

    @Test fun badBackupFileIsRejected() {
        val good = RungsJson.encodeToString(AppData())
        for (bad in listOf("", "not json", good.take(good.length / 2))) {
            try {
                Store.import(bad.byteInputStream())
                fail("accepted: ${bad.take(20)}")
            } catch (_: Exception) {}
        }
        assertEquals(AppData(), Store.import(good.byteInputStream()))
    }

    @Test fun stepperStartsOneAboveLastTime() {
        val reps = Level("x", "", 8, 15)
        val hold = Level("h", "", 20, 45, Measure.SECONDS)
        val last = sets("pushup", 4, 12, 11).toList()
        assertEquals(13, startValue(reps, last, 0, comeback = false))
        assertEquals(8, startValue(reps, last, 2, comeback = false))   // no third set last time
        assertEquals(6, startValue(reps, last, 0, comeback = true))    // comeback uses lowered target
        assertEquals(35, startValue(hold, listOf(SetLog("core", 1, 30)), 0, comeback = false))
        assertEquals(15, startValue(reps, sets("pushup", 4, 15).toList(), 0, comeback = false)) // capped at top
    }

    @Test fun strengthTestPlacement() {
        assertEquals(TestStep.Start(4), testStep(pushup, 4, 12, emptySet()))
        assertEquals(TestStep.TestAgain(5), testStep(pushup, 4, 24, setOf(4)))
        assertEquals(TestStep.TestAgain(3), testStep(pushup, 4, 5, setOf(4)))
        // Went up, too hard: settle on the lower level instead of looping.
        assertEquals(TestStep.Start(4), testStep(pushup, 5, 5, setOf(4, 5)))
        // Went down, too easy: also settle on the lower.
        assertEquals(TestStep.Start(3), testStep(pushup, 3, 20, setOf(3, 4)))
        // Ends of the ladder.
        assertEquals(TestStep.Start(7), testStep(pushup, 7, 30, emptySet()))
        assertEquals(TestStep.Start(0), testStep(pushup, 0, 2, emptySet()))
    }

    @Test fun testSetsNeverCountForProgression() {
        val p = Progress(level = 4)
        val maxSets = s(Day.PUSH, 1, *sets("pushup", 4, 15, 15, 15).map { it.copy(test = true) }.toTypedArray())
        assertTrue(countingSets(maxSets.sets, p).isEmpty())
        assertEquals(null, suggestion(pushup, p, listOf(maxSets, maxSets)))
    }

    @Test fun historyIsNewestFirstWithoutTestSets() {
        val old = s(Day.PUSH, 3, *sets("pushup", 4, 10, 9))
        val new = s(Day.PUSH, 1, SetLog("pushup", 4, 20, test = true), *sets("pushup", 4, 12))
        val h = history("pushup", listOf(old, s(Day.PULL, 2), new))
        assertEquals(listOf(listOf(12), listOf(10, 9)), h.map { (_, sets) -> sets.map { it.value } })
    }

    @Test fun sameDayWeightReplacesAndStaysSorted() {
        val w = withWeight(listOf(Weight("2026-11-01", 60.0), Weight("2026-11-09", 61.0)), Weight("2026-11-05", 60.5))
        assertEquals(listOf("2026-11-01", "2026-11-05", "2026-11-09"), w.map { it.date })
        assertEquals(listOf(60.0, 62.0), withWeight(listOf(Weight("2026-11-01", 60.0), Weight("2026-11-09", 61.0)), Weight("2026-11-09", 62.0)).map { it.kg })
    }

    @Test fun backupNudgeAfterTwoWeeks() {
        assertFalse(backupDue(null, emptyList(), today))
        assertTrue(backupDue(null, listOf(s(Day.PUSH, 1)), today))
        assertFalse(backupDue(today.minusDays(13).toString(), listOf(s(Day.PUSH, 1)), today))
        assertTrue(backupDue(today.minusDays(14).toString(), listOf(s(Day.PUSH, 1)), today))
    }

    @Test fun backupWithLevelOutOfRangeIsRejected() {
        val bad = RungsJson.encodeToString(AppData(progress = mapOf("pushup" to Progress(level = 99))))
        try {
            Store.import(bad.byteInputStream()); fail("should reject")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun dataSurvivesJsonRoundTrip() {
        val d = AppData(sessions = listOf(s(Day.PUSH, 1, *sets("pushup", 4, 12, 11))), lastExport = "2026-11-01")
        assertEquals(d, RungsJson.decodeFromString<AppData>(RungsJson.encodeToString(d)))
    }

    @Test fun defaultLaddersAreValid() {
        assertEquals(DefaultLadders.size, DefaultLadders.map { it.id }.toSet().size)
        Day.entries.forEach { d -> assertTrue(DefaultLadders.any { it.day == d }) }
        DefaultLadders.forEach { m ->
            assertTrue(m.id, m.startLevel in m.levels.indices)
            m.levels.forEach { assertTrue(it.name, it.min < it.max) }
        }
    }
}
