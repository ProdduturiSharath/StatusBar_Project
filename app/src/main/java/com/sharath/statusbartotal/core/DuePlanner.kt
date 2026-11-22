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

    data class CardReminderDue(val reminder: ReminderDue) : DueEvent() {
        override val due: LocalDateTime get() = reminder.moment
    }
}

object DuePlanner {
    private val RECURRING_TIME: LocalTime = LocalTime.of(0, 5)

    private fun priority(e: DueEvent): Int = when (e) {
        is DueEvent.AutoResetDue -> 0
        is DueEvent.RecurringDue -> 1
        is DueEvent.CardReminderDue -> 2
    }

    /**
     * Everything owed right now, in the order it must be written: sorted by due instant, and on
     * the same instant an auto-reset before a recurring payment before a card reminder. Card
     * reminders are only included while their due date has not passed.
     */
    fun plan(
        items: List<Recurring>,
        applied: Map<Long, Set<YearMonth>>,
        autoDay: Int?,
        since: LocalDateTime?,
        appliedResets: Set<YearMonth>,
        now: LocalDateTime,
        cardDueDays: Map<String, Int> = emptyMap(),
        notes: Set<String> = emptySet()
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
        for (rem in Reminders.missing(cardDueDays, notes, now)) {
            events.add(DueEvent.CardReminderDue(rem))
        }
        return events.sortedWith(compareBy<DueEvent> { it.due }.thenBy { priority(it) })
    }

    /** The earliest of the next recurring due instant, auto-reset moment and card reminder. */
    fun nextWake(
        items: List<Recurring>,
        applied: Map<Long, Set<YearMonth>>,
        autoDay: Int?,
        now: LocalDateTime,
        cardDueDays: Map<String, Int> = emptyMap(),
        notes: Set<String> = emptySet()
    ): LocalDateTime? {
        var earliest: LocalDateTime? = null
        fun consider(t: LocalDateTime?) {
            if (t != null && (earliest == null || t.isBefore(earliest))) earliest = t
        }
        for (r in items) consider(RecurringRules.nextDue(r, applied[r.id] ?: emptySet(), now))
        if (autoDay != null) consider(AutoReset.nextAfter(autoDay, now))
        consider(Reminders.nextAfter(cardDueDays, notes, now))
        return earliest
    }
}
