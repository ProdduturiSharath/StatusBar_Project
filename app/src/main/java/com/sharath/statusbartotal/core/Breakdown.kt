package com.sharath.statusbartotal.core

object Breakdown {
    /**
     * One "label amount" line per non-zero bucket: UPI, CC, CC UPI, then cards sorted by code.
     * [cardNames] maps lowercase code to the saved name; a missing code shows "ABC (deleted)".
     */
    fun lines(buckets: Map<String, Long>, cardNames: Map<String, String>): List<String> {
        val out = ArrayList<String>()
        fun add(label: String, key: String) {
            val v = buckets[key] ?: 0L
            if (v != 0L) out.add("$label ${RupeeFormat.exact(v)}")
        }
        add("UPI", "UPI")
        add("CC", "CC")
        add("CC UPI", "CCUPI")
        for (key in buckets.keys.filter { it.startsWith("card:") }.sorted()) {
            val code = key.removePrefix("card:")
            val upper = code.uppercase()
            val name = cardNames[code]
            val label = if (name != null) "$name ($upper)" else "$upper (deleted)"
            add(label, key)
        }
        return out
    }
}
