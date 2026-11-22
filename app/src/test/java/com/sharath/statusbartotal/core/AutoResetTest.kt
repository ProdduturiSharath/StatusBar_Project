package com.sharath.statusbartotal.core

import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoResetTest {
    private fun ym(s: String) = YearMonth.parse(s)
    private fun dt(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) = LocalDateTime.of(y, m, d, h, min)

    @Test fun momentClamps() {
        assertEquals(dt(2026, 2, 28), AutoReset.moment(31, ym("2026-02")))
        assertEquals(dt(2026, 12, 15), AutoReset.moment(15, ym("2026-12")))
    }

    @Test fun noRetroactiveResetsBeforeSince() {
        // Enabled 20 Nov with day 15: the 15 Nov moment is before it, so nothing is missing.
        val m = AutoReset.missing(15, dt(2026, 11, 20, 10), dt(2026, 11, 23, 9), emptySet())
        assertTrue(m.isEmpty())
    }

    @Test fun missingAcrossMonthsSkipsApplied() {
        val m = AutoReset.missing(
            15, dt(2026, 9, 20), dt(2026, 12, 20), setOf(ym("2026-11"))
        )
        assertEquals(listOf(ym("2026-10"), ym("2026-12")), m.map { it.first })
    }

    @Test fun sinceExactlyAtMomentIsNotRepeated() {
        val m = AutoReset.missing(15, dt(2026, 11, 15), dt(2026, 11, 16), emptySet())
        assertTrue(m.isEmpty())
    }

    @Test fun nextAfterIsStrictlyAfter() {
        assertEquals(dt(2026, 12, 15), AutoReset.nextAfter(15, dt(2026, 11, 15)))
        assertEquals(dt(2026, 11, 15), AutoReset.nextAfter(15, dt(2026, 11, 14, 23, 59)))
        assertEquals(dt(2027, 2, 28), AutoReset.nextAfter(31, dt(2027, 1, 31, 12)))
    }

    private fun rec(id: Long, day: Int, start: String) = Recurring(
        id, RecurringType.SUBSCRIPTION, 100, Category.UPI, null, day, ym(start), null, "N"
    )

    @Test fun resetComesBeforeRecurringSameDay() {
        val events = DuePlanner.plan(
            listOf(rec(1, 15, "2026-11")), emptyMap(), 15, dt(2026, 11, 1), emptySet(),
            dt(2026, 11, 16)
        )
        assertEquals(2, events.size)
        assertTrue(events[0] is DueEvent.AutoResetDue)
        assertTrue(events[1] is DueEvent.RecurringDue)
    }

    @Test fun catchUpAcrossMonthsIsOrderedByDueInstant() {
        val events = DuePlanner.plan(
            listOf(rec(1, 5, "2026-09")), emptyMap(), 10, dt(2026, 9, 1), emptySet(),
            dt(2026, 11, 20)
        )
        val labels = events.map {
            when (it) {
                is DueEvent.RecurringDue -> "R" + it.month
                is DueEvent.AutoResetDue -> "A" + it.month
                is DueEvent.CardReminderDue -> "C"
            }
        }
        assertEquals(
            listOf("R2026-09", "A2026-09", "R2026-10", "A2026-10", "R2026-11", "A2026-11"), labels
        )
    }

    @Test fun noDuplicatesWhenAppliedAlready() {
        val events = DuePlanner.plan(
            listOf(rec(1, 5, "2026-10")),
            mapOf(1L to setOf(ym("2026-10"), ym("2026-11"))),
            10, dt(2026, 9, 1), setOf(ym("2026-09"), ym("2026-10"), ym("2026-11")),
            dt(2026, 11, 20)
        )
        assertTrue(events.isEmpty())
    }

    @Test fun nextWakeIsEarliest() {
        val r = rec(1, 25, "2026-11")
        val now = dt(2026, 11, 23, 12)
        assertEquals(dt(2026, 11, 25, 0, 5), DuePlanner.nextWake(listOf(r), emptyMap(), 28, now))
        assertEquals(dt(2026, 11, 24), DuePlanner.nextWake(listOf(r), emptyMap(), 24, now))
        assertEquals(dt(2026, 11, 25, 0, 5), DuePlanner.nextWake(listOf(r), emptyMap(), null, now))
        assertEquals(null, DuePlanner.nextWake(emptyList(), emptyMap(), null, now))
    }
}
