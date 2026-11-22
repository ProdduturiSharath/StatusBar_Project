package com.sharath.statusbartotal.data

import com.sharath.statusbartotal.core.Category
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LedgerTest {
    private lateinit var dir: File
    private lateinit var file: File
    private lateinit var ledger: Ledger

    private fun Ledger.add(amount: Long, time: Long) = addEntry(amount, time, Category.UPI, null, null)

    @Before fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "ledger_test_" + System.nanoTime())
        dir.mkdirs()
        file = File(dir, "ledger.txt")
        ledger = Ledger(file)
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    @Test fun historyLineIdsAndUndoMarking() {
        file.writeText("E,1,450\nE,2,100,CC,abc,\nR,3\nE,4,50,UPI,,\nR,5,auto@2026-11\n")
        assertTrue(ledger.undo(2, 9))
        val h = ledger.history()
        assertEquals(listOf(1, 2, 3, 4, 5, 6), h.map { it.id })
        assertTrue((h[1] as HistoryItem.Entry).undone)
        assertFalse((h[0] as HistoryItem.Entry).undone)
        val undo = h[5] as HistoryItem.Entry
        assertEquals(-100L, undo.amount)
        assertEquals("abc", undo.card)
        assertEquals("undo#2", undo.source)
        assertFalse(undo.undone)
        assertFalse((h[2] as HistoryItem.Reset).auto)
        val auto = h[4] as HistoryItem.Reset
        assertTrue(auto.auto)
        assertEquals(java.time.YearMonth.of(2026, 11), auto.autoMonth)
    }

    @Test fun secondUndoAndUndoOfUndoRefused() {
        ledger.add(450, 1)
        assertTrue(ledger.undo(1, 2))
        assertFalse(ledger.undo(1, 3))
        assertFalse(ledger.undo(2, 3))
        assertFalse(ledger.undo(99, 3))
        assertEquals(0L, ledger.snapshot().total)
        assertEquals(2, ledger.history().size)
    }

    @Test fun undoSourceIsNotARecurringSource() {
        ledger.add(450, 1)
        ledger.undo(1, 2)
        val applied = com.sharath.statusbartotal.core.RecurringRules.appliedMonths(
            1, ledger.snapshot().appliedSources
        )
        assertTrue(applied.isEmpty())
    }

    @Test fun resetWritesAutoLineAndSnapshotTracksIt() {
        ledger.add(100, 1)
        assertTrue(ledger.reset(5, java.time.YearMonth.of(2026, 12)))
        assertEquals("E,1,100,UPI,,\nR,5,auto@2026-12\n", file.readText())
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertEquals(setOf(java.time.YearMonth.of(2026, 12)), s.autoResetMonths)
        assertEquals(5L, s.cycleStart)
    }

    @Test fun missingFileIsZero() {
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertNull(s.lastEntry)
    }

    @Test fun emptyFileIsZero() {
        file.writeText("")
        assertEquals(0L, ledger.snapshot().total)
    }

    @Test fun entriesSum() {
        assertTrue(ledger.add(450, 1))
        assertTrue(ledger.add(100, 2))
        val s = ledger.snapshot()
        assertEquals(550L, s.total)
        assertEquals(100L, s.lastEntry)
        assertEquals("E,1,450,UPI,,\nE,2,100,UPI,,\n", file.readText())
    }

    @Test fun resetZeroesTotalAndKeepsLines() {
        ledger.add(450, 1)
        ledger.add(100, 2)
        assertTrue(ledger.reset(3))
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertNull(s.lastEntry)
        assertEquals("E,1,450,UPI,,\nE,2,100,UPI,,\nR,3\n", file.readText())
        ledger.add(7, 4)
        assertEquals(7L, ledger.snapshot().total)
    }

    @Test fun resetOnEmptyAppendsLine() {
        ledger.reset(5)
        assertEquals("R,5\n", file.readText())
        assertEquals(0L, ledger.snapshot().total)
    }

    @Test fun cutOffLastLineNotCounted() {
        file.writeText("E,1,100\nE,1,45")
        val s = ledger.snapshot()
        assertEquals(100L, s.total)
        assertEquals(100L, s.lastEntry)
    }

    @Test fun cutOffPieceRemovedOnNextAppend() {
        file.writeText("E,1,100\nE,1,45")
        assertTrue(ledger.add(5, 2))
        assertEquals("E,1,100\nE,2,5,UPI,,\n", file.readText())
        assertEquals(105L, ledger.snapshot().total)
    }

    @Test fun cutOffWithoutAnyNewlineTruncatesToZero() {
        file.writeText("E,1,45")
        assertEquals(0L, ledger.snapshot().total)
        assertTrue(ledger.add(5, 2))
        assertEquals("E,2,5,UPI,,\n", file.readText())
        assertEquals(5L, ledger.snapshot().total)
    }

    @Test fun noNewlineAddedToFinishCutOffPiece() {
        file.writeText("E,1,100\nR,9")
        assertEquals(100L, ledger.snapshot().total)
        ledger.add(1, 3)
        assertFalse(file.readText().contains("R,9"))
        assertEquals(101L, ledger.snapshot().total)
    }

    @Test fun entriesAfterResetWithEarlierTimestampStillCount() {
        file.writeText("E,1000,100\nR,2000\nE,500,30\n")
        val s = ledger.snapshot()
        assertEquals(30L, s.total)
        assertEquals(30L, s.lastEntry)
    }

    @Test fun negativeTotals() {
        ledger.add(-450, 1)
        assertEquals(-450L, ledger.snapshot().total)
        ledger.add(100, 2)
        assertEquals(-350L, ledger.snapshot().total)
    }

    @Test fun malformedCompleteLinesSkipped() {
        file.writeText("garbage\nE,1,abc\nE,x,5\nE,1\nR\nR,zz\nE,1,2,3\nE,1,10\n\n")
        assertEquals(10L, ledger.snapshot().total)
    }

    @Test fun writeFailureReturnsFalse() {
        val bad = Ledger(File(File(dir, "missing_subdir"), "ledger.txt"))
        assertFalse(bad.add(1, 1))
        assertFalse(bad.reset(1))
    }

    @Test fun v1AndV2LinesMixed() {
        file.writeText("E,1,100\nE,2,50,CC,,\nE,3,25,CCUPI,abc,\nE,4,10,CC,abc,7@2026-11\n")
        val s = ledger.snapshot()
        assertEquals(185L, s.total)
        assertEquals(10L, s.lastEntry)
        assertEquals(mapOf("UPI" to 100L, "CC" to 50L, "card:abc" to 35L), s.buckets)
        assertEquals(setOf("7@2026-11"), s.appliedSources)
    }

    @Test fun trailingEmptyFieldsParse() {
        file.writeText("E,1,450,UPI,,\n")
        assertEquals(450L, ledger.snapshot().buckets["UPI"])
    }

    @Test fun unknownCategoryAndBadPartCountSkipped() {
        file.writeText("E,1,5,XX,,\nE,1,5,UPI\nE,1,5,UPI,,,\nE,1,6,CC,,\n")
        assertEquals(6L, ledger.snapshot().total)
    }

    @Test fun resetClearsBucketsButKeepsAppliedSources() {
        assertTrue(ledger.addEntry(100, 1, Category.CC, "abc", "9@2026-10"))
        ledger.reset(2)
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertTrue(s.buckets.isEmpty())
        assertEquals(setOf("9@2026-10"), s.appliedSources)
        ledger.addEntry(5, 3, Category.UPI, null, null)
        assertEquals(mapOf("UPI" to 5L), ledger.snapshot().buckets)
    }

    @Test fun writesV2Line() {
        ledger.addEntry(450, 1, Category.CCUPI, "abc", "5@2026-11")
        assertEquals("E,1,450,CCUPI,abc,5@2026-11\n", file.readText())
    }
}
