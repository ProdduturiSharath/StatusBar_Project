package com.sharath.statusbartotal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryParserTest {
    @Test fun plainNumber() = assertEquals(450L, EntryParser.parse("450"))
    @Test fun plusSign() = assertEquals(450L, EntryParser.parse("+450"))
    @Test fun minusSign() = assertEquals(-200L, EntryParser.parse("-200"))
    @Test fun commas() = assertEquals(1200L, EntryParser.parse("1,200"))
    @Test fun indianCommasNegative() = assertEquals(-999999999L, EntryParser.parse("-99,99,99,999"))
    @Test fun spacesRemoved() = assertEquals(450L, EntryParser.parse(" 4 50 "))
    @Test fun nonBreakingSpaceRemoved() = assertEquals(450L, EntryParser.parse(" 450 "))
    @Test fun unicodeMinus() = assertEquals(-405L, EntryParser.parse("−405"))
    @Test fun nineDigitsAccepted() = assertEquals(999999999L, EntryParser.parse("999999999"))
    @Test fun negativeNineDigitsAccepted() = assertEquals(-999999999L, EntryParser.parse("-999999999"))
    @Test fun tenDigitsRejected() = assertNull(EntryParser.parse("1000000000"))
    @Test fun tenDigitsNegativeRejected() = assertNull(EntryParser.parse("-1000000000"))
    @Test fun leadingZerosWithinLimit() = assertEquals(45L, EntryParser.parse("0045"))
    @Test fun emptyRejected() = assertNull(EntryParser.parse(""))
    @Test fun blankRejected() = assertNull(EntryParser.parse("  "))
    @Test fun zeroRejected() = assertNull(EntryParser.parse("0"))
    @Test fun signedZeroRejected() {
        assertNull(EntryParser.parse("-0"))
        assertNull(EntryParser.parse("+0"))
    }
    @Test fun decimalRejected() {
        assertNull(EntryParser.parse("450.50"))
        assertNull(EntryParser.parse("1,200.5"))
    }
    @Test fun lettersRejected() {
        assertNull(EntryParser.parse("abc"))
        assertNull(EntryParser.parse("45a"))
    }
    @Test fun rupeeSignRejected() = assertNull(EntryParser.parse("₹450"))
    @Test fun signOnlyRejected() {
        assertNull(EntryParser.parse("-"))
        assertNull(EntryParser.parse("+"))
    }
    @Test fun doubleSignRejected() = assertNull(EntryParser.parse("--5"))
    @Test fun nonAsciiDigitsRejected() = assertNull(EntryParser.parse("٤٥"))

    private val cards = setOf("abc", "xyz")
    private fun ok(a: Long, c: Category, card: String?) = ParseResult.Ok(a, c, card)
    private fun entry(t: String) = EntryParser.parseEntry(t, cards)

    @Test fun grammarTable() {
        assertEquals(ok(450, Category.UPI, null), entry("450"))
        assertEquals(ok(450, Category.UPI, null), entry("450 upi"))
        assertEquals(ok(450, Category.CC, null), entry("450 cc"))
        assertEquals(ok(450, Category.CCUPI, null), entry("450 ccupi"))
        assertEquals(ok(450, Category.CC, "abc"), entry("450 abc"))
        assertEquals(ok(450, Category.CC, "abc"), entry("450 cc abc"))
        assertEquals(ok(450, Category.CCUPI, "abc"), entry("450 ccupi abc"))
        assertEquals(ok(-200, Category.CC, "abc"), entry("-200 abc"))
    }

    @Test fun caseInsensitiveAndWhitespaceRuns() {
        assertEquals(ok(450, Category.CCUPI, "abc"), entry("  450   CCUPI ABC "))
        assertEquals(ok(1200, Category.UPI, null), entry("1,200"))
        assertEquals(ok(-405, Category.UPI, null), entry("−405"))
    }

    @Test fun rejections() {
        assertEquals(ParseResult.BadAmount, entry(""))
        assertEquals(ParseResult.BadAmount, entry("abc"))
        assertEquals(ParseResult.BadAmount, entry("0 cc"))
        assertEquals(ParseResult.NoCard("200"), entry("1 200"))
        assertEquals(ParseResult.BadAmount, entry("450 upi abc"))
        assertEquals(ParseResult.BadAmount, entry("450 abc abc"))
        assertEquals(ParseResult.BadAmount, entry("450 cc abc x"))
        assertEquals(ParseResult.BadAmount, entry("450 cc cc"))
        assertEquals(ParseResult.BadAmount, entry("450 cc upi"))
        assertEquals(ParseResult.BadAmount, entry("450 ccupi ccupi"))
    }

    @Test fun unknownCard() {
        assertEquals(ParseResult.NoCard("qqq"), entry("450 qqq"))
        assertEquals(ParseResult.NoCard("qqq"), entry("450 cc QQQ"))
    }
}
