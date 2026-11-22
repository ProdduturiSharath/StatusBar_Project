package com.sharath.statusbartotal.data

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardsV4Test {
    private lateinit var dir: File
    private lateinit var f: File

    @Before fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "cards_v4_test_" + System.nanoTime())
        dir.mkdirs()
        f = File(dir, "cards.txt")
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    @Test fun oldTwoFieldLinesReadAsNoDueNoDefault() {
        f.writeText("abc,HDFC Millennia\nxyz,SBI Elite\n")
        assertEquals(
            listOf(Card("abc", "HDFC Millennia", null, false), Card("xyz", "SBI Elite", null, false)),
            Cards(f).read()
        )
    }

    @Test fun newFormatRoundTrip() {
        f.writeText("abc,HDFC Millennia,15,1\nxyz,SBI Elite,,0\n")
        val cards = Cards(f).read()
        assertEquals(Card("abc", "HDFC Millennia", 15, true), cards[0])
        assertEquals(Card("xyz", "SBI Elite", null, false), cards[1])
        assertEquals("abc,HDFC Millennia,15,1", Cards.format(cards[0]))
        assertEquals("xyz,SBI Elite,,0", Cards.format(cards[1]))
    }

    @Test fun addWritesNewFormatAndKeepsOldLines() {
        f.writeText("abc,Old\n")
        assertTrue(Cards(f).add("xyz", "New"))
        assertEquals("abc,Old,,0\nxyz,New,,0\n", f.readText())
    }

    @Test fun onlyOneCcUpiDefaultWhenSaving() {
        val cards = Cards(f)
        cards.add("abc", "A")
        cards.add("xyz", "B")
        assertTrue(cards.update("abc", "abc", "A", 5, true))
        assertTrue(cards.update("xyz", "xyz", "B", null, true))
        val read = cards.read()
        assertEquals(false, read.first { it.code == "abc" }.ccUpiDefault)
        assertEquals(true, read.first { it.code == "xyz" }.ccUpiDefault)
        assertEquals(5, read.first { it.code == "abc" }.dueDay)
    }

    @Test fun twoDefaultsInFileKeepFirstOnly() {
        f.writeText("abc,A,,1\nxyz,B,,1\n")
        val read = Cards(f).read()
        assertEquals(listOf(true, false), read.map { it.ccUpiDefault })
    }

    @Test fun updateRejectsBadInput() {
        val cards = Cards(f)
        cards.add("abc", "A")
        assertFalse(cards.update("abc", "abc", "   ", null, false))
        assertFalse(cards.update("abc", "abc", "A", 32, false))
        assertFalse(cards.update("zzz", "zzz", "A", null, false))
        assertEquals("A", cards.read()[0].name)
    }

    @Test fun deletingDefaultCardClearsDefault() {
        f.writeText("abc,A,,1\nxyz,B,,0\n")
        Cards(f).delete("abc")
        assertTrue(Cards(f).read().none { it.ccUpiDefault })
    }

    @Test fun settingsKeepBudgetAndAutoResetTogether() {
        val s = SettingsStore(File(dir, "settings.txt"))
        assertTrue(s.setBudget(30_000))
        assertEquals(30_000L, s.read().budget)
        assertTrue(s.setAutoResetDay(15, 1234))
        val r = s.read()
        assertEquals(15, r.autoResetDay)
        assertEquals(30_000L, r.budget)
        assertTrue(s.setBudget(null))
        assertEquals(null, s.read().budget)
        assertEquals(15, s.read().autoResetDay)
        // Old two-line file still reads.
        File(dir, "settings.txt").writeText("autoResetDay=3\nautoResetSince=99\n")
        assertEquals(AppSettings(3, 99L, null), s.read())
    }

    @org.junit.Test fun renameCodeMovesEntriesAndRecurringKeepsLineIds() {
        val dir = java.nio.file.Files.createTempDirectory("rn").toFile()
        val ledger = Ledger(java.io.File(dir, "ledger.txt"))
        val cat = com.sharath.statusbartotal.core.Category.CC
        org.junit.Assert.assertTrue(ledger.addEntry(100, 1, cat, "abc", null))
        org.junit.Assert.assertTrue(ledger.addEntry(50, 2, cat, "xyz", null))
        org.junit.Assert.assertTrue(ledger.reset(3))
        org.junit.Assert.assertTrue(ledger.addEntry(30, 4, cat, "abc", null))
        val before = java.io.File(dir, "ledger.txt").readLines().size
        org.junit.Assert.assertTrue(ledger.renameCard("abc", "hdf"))
        val lines = java.io.File(dir, "ledger.txt").readLines()
        org.junit.Assert.assertEquals(before, lines.size)
        org.junit.Assert.assertEquals("E,1,100,CC,hdf,", lines[0])
        org.junit.Assert.assertEquals("E,2,50,CC,xyz,", lines[1])
        val snap = ledger.snapshot()
        org.junit.Assert.assertEquals(30L, snap.buckets["card:hdf"])
        org.junit.Assert.assertEquals(null, snap.buckets["card:abc"])

        val cards = Cards(java.io.File(dir, "cards.txt"))
        org.junit.Assert.assertTrue(cards.add("abc", "A"))
        org.junit.Assert.assertTrue(cards.add("xyz", "B"))
        org.junit.Assert.assertFalse(cards.update("abc", "xyz", "A", null, false))
        org.junit.Assert.assertTrue(cards.update("abc", "hdf", "A", 5, true))
        org.junit.Assert.assertEquals(listOf("hdf", "xyz"), cards.read().map { it.code })
    }
}
