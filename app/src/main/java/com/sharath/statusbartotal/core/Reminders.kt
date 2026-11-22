package com.sharath.statusbartotal.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/** A card bill reminder that should be shown. [dueMonth] is the month of the due date. */
data class ReminderDue(
    val code: String,
    val dueMonth: YearMonth,
    val dueDate: LocalDate,
    val moment: LocalDateTime
)

object Reminders {
    const val DAYS_BEFORE = 3L
    val TIME: LocalTime = LocalTime.of(9, 0)

    /** 09:00, three days before the due date (the due day is clamped to the month's length). */
    fun moment(day: Int, dueMonth: YearMonth): LocalDateTime =
        RecurringRules.dueDate(day, dueMonth).minusDays(DAYS_BEFORE).atTime(TIME)

    fun key(code: String, dueMonth: YearMonth): String = "remind:$code@$dueMonth"

    /** Reminders whose moment has come, whose due date has not passed, and that were not shown yet. */
    fun missing(
        dueDays: Map<String, Int>, notes: Set<String>, now: LocalDateTime
    ): List<ReminderDue> {
        val out = ArrayList<ReminderDue>()
        val today = now.toLocalDate()
        val first = YearMonth.from(today)
        for ((code, day) in dueDays) {
            for (k in 0..1L) {
                val m = first.plusMonths(k)
                val moment = moment(day, m)
                val due = RecurringRules.dueDate(day, m)
                if (moment.isAfter(now) || due.isBefore(today)) continue
                if (key(code, m) in notes) continue
                out.add(ReminderDue(code, m, due, moment))
            }
        }
        return out.sortedBy { it.moment }
    }

    /** The first reminder moment strictly after [now] that was not shown yet. */
    fun nextAfter(
        dueDays: Map<String, Int>, notes: Set<String>, now: LocalDateTime
    ): LocalDateTime? {
        var best: LocalDateTime? = null
        val first = YearMonth.from(now)
        for ((code, day) in dueDays) {
            for (k in 0..2L) {
                val m = first.plusMonths(k)
                val moment = moment(day, m)
                if (!moment.isAfter(now) || key(code, m) in notes) continue
                if (best == null || moment.isBefore(best)) best = moment
            }
        }
        return best
    }
}
