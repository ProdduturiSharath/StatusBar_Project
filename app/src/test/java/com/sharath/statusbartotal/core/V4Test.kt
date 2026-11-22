package com.sharath.statusbartotal.core

import com.sharath.statusbartotal.data.HistoryItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V4Test {
    private fun dt(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) = LocalDateTime.of(y, m, d, h, min)
    private fun ym(s: String) = YearMonth.parse(s)

    // ---- Budget

    @Test fun budgetStates() {
        assertEquals(BudgetLevel.OK, Budget.level(7_900, 10_000))
        assertEquals(BudgetLevel.WARN, Budget.level(8_000, 10_000))
        assertEquals(BudgetLevel.WARN, Budget.level(10_000, 10_000))
        assertEquals(BudgetLevel.OVER, Budget.level(10_001, 10_000))
        assertEquals(BudgetLevel.OK, Budget.level(-500, 10_000))
        assertTrue(Budget.isOver(10_001, 10_000))
        assertEquals(false, Budget.isOver(10_000, 10_000))
        assertEquals(0.5f, Budget.fraction(5_000, 10_000), 0.0001f)
        assertEquals(1f, Budget.fraction(50_000, 10_000), 0.0001f)
        assertEquals(0f, Budget.fraction(-5, 10_000), 0.0001f)
    }

    @Test fun budgetTexts() {
        assertEquals("₹14,500 of ₹30,000 · ₹15,500 left", Budget.heroLine(14_500, 30_000))
        assertEquals("₹31,200 of ₹30,000 · ₹1,200 over budget", Budget.heroLine(31_200, 30_000))
        assertEquals("₹15,500 left of ₹30,000", Budget.notificationLine(14_500, 30_000))
        assertEquals("₹1,200 over budget", Budget.notificationLine(31_200, 30_000))
        assertEquals("Over budget: ₹31,200 of ₹30,000", Budget.alertText(31_200, 30_000))
    }

    @Test fun alertKeyIsPerCycleAndBudget() {
        assertEquals("budget@0:30000", Budget.alertKey(0, 30_000))
        assertEquals("budget@12:30000", Budget.alertKey(12, 30_000))
        assertTrue(Budget.alertKey(0, 30_000) != Budget.alertKey(0, 20_000))
    }

    // ---- Reminders

    @Test fun reminderIsThreeDaysBeforeAt0900() {
        assertEquals(dt(2026, 12, 12, 9), Reminders.moment(15, ym("2026-12")))
    }

    @Test fun reminderClampsToMonthEnd() {
        // Day 31 in February 2027 is the 28th; three days before is the 25th.
        assertEquals(dt(2027, 2, 25, 9), Reminders.moment(31, ym("2027-02")))
    }

    @Test fun reminderCanFallInPreviousMonth() {
        assertEquals(dt(2026, 11, 29, 9), Reminders.moment(2, ym("2026-12")))
    }

    @Test fun missingSkipsNotedAndPassed() {
        val cards = mapOf("abc" to 15)
        // Before the moment: nothing yet.
        assertTrue(Reminders.missing(cards, emptySet(), dt(2026, 12, 12, 8, 59)).isEmpty())
        // After the moment, due date not passed: shown.
        val m = Reminders.missing(cards, emptySet(), dt(2026, 12, 13, 10))
        assertEquals(1, m.size)
        assertEquals(ym("2026-12"), m[0].dueMonth)
        assertEquals(LocalDate.of(2026, 12, 15), m[0].dueDate)
        // On the due date itself it is still shown.
        assertEquals(1, Reminders.missing(cards, emptySet(), dt(2026, 12, 15, 10)).size)
        // After the due date: skipped.
        assertTrue(Reminders.missing(cards, emptySet(), dt(2026, 12, 16, 10)).isEmpty())
        // Already noted: skipped.
        assertTrue(Reminders.missing(cards, setOf("remind:abc@2026-12"), dt(2026, 12, 13, 10)).isEmpty())
    }

    @Test fun missingFindsNextMonthsEarlyDueDay() {
        // Due on the 2nd: reminder on 29 Nov for the December due date.
        val m = Reminders.missing(mapOf("abc" to 2), emptySet(), dt(2026, 11, 30, 9))
        assertEquals(listOf(ym("2026-12")), m.map { it.dueMonth })
    }

    @Test fun nextAfterSkipsNoted() {
        val cards = mapOf("abc" to 15)
        assertEquals(dt(2026, 12, 12, 9), Reminders.nextAfter(cards, emptySet(), dt(2026, 11, 23, 10)))
        assertEquals(
            dt(2027, 1, 12, 9),
            Reminders.nextAfter(cards, setOf("remind:abc@2026-12"), dt(2026, 11, 23, 10))
        )
        assertNull(Reminders.nextAfter(emptyMap(), emptySet(), dt(2026, 11, 23)))
    }

    // ---- Scheduling order with all event kinds

    @Test fun planOrdersAllEventKinds() {
        val r = Recurring(
            1, RecurringType.AUTOPAY, 100, Category.UPI, null, 12, ym("2026-12"), null, "Net"
        )
        val events = DuePlanner.plan(
            listOf(r), emptyMap(), 12, dt(2026, 11, 20), emptySet(), dt(2026, 12, 12, 10),
            mapOf("abc" to 15), emptySet()
        )
        // Reminder moment for due 15 Dec is 12 Dec 09:00; reset 00:00; recurring 00:05.
        assertEquals(
            listOf("AutoResetDue", "RecurringDue", "CardReminderDue"),
            events.map { it::class.simpleName }
        )
    }

    @Test fun planSkipsReminderWhoseDueDatePassed() {
        val events = DuePlanner.plan(
            emptyList(), emptyMap(), null, null, emptySet(), dt(2026, 12, 20, 10),
            mapOf("abc" to 15), emptySet()
        )
        assertTrue(events.isEmpty())
    }

    @Test fun nextWakeIncludesReminder() {
        val now = dt(2026, 11, 23, 10)
        assertEquals(
            dt(2026, 12, 12, 9),
            DuePlanner.nextWake(emptyList(), emptyMap(), null, now, mapOf("abc" to 15), emptySet())
        )
        // A closer auto-reset wins.
        assertEquals(
            dt(2026, 11, 24),
            DuePlanner.nextWake(emptyList(), emptyMap(), 24, now, mapOf("abc" to 15), emptySet())
        )
    }

    // ---- Cycles, summary, report

    private fun entry(
        id: Int, t: Long, amt: Long, cat: Category = Category.UPI, card: String? = null,
        source: String? = null
    ) = HistoryItem.Entry(id, t, amt, cat, card, source, false)

    private fun history(): List<HistoryItem> = listOf(
        entry(1, 1000, 500),
        entry(2, 2000, 200, Category.CC, "abc"),
        HistoryItem.Reset(3, 3000, false),
        entry(4, 4000, 900),
        entry(5, 5000, 100, Category.CCUPI, "abc", "7@2026-11"),
        HistoryItem.Reset(6, 6000, true, ym("2026-12")),
        entry(7, 7000, 50)
    )

    @Test fun cycleTotalsAndKeys() {
        val c = Cycles.build(history())
        assertEquals(listOf(0, 3, 6), c.map { it.key })
        assertEquals(listOf(700L, 1000L, 50L), c.map { it.total })
        assertEquals(listOf<Long?>(1000L, 3000L, 6000L), c.map { it.startMillis })
        assertEquals(mapOf("abc" to 100L), c[1].cardCcUpi)
        assertEquals(mapOf("UPI" to 900L, "card:abc" to 100L), c[1].buckets)
        assertTrue(c[2].isCurrent)
    }

    @Test fun summaryDeltasNewestFirst() {
        val rows = Summary.build(Cycles.build(history()))
        assertEquals(listOf(6, 3, 0), rows.map { it.cycle.key })
        assertEquals(listOf<Long?>(-950L, 300L, null), rows.map { it.delta })
    }

    @Test fun summaryHidesEmptyPastCyclesButKeepsEmptyCurrentAfterReset() {
        val h = listOf(entry(1, 1000, 500), HistoryItem.Reset(2, 2000, false), HistoryItem.Reset(3, 3000, false))
        val rows = Summary.build(Cycles.build(h))
        assertEquals(listOf(3, 0), rows.map { it.cycle.key })
        assertEquals(-500L, rows[0].delta)
        assertTrue(Summary.build(Cycles.build(emptyList())).isEmpty())
    }

    @Test fun csvContent() {
        val cycle = Cycles.build(history())[1]
        val csv = ReportBuilder.csv(
            cycle, 6000, 30_000, mapOf("abc" to "HDFC, Millennia"), mapOf(7L to "Net"), ZoneOffset.UTC
        )
        val lines = csv.trimEnd().split("\n")
        assertEquals("Cycle start,1970-01-01", lines[0])
        assertEquals("Cycle end,1970-01-01", lines[1])
        assertEquals("Total,1000", lines[2])
        assertEquals("Budget,30000", lines[3])
        assertEquals("Bucket,Amount", lines[4])
        assertEquals("UPI,900", lines[5])
        assertEquals("\"HDFC, Millennia (ABC)\",100", lines[6])
        assertEquals("", lines[7])
        assertEquals("Date,Time,Amount,Category,Card code,Card name,Type", lines[8])
        assertEquals("1970-01-01,00:00,900,UPI,,,Manual", lines[9])
        assertEquals("1970-01-01,00:00,100,CC UPI,ABC,\"HDFC, Millennia\",Auto: Net", lines[10])
        assertEquals(11, lines.size)
    }

    @Test fun csvWithoutBudgetAndUndoRows() {
        val h = listOf(entry(1, 1000, 500), entry(2, 2000, -500, source = "undo#1"))
        val csv = ReportBuilder.csv(Cycles.build(h)[0], 3000, null, emptyMap(), emptyMap(), ZoneOffset.UTC)
        assertTrue(!csv.contains("Budget"))
        assertTrue(csv.contains("1970-01-01,00:00,-500,UPI,,,Undo"))
    }

    @Test fun fileNames() {
        val zone = ZoneId.of("UTC")
        val start = LocalDate.of(2026, 11, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 12, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(
            "StatusBarTotal_2026-11-15_to_2026-12-15.csv", ReportBuilder.fileName(start, end, zone, null)
        )
        val at = LocalDateTime.of(2026, 12, 3, 14, 7).atZone(zone).toInstant().toEpochMilli()
        assertEquals(
            "StatusBarTotal_2026-11-15_to_2026-12-03_exported_20261203-1407.csv",
            ReportBuilder.fileName(start, at, zone, at)
        )
    }

    @Test fun breakdownLabelsAndCcUpiLine() {
        val lines = Breakdown.lines(
            mapOf("UPI" to 100L, "CC" to 50L, "CCUPI" to 25L, "card:abc" to 9200L),
            mapOf("abc" to "HDFC Millennia"), mapOf("abc" to 1200L)
        )
        assertEquals(
            listOf(
                "UPI ₹100", "CC (no card) ₹50", "CC UPI (no card) ₹25",
                "HDFC Millennia (ABC) ₹9,200", "  incl. CC UPI ₹1,200"
            ),
            lines
        )
    }
}
