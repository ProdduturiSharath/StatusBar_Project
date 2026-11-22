package com.sharath.statusbartotal.core

object RupeeFormat {
    /** Indian-grouped rupees: ₹0, ₹14,500, ₹1,23,456, -₹450. */
    fun exact(amount: Long): String {
        val digits = amount.toString().removePrefix("-")
        val sign = if (amount < 0) "-" else ""
        return sign + "₹" + group(digits)
    }

    /** +₹450 or -₹405 for the "Last entry" line. */
    fun signed(amount: Long): String =
        if (amount < 0) exact(amount) else "+" + exact(amount)

    private fun group(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.substring(0, digits.length - 3)
        val tail = digits.substring(digits.length - 3)
        val parts = ArrayList<String>()
        var end = head.length
        while (end > 0) {
            val start = maxOf(0, end - 2)
            parts.add(0, head.substring(start, end))
            end = start
        }
        return parts.joinToString(",") + "," + tail
    }
}
