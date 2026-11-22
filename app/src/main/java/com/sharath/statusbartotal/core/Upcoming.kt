package com.sharath.statusbartotal.core

import java.time.LocalDate
import java.time.YearMonth

data class UpcomingDue(val item: Recurring, val date: LocalDate, val monthsLeft: Int?)

object Upcoming {
    /** Recurring payments still due later this month and not yet added for this month. */
    fun compute(
        items: List<Recurring>, applied: Map<Long, Set<YearMonth>>, today: LocalDate
    ): List<UpcomingDue> {
        val month = YearMonth.from(today)
        val out = ArrayList<UpcomingDue>()
        for (r in items) {
            val set = applied[r.id] ?: emptySet()
            if (RecurringRules.isFinished(r, set)) continue
            if (month.isBefore(r.startMonth) || month in set) continue
            val date = RecurringRules.dueDate(r.day, month)
            if (!date.isAfter(today)) continue
            val left = if (r.type == RecurringType.EMI && r.months != null)
                RecurringRules.monthsLeft(r, set) else null
            out.add(UpcomingDue(r, date, left))
        }
        return out.sortedBy { it.date }
    }
}
