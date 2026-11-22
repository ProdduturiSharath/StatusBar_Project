package com.sharath.statusbartotal.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeParseException

data class Recurring(
    val id: Long,
    val type: RecurringType,
    val amount: Long,
    val category: Category,
    val card: String?,
    val day: Int,
    val startMonth: YearMonth,
    val months: Int?,
    val name: String
)

object RecurringRules {
    private val ALARM_TIME: LocalTime = LocalTime.of(0, 5)

    /** The chosen day in the month, or the month's last day if the month is shorter. */
    fun dueDate(day: Int, month: YearMonth): LocalDate =
        month.atDay(minOf(day, month.lengthOfMonth()))

    /** Months to be added now, oldest first. */
    fun missingEntries(r: Recurring, applied: Set<YearMonth>, today: LocalDate): List<YearMonth> {
        val current = YearMonth.from(today)
        val result = ArrayList<YearMonth>()
        var m = r.startMonth
        while (!m.isAfter(current)) {
            if (m !in applied && !dueDate(r.day, m).isAfter(today)) result.add(m)
            m = m.plusMonths(1)
        }
        if (r.type == RecurringType.EMI && r.months != null) {
            val allowed = r.months - applied.size
            if (allowed <= 0) return emptyList()
            if (result.size > allowed) return result.subList(0, allowed).toList()
        }
        return result
    }

    /** Next 00:05 on a due date for a month not yet applied; null for a finished EMI. */
    @Suppress("UNUSED_PARAMETER")
    fun nextDue(r: Recurring, applied: Set<YearMonth>, now: LocalDateTime): LocalDateTime? {
        if (isFinished(r, applied)) return null
        var m = r.startMonth
        while (m in applied) m = m.plusMonths(1)
        return LocalDateTime.of(dueDate(r.day, m), ALARM_TIME)
    }

    fun isFinished(r: Recurring, applied: Set<YearMonth>): Boolean =
        r.type == RecurringType.EMI && r.months != null && r.months - applied.size <= 0

    fun monthsLeft(r: Recurring, applied: Set<YearMonth>): Int =
        maxOf(0, (r.months ?: 0) - applied.size)

    /** Months applied to recurring item [id], read from "<id>@<yyyy-MM>" sources. */
    fun appliedMonths(id: Long, sources: Set<String>): Set<YearMonth> {
        val prefix = "$id@"
        val out = HashSet<YearMonth>()
        for (s in sources) {
            if (!s.contains('@') || !s.startsWith(prefix)) continue
            try {
                out.add(YearMonth.parse(s.substring(prefix.length)))
            } catch (e: DateTimeParseException) {
                // ignore malformed
            }
        }
        return out
    }
}
