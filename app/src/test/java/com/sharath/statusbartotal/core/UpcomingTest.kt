package com.sharath.statusbartotal.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class UpcomingTest {
    private fun ym(s: String) = YearMonth.parse(s)
    private fun rec(
        id: Long, day: Int, type: RecurringType = RecurringType.SUBSCRIPTION, months: Int? = null,
        start: String = "2026-01"
    ) = Recurring(id, type, 100 * id, Category.UPI, null, day, ym(start), months, "N$id")

    private val today = LocalDate.of(2026, 11, 23)

    @Test fun onlyStrictlyAfterToday() {
        val items = listOf(rec(1, 22), rec(2, 23), rec(3, 24), rec(4, 30))
        val out = Upcoming.compute(items, emptyMap(), today)
        assertEquals(listOf(3L, 4L), out.map { it.item.id })
    }

    @Test fun sortedByDate() {
        val out = Upcoming.compute(listOf(rec(1, 28), rec(2, 25)), emptyMap(), today)
        assertEquals(listOf(2L, 1L), out.map { it.item.id })
    }

    @Test fun appliedMonthExcluded() {
        val out = Upcoming.compute(
            listOf(rec(1, 28)), mapOf(1L to setOf(ym("2026-11"))), today
        )
        assertEquals(emptyList<Long>(), out.map { it.item.id })
    }

    @Test fun finishedEmiExcludedAndMonthsLeftShown() {
        val done = rec(1, 28, RecurringType.EMI, 2)
        val running = rec(2, 28, RecurringType.EMI, 5)
        val applied = mapOf(
            1L to setOf(ym("2026-09"), ym("2026-10")),
            2L to setOf(ym("2026-09"), ym("2026-10"))
        )
        val out = Upcoming.compute(listOf(done, running), applied, today)
        assertEquals(listOf(2L), out.map { it.item.id })
        assertEquals(3, out[0].monthsLeft)
    }

    @Test fun clampedDayInShortMonth() {
        val out = Upcoming.compute(listOf(rec(1, 31)), emptyMap(), LocalDate.of(2026, 2, 10))
        assertEquals(LocalDate.of(2026, 2, 28), out[0].date)
    }
}
