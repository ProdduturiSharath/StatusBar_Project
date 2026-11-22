package com.sharath.statusbartotal.data

import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.RecurringRules
import java.io.File
import java.time.YearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LedgerV4Test {
    private lateinit var dir: File
    private lateinit var file: File
    private lateinit var ledger: Ledger

    @Before fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "ledger_v4_test_" + System.nanoTime())
        dir.mkdirs()
        file = File(dir, "ledger.txt")
        ledger = Ledger(file)
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    @Test fun singleEntryTombstone() {
        file.writeText("E,1,100\nE,2,50,UPI,,\nE,3,25,UPI,,\n")
        assertTrue(ledger.deleteRange(9, 2, 2))
        assertEquals("E,1,100\nE,2,50,UPI,,\nE,3,25,UPI,,\nD,9,2-2\n", file.readText())
        val s = ledger.snapshot()
        assertEquals(125L, s.total)
        assertEquals(25L, s.lastEntry)
        assertEquals(listOf(1, 3), ledger.history().map { it.id })
        assertEquals(4, s.lastLineId)
    }

    @Test fun rangeTombstoneAndLaterEntriesSurvive() {
        file.writeText("E,1,100\nE,2,50,UPI,,\nE,3,25,UPI,,\n")
        assertTrue(ledger.deleteRange(9, 1, 3))
        assertEquals(0L, ledger.snapshot().total)
        assertTrue(ledger.addEntry(7, 10, Category.UPI, null, null))
        assertEquals(7L, ledger.snapshot().total)
    }

    @Test fun deleteAllKeepsResetBoundaries() {
        file.writeText("E,1,100\nR,2\nE,3,50,UPI,,\nR,4,auto@2026-12\nE,5,7,UPI,,\n")
        assertTrue(ledger.deleteRange(9, 1, 5))
        val h = ledger.history()
        assertEquals(listOf(2, 4), h.map { it.id })
        assertTrue(h.all { it is HistoryItem.Reset })
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertEquals(4, s.cycleKey)
        assertEquals(setOf(YearMonth.of(2026, 12)), s.autoResetMonths)
    }

    @Test fun deletingPastCycleDoesNotChangeCurrentTotal() {
        file.writeText("E,1,100\nE,2,200,UPI,,\nR,3\nE,4,50,UPI,,\n")
        assertEquals(50L, ledger.snapshot().total)
        assertEquals(50L, ledger.snapshot(1..2).total)
        assertTrue(ledger.deleteRange(9, 1, 2))
        assertEquals(50L, ledger.snapshot().total)
    }

    @Test fun deletingCurrentCycleChangesCurrentTotal() {
        file.writeText("E,1,100\nR,2\nE,3,50,UPI,,\nE,4,20,UPI,,\n")
        assertEquals(70L, ledger.snapshot().total)
        assertEquals(0L, ledger.snapshot(3..4).total)
        assertEquals(20L, ledger.snapshot(3..3).total)
    }

    @Test fun undoPairDeletedFromEitherSide() {
        file.writeText("E,1,100\nE,2,40,UPI,,\nE,3,-40,UPI,,undo#2\nE,4,5,UPI,,\n")
        assertEquals(105L, ledger.snapshot().total)
        // Deleting the original also hides its undo.
        assertEquals(105L, ledger.snapshot(2..2).total)
        // The preview writes nothing.
        assertEquals(listOf(1, 2, 3, 4), ledger.history().map { it.id })
        ledger.deleteRange(9, 2, 2)
        assertEquals(listOf(1, 4), ledger.history().map { it.id })
        assertEquals(105L, ledger.snapshot().total)
    }

    @Test fun undoTombstonedAlsoRemovesOriginal() {
        file.writeText("E,1,100\nE,2,40,UPI,,\nE,3,-40,UPI,,undo#2\n")
        ledger.deleteRange(9, 3, 3)
        assertEquals(listOf(1), ledger.history().map { it.id })
        assertEquals(100L, ledger.snapshot().total)
    }

    @Test fun undoInLaterCycleHiddenWhenPastOriginalDeleted() {
        // Entry 2 (past cycle) was undone in the current cycle by line 5.
        file.writeText("E,1,100\nE,2,40,UPI,,\nR,3\nE,4,10,UPI,,\nE,5,-40,UPI,,undo#2\n")
        assertEquals(-30L, ledger.snapshot().total)
        // The same snapshot function with the tombstone previews the new current total.
        assertEquals(10L, ledger.snapshot(1..2).total)
        ledger.deleteRange(9, 1, 2)
        assertEquals(10L, ledger.snapshot().total)
        assertEquals(listOf(3, 4), ledger.history().map { it.id })
    }

    @Test fun undoRefusedOnDeletedEntry() {
        file.writeText("E,1,100\n")
        ledger.deleteRange(9, 1, 1)
        assertFalse(ledger.undo(1, 10))
    }

    @Test fun deletedRecurringEntryStillMarksMonthApplied() {
        assertTrue(ledger.addEntry(500, 1, Category.UPI, null, "7@2026-11"))
        ledger.deleteRange(9, 1, 1)
        val s = ledger.snapshot()
        assertEquals(0L, s.total)
        assertEquals(setOf(YearMonth.of(2026, 11)), RecurringRules.appliedMonths(7, s.appliedSources))
    }

    @Test fun tombstoneNeverDeletesNotesAndNotesAreRead() {
        file.writeText("E,1,100\nN,2,budget@0:30000\nE,3,5,UPI,,\n")
        ledger.deleteRange(9, 1, 3)
        val s = ledger.snapshot()
        assertEquals(setOf("budget@0:30000"), s.notes)
        assertEquals(0L, s.total)
    }

    @Test fun addNoteAppendsAndRejectsBadKeys() {
        assertTrue(ledger.addNote(5, "report@0"))
        assertFalse(ledger.addNote(5, "a,b"))
        assertFalse(ledger.addNote(5, ""))
        assertEquals("N,5,report@0\n", file.readText())
        assertEquals(setOf("report@0"), ledger.snapshot().notes)
    }

    @Test fun cycleKeyIsLastResetLineId() {
        file.writeText("E,1,100\nN,2,x@1\nR,3\nE,4,5,UPI,,\nD,5,4-4\nR,6\n")
        assertEquals(6, ledger.snapshot().cycleKey)
        file.writeText("E,1,100\n")
        assertEquals(0, ledger.snapshot().cycleKey)
    }

    @Test fun malformedTombstonesAndNotesIgnored() {
        file.writeText("E,1,100\nD,1\nD,x,1-1\nD,1,a-b\nD,1,3-1\nN,1\nN,x,key\nE,2,5,UPI,,\n")
        assertEquals(105L, ledger.snapshot().total)
    }

    @Test fun cardCcUpiTrackedPerCard() {
        file.writeText("E,1,900,CC,abc,\nE,2,100,CCUPI,abc,\nE,3,40,CCUPI,xyz,\n")
        val s = ledger.snapshot()
        assertEquals(mapOf("card:abc" to 1000L, "card:xyz" to 40L), s.buckets)
        assertEquals(mapOf("abc" to 100L, "xyz" to 40L), s.cardCcUpi)
    }

    @Test fun preV4LedgerBucketsUnchanged() {
        // V1, V2 and V3 style lines, including untagged CC / CC UPI and card entries.
        file.writeText(
            "E,1,100\nE,2,50,CC,,\nE,3,25,CCUPI,,\nE,4,200,CC,abc,\nE,5,30,CCUPI,abc,\n" +
                "E,6,10,CC,,7@2026-10\nR,7\nE,8,60,UPI,,\nR,9,auto@2026-11\nE,10,5,CCUPI,,\n"
        )
        val s = ledger.snapshot()
        assertEquals(5L, s.total)
        assertEquals(mapOf("CCUPI" to 5L), s.buckets)
        file.writeText("E,1,100\nE,2,50,CC,,\nE,3,25,CCUPI,,\nE,4,200,CC,abc,\nE,5,30,CCUPI,abc,\nE,6,10,CC,,7@2026-10\n")
        val s2 = ledger.snapshot()
        assertEquals(
            mapOf("UPI" to 100L, "CC" to 60L, "CCUPI" to 25L, "card:abc" to 230L), s2.buckets
        )
        assertEquals(415L, s2.total)
    }
}
