package com.sharath.statusbartotal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IconLabelTest {
    private fun check(total: Long, expected: String) {
        assertEquals(expected, IconLabel.text(total))
        assertEquals(listOf(expected), IconLabel.lines(total))
    }

    @Test fun table() {
        check(0, "0")
        check(5, "0")
        check(10, "0.01")
        check(150, "0.15")
        check(850, "0.85")
        check(999, "0.99")
        check(1000, "1")
        check(1050, "1.05")
        check(2700, "2.7")
        check(2750, "2.75")
        check(9999, "9.99")
        check(10000, "10")
        check(14500, "14.5")
        check(14560, "14.5")
        check(99999, "99.9")
        check(100000, "100")
        check(145500, "145")
        check(1234567, "1234")
        check(12345678901, "12345678")
    }

    @Test fun negativesRoundTowardZero() {
        check(-5, "0")
        check(-150, "-0.15")
        check(-2750, "-2.75")
        check(-14560, "-14.5")
        check(-145500, "-145")
    }
}
