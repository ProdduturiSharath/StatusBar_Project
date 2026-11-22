package com.sharath.statusbartotal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RupeeFormatTest {
    private val r = "₹"

    @Test fun zero() = assertEquals("${r}0", RupeeFormat.exact(0))
    @Test fun small() = assertEquals("${r}850", RupeeFormat.exact(850))
    @Test fun thousands() = assertEquals("${r}14,500", RupeeFormat.exact(14500))
    @Test fun lakhs() = assertEquals("${r}1,23,456", RupeeFormat.exact(123456))
    @Test fun negative() = assertEquals("-${r}450", RupeeFormat.exact(-450))

    @Test fun groupingForOneToTwelveDigits() {
        val expected = listOf(
            "1", "12", "123", "1,234", "12,345", "1,23,456", "12,34,567",
            "1,23,45,678", "12,34,56,789", "1,23,45,67,890", "12,34,56,78,901",
            "1,23,45,67,89,012"
        )
        var n = 0L
        for (i in 1..12) {
            n = n * 10 + (i % 10)
            assertEquals("${r}${expected[i - 1]}", RupeeFormat.exact(n))
            assertEquals("-${r}${expected[i - 1]}", RupeeFormat.exact(-n))
        }
    }

    @Test fun boundaries() {
        assertEquals("${r}999", RupeeFormat.exact(999))
        assertEquals("${r}1,000", RupeeFormat.exact(1000))
        assertEquals("${r}99,999", RupeeFormat.exact(99999))
        assertEquals("${r}1,00,000", RupeeFormat.exact(100000))
        assertEquals("${r}99,99,999", RupeeFormat.exact(9999999))
        assertEquals("${r}1,00,00,000", RupeeFormat.exact(10000000))
    }

    @Test fun signedPositive() = assertEquals("+${r}450", RupeeFormat.signed(450))
    @Test fun signedNegative() = assertEquals("-${r}405", RupeeFormat.signed(-405))
    @Test fun signedGrouped() = assertEquals("+${r}1,23,456", RupeeFormat.signed(123456))
}
