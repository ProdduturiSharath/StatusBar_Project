package com.sharath.statusbartotal.core

import java.time.LocalDateTime
import java.time.YearMonth

object AutoReset {
    /** 00:00 local time on the due date (day clamped to the month's length). */
    fun moment(day: Int, month: YearMonth): LocalDateTime =
        RecurringRules.dueDate(day, month).atStartOfDay()

    /** Reset moments after [since] and not after [now] whose month is not in [applied], oldest first. */
    fun missing(
        day: Int, since: LocalDateTime, now: LocalDateTime, applied: Set<YearMonth>
    ): List<Pair<YearMonth, LocalDateTime>> {
        val out = ArrayList<Pair<YearMonth, LocalDateTime>>()
        val last = YearMonth.from(now)
        var m = YearMonth.from(since)
        while (!m.isAfter(last)) {
            val at = moment(day, m)
            if (at.isAfter(since) && !at.isAfter(now) && m !in applied) out.add(m to at)
            m = m.plusMonths(1)
        }
        return out
    }

    /** The first reset moment strictly after [now]. */
    fun nextAfter(day: Int, now: LocalDateTime): LocalDateTime {
        var m = YearMonth.from(now)
        while (true) {
            val at = moment(day, m)
            if (at.isAfter(now)) return at
            m = m.plusMonths(1)
        }
    }
}
