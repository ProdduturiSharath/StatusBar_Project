package com.sharath.statusbartotal.core

/**
 * Status bar icon text: the total in thousands of rupees, numbers only, one line.
 * At most 3 significant digits below 1,00,000; always rounded toward zero; trailing zeros dropped.
 * 150 -> "0.15", 2,750 -> "2.75", 14,500 -> "14.5", 1,45,500 -> "145", 12,34,567 -> "1234".
 * Chosen by the user after phone tests (2026-11-23): unit letters (K/L/Cr) were not wanted.
 */
object IconLabel {
    fun lines(total: Long): List<String> = listOf(text(total))

    fun text(total: Long): String {
        val negative = total < 0
        val abs = if (negative) -total else total
        // Decimal places shown for the value in thousands.
        val decimals = when {
            abs < 10_000L -> 2      // 0.15, 2.75
            abs < 100_000L -> 1     // 14.5
            else -> 0               // 145, 1234
        }
        var scaled = abs / pow10(3 - decimals)   // value in thousands times 10^decimals, truncated
        var places = decimals
        // Drop trailing zeros after the decimal point.
        while (places > 0 && scaled % 10 == 0L) {
            scaled /= 10
            places--
        }
        val digits = scaled.toString()
        val number = if (places == 0) {
            digits
        } else {
            val padded = digits.padStart(places + 1, '0')
            padded.dropLast(places) + "." + padded.takeLast(places)
        }
        return if (negative && number != "0") "-$number" else number
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }
}
