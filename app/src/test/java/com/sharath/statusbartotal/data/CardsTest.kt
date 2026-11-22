package com.sharath.statusbartotal.data

import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.Recurring
import com.sharath.statusbartotal.core.RecurringType
import java.io.File
import java.time.YearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardsTest {
    private lateinit var dir: File

    @Before fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "cards_test_" + System.nanoTime())
        dir.mkdirs()
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    @Test fun codeValidation() {
        assertTrue(Cards.isValidCode("abc"))
        assertFalse(Cards.isValidCode("ab"))
        assertFalse(Cards.isValidCode("abcd"))
        assertFalse(Cards.isValidCode("ab1"))
        assertFalse(Cards.isValidCode("ABC"))
        assertFalse(Cards.isValidCode("upi"))
        assertFalse(Cards.isValidCode(""))
    }

    @Test fun nameCleaning() {
        assertEquals("HDFC Millennia", Cards.cleanName("  HDFC Millennia "))
        assertEquals("HDFC Millennia", Cards.cleanName("HDFC,Millennia"))
        assertEquals("a b c", Cards.cleanName("a\nb\rc"))
        assertNull(Cards.cleanName("   "))
        assertNull(Cards.cleanName("x".repeat(41)))
        assertEquals(40, Cards.cleanName("x".repeat(40))!!.length)
    }

    @Test fun roundTripAddDeleteAndUniqueness() {
        val f = File(dir, "cards.txt")
        val cards = Cards(f)
        assertTrue(cards.add("ABC", "HDFC Millennia"))
        assertFalse(cards.add("abc", "Other"))
        assertFalse(cards.add("cc", "Bad"))
        assertTrue(cards.add("xyz", "SBI, Elite"))
        assertEquals(listOf(Card("abc", "HDFC Millennia"), Card("xyz", "SBI  Elite")), cards.read())
        assertFalse(File(dir, "cards.txt.tmp").exists())
        assertTrue(cards.delete("abc"))
        assertEquals(listOf(Card("xyz", "SBI  Elite")), Cards(f).read())
    }

    @Test fun leftoverTmpAndMalformedLinesIgnored() {
        File(dir, "cards.txt.tmp").writeText("zzz,junk\n")
        val f = File(dir, "cards.txt")
        f.writeText("abc,Good\nbad line\nab,Short\n")
        assertEquals(listOf(Card("abc", "Good")), Cards(f).read())
        assertTrue(Cards(f).add("def", "Next"))
        assertEquals(2, Cards(f).read().size)
    }

    @Test fun recurringStoreRoundTrip() {
        val f = File(dir, "recurring.txt")
        val store = RecurringStore(f)
        val a = Recurring(
            1, RecurringType.EMI, 5000, Category.CC, "abc", 31, YearMonth.parse("2026-11"), 12, "Phone EMI"
        )
        val b = Recurring(
            2, RecurringType.AUTOPAY, 649, Category.UPI, null, 5, YearMonth.parse("2026-11"), null, "Net"
        )
        assertTrue(store.add(a))
        assertTrue(store.add(b))
        assertEquals(listOf(a, b), store.read())
        assertTrue(store.delete(1))
        assertEquals(listOf(b), RecurringStore(f).read())
    }
}
