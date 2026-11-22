package com.sharath.statusbartotal.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringTest {
    private fun rec(
        day: Int, start: String, type: RecurringType = RecurringType.SUBSCRIPTION, months: Int? = null
    ) = Recurring(1, type, 649, Category.UPI, null, day, YearMonth.parse(start), months, "N")

    private fun ym(s: String) = YearMonth.parse(s)

    @Test fun dueDateClamping() {
        assertEquals(LocalDate.of(2026, 2, 28), RecurringRules.dueDate(31, ym("2026-02")))
        assertEquals(LocalDate.of(2028, 2, 29), RecurringRules.dueDate(31, ym("2028-02")))
        assertEquals(LocalDate.of(2026, 2, 28), RecurringRules.dueDate(29, ym("2026-02")))
        assertEquals(LocalDate.of(2026, 4, 30), RecurringRules.dueDate(31, ym("2026-04")))
        assertEquals(LocalDate.of(2026, 4, 5), RecurringRules.dueDate(5, ym("2026-04")))
    }

    @Test fun firstMonthAlreadyPassedAddsNow() {
        val r = rec(5, "2026-11")
        assertEquals(
            listOf(ym("2026-11")),
            RecurringRules.missingEntries(r, emptySet(), LocalDate.of(2026, 11, 23))
        )
    }

    @Test fun notYetDueAddsNothing() {
        val r = rec(25, "2026-11")
        assertEquals(
            emptyList<YearMonth>(),
            RecurringRules.missingEntries(r, emptySet(), LocalDate.of(2026, 11, 23))
        )
    }

    @Test fun dueTodayCounts() {
        val r = rec(23, "2026-11")
        assertEquals(1, RecurringRules.missingEntries(r, emptySet(), LocalDate.of(2026, 11, 23)).size)
    }

    @Test fun multiMonthCatchUpOldestFirst() {
        val r = rec(5, "2026-08")
        val m = RecurringRules.missingEntries(r, setOf(ym("2026-09")), LocalDate.of(2026, 11, 23))
        assertEquals(listOf(ym("2026-08"), ym("2026-10"), ym("2026-11")), m)
    }

    @Test fun runningTwiceDoesNotDuplicate() {
        val r = rec(5, "2026-08")
        val today = LocalDate.of(2026, 11, 23)
        val first = RecurringRules.missingEntries(r, emptySet(), today)
        assertEquals(4, first.size)
        assertEquals(emptyList<YearMonth>(), RecurringRules.missingEntries(r, first.toSet(), today))
    }

    @Test fun emiStopsAtN() {
        val r = rec(5, "2026-08", RecurringType.EMI, 3)
        val today = LocalDate.of(2026, 11, 23)
        val first = RecurringRules.missingEntries(r, emptySet(), today)
        assertEquals(listOf(ym("2026-08"), ym("2026-09"), ym("2026-10")), first)
        assertEquals(emptyList<YearMonth>(), RecurringRules.missingEntries(r, first.toSet(), today))
        assertEquals(0, RecurringRules.monthsLeft(r, first.toSet()))
        assertNull(RecurringRules.nextDue(r, first.toSet(), LocalDateTime.of(2026, 11, 23, 0, 0)))
    }

    @Test fun emiMonthsLeftCountsFirst() {
        val r = rec(5, "2026-11", RecurringType.EMI, 7)
        assertEquals(7, RecurringRules.monthsLeft(r, emptySet()))
        assertEquals(6, RecurringRules.monthsLeft(r, setOf(ym("2026-11"))))
    }

    @Test fun neverFutureMonth() {
        val r = rec(31, "2026-11")
        assertEquals(
            emptyList<YearMonth>(),
            RecurringRules.missingEntries(r, emptySet(), LocalDate.of(2026, 11, 29))
        )
        assertEquals(1, RecurringRules.missingEntries(r, emptySet(), LocalDate.of(2026, 11, 30)).size)
    }

    @Test fun nextDueSkipsAppliedMonths() {
        val r = rec(5, "2026-11")
        val now = LocalDateTime.of(2026, 11, 23, 10, 0)
        assertEquals(
            LocalDateTime.of(2026, 12, 5, 0, 5),
            RecurringRules.nextDue(r, setOf(ym("2026-11")), now)
        )
        assertEquals(LocalDateTime.of(2026, 11, 5, 0, 5), RecurringRules.nextDue(r, emptySet(), now))
    }

    @Test fun appliedMonthsFromSources() {
        val s = setOf("1@2026-10", "1@2026-11", "2@2026-11", "1@bad")
        assertEquals(setOf(ym("2026-10"), ym("2026-11")), RecurringRules.appliedMonths(1, s))
    }
}
