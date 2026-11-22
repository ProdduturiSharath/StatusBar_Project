package com.sharath.statusbartotal.core

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

sealed class DueEvent {
    abstract val due: LocalDateTime

    data class RecurringDue(
        val item: Recurring, val month: YearMonth, override val due: LocalDateTime
    ) : DueEvent()

    data class AutoResetDue(val month: YearMonth, override val due: LocalDateTime) : DueEvent()
}

object DuePlanner {
    private val RECURRING_TIME: LocalTime = LocalTime.of(0, 5)

    /**
     * Everything owed right now, in the order it must be written: sorted by due instant, and an
     * auto-reset (00:00) before a recurring payment (00:05) on the same day.
     */
    fun plan(
        items: List<Recurring>,
        applied: Map<Long, Set<YearMonth>>,
        autoDay: Int?,
        since: LocalDateTime?,
        appliedResets: Set<YearMonth>,
        now: LocalDateTime
    ): List<DueEvent> {
        val events = ArrayList<DueEvent>()
        for (r in items) {
            val set = applied[r.id] ?: emptySet()
            for (m in RecurringRules.missingEntries(r, set, now.toLocalDate())) {
                events.add(
                    DueEvent.RecurringDue(
                        r, m, LocalDateTime.of(RecurringRules.dueDate(r.day, m), RECURRING_TIME)
                    )
                )
            }
        }
        if (autoDay != null && since != null) {
            for ((m, at) in AutoReset.missing(autoDay, since, now, appliedResets)) {
                events.add(DueEvent.AutoResetDue(m, at))
            }
        }
        return events.sortedWith(
            compareBy<DueEvent> { it.due }.thenBy { if (it is DueEvent.AutoResetDue) 0 else 1 }
        )
    }

    /** The earliest of the next recurring due instant and the next auto-reset moment. */
    fun nextWake(
        items: List<Recurring>,
        applied: Map<Long, Set<YearMonth>>,
        autoDay: Int?,
        now: LocalDateTime
    ): LocalDateTime? {
        var earliest: LocalDateTime? = null
        for (r in items) {
            val next = RecurringRules.nextDue(r, applied[r.id] ?: emptySet(), now) ?: continue
            if (earliest == null || next.isBefore(earliest)) earliest = next
        }
        if (autoDay != null) {
            val next = AutoReset.nextAfter(autoDay, now)
            if (earliest == null || next.isBefore(earliest)) earliest = next
        }
        return earliest
    }
}
