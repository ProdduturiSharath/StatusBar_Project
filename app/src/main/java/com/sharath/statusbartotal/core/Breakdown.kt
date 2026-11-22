package com.sharath.statusbartotal.core

object Breakdown {
    /**
     * One "label amount" line per non-zero bucket: UPI, CC (no card), CC UPI (no card), then cards
     * sorted by code; a card with CC UPI entries gets a second "incl. CC UPI" line.
     * [cardNames] maps lowercase code to the saved name; a missing code shows "ABC (deleted)".
     */
    fun lines(
        buckets: Map<String, Long>,
        cardNames: Map<String, String>,
        cardCcUpi: Map<String, Long> = emptyMap()
    ): List<String> {
        val out = ArrayList<String>()
        fun add(label: String, key: String) {
            val v = buckets[key] ?: 0L
            if (v != 0L) out.add("$label ${RupeeFormat.exact(v)}")
        }
        add("UPI", "UPI")
        add("CC (no card)", "CC")
        add("CC UPI (no card)", "CCUPI")
        for (key in buckets.keys.filter { it.startsWith("card:") }.sorted()) {
            val code = key.removePrefix("card:")
            val upper = code.uppercase()
            val name = cardNames[code]
            val label = if (name != null) "$name ($upper)" else "$upper (deleted)"
            add(label, key)
            val upi = cardCcUpi[code] ?: 0L
            if (upi != 0L && (buckets[key] ?: 0L) != 0L) out.add("  incl. CC UPI ${RupeeFormat.exact(upi)}")
        }
        return out
    }
}
