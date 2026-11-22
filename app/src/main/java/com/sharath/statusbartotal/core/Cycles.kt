package com.sharath.statusbartotal.core

import com.sharath.statusbartotal.data.HistoryItem

/**
 * One cycle: the entries between two reset lines. [key] is the line id of the reset line that
 * started it (0 for the first cycle). [startMillis] is that reset's time, or for the first cycle
 * the time of its first entry (null if it has none).
 */
class CycleInfo(
    val key: Int,
    val startMillis: Long?,
    val endReset: HistoryItem.Reset?,
    val startReset: HistoryItem.Reset?,
    val entries: List<HistoryItem.Entry>
) {
    val isCurrent: Boolean get() = endReset == null
    val total: Long get() = entries.sumOf { it.amount }

    /** Bucket totals ("UPI", "CC", "CCUPI", "card:<code>"). */
    val buckets: Map<String, Long>
        get() {
            val m = LinkedHashMap<String, Long>()
            for (e in entries) m[e.bucketKey] = (m[e.bucketKey] ?: 0L) + e.amount
            return m
        }

    /** The CC UPI part of each card bucket, by card code. */
    val cardCcUpi: Map<String, Long>
        get() {
            val m = LinkedHashMap<String, Long>()
            for (e in entries) {
                if (e.category == Category.CCUPI && e.card != null) {
                    m[e.card] = (m[e.card] ?: 0L) + e.amount
                }
            }
            return m
        }

    /** First line id belonging to this cycle (the line after its reset). */
    val firstLineId: Int get() = key + 1
}

object Cycles {
    /** Cycles from [history] (as returned by the ledger), oldest first; the last one is current. */
    fun build(history: List<HistoryItem>): List<CycleInfo> {
        val out = ArrayList<CycleInfo>()
        var key = 0
        var startReset: HistoryItem.Reset? = null
        var entries = ArrayList<HistoryItem.Entry>()
        fun start(): Long? = startReset?.timeMillis ?: entries.firstOrNull()?.timeMillis
        for (item in history) {
            when (item) {
                is HistoryItem.Entry -> entries.add(item)
                is HistoryItem.Reset -> {
                    out.add(CycleInfo(key, start(), item, startReset, entries))
                    key = item.id
                    startReset = item
                    entries = ArrayList()
                }
            }
        }
        out.add(CycleInfo(key, start(), null, startReset, entries))
        return out
    }
}

/** [delta] is this cycle's total minus the previous shown cycle's total, null for the oldest. */
data class SummaryRow(val cycle: CycleInfo, val delta: Long?)

object Summary {
    /**
     * Newest first. Cycles without entries are hidden, except the current one when it has a start
     * (right after a reset).
     */
    fun build(cycles: List<CycleInfo>): List<SummaryRow> {
        val shown = cycles.filter { it.entries.isNotEmpty() || (it.isCurrent && it.startMillis != null) }
        val out = ArrayList<SummaryRow>()
        for (i in shown.indices.reversed()) {
            val prev = shown.getOrNull(i - 1)
            out.add(SummaryRow(shown[i], prev?.let { shown[i].total - it.total }))
        }
        return out
    }
}
