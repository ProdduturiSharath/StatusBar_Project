package com.sharath.statusbartotal.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReportBuilder {
    private val DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ENGLISH)

    /**
     * StatusBarTotal_<start>_to_<end>.csv; an on-demand export also gets
     * "_exported_<yyyyMMdd-HHmm>" so nothing collides.
     */
    fun fileName(startMillis: Long, endMillis: Long, zone: ZoneId, exportedAtMillis: Long?): String {
        val s = DAY.format(Instant.ofEpochMilli(startMillis).atZone(zone))
        val e = DAY.format(Instant.ofEpochMilli(endMillis).atZone(zone))
        val stamp = exportedAtMillis?.let { "_exported_" + STAMP.format(Instant.ofEpochMilli(it).atZone(zone)) } ?: ""
        return "StatusBarTotal_${s}_to_$e$stamp.csv"
    }

    /** Quotes a CSV field when it contains a comma, quote or line break. */
    fun field(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun bucketName(key: String, cardNames: Map<String, String>): String = when (key) {
        "UPI" -> "UPI"
        "CC" -> "CC (no card)"
        "CCUPI" -> "CC UPI (no card)"
        else -> {
            val code = key.removePrefix("card:")
            val name = cardNames[code]
            if (name != null) "$name (${code.uppercase()})" else "${code.uppercase()} (deleted)"
        }
    }

    /**
     * UTF-8 CSV text: a header block (dates, total, budget if set, bucket totals), an empty line,
     * then one row per entry. [endMillis] is the end of the cycle (the reset time, or now).
     * Amounts are plain whole rupees.
     */
    fun csv(
        cycle: CycleInfo,
        endMillis: Long,
        budget: Long?,
        cardNames: Map<String, String>,
        recurringNames: Map<Long, String>,
        zone: ZoneId
    ): String {
        val sb = StringBuilder()
        fun day(ms: Long) = DAY.format(Instant.ofEpochMilli(ms).atZone(zone))
        sb.append("Cycle start,").append(cycle.startMillis?.let { day(it) } ?: "").append('\n')
        sb.append("Cycle end,").append(day(endMillis)).append('\n')
        sb.append("Total,").append(cycle.total).append('\n')
        if (budget != null) sb.append("Budget,").append(budget).append('\n')
        sb.append("Bucket,Amount\n")
        val keys = cycle.buckets.keys.sortedWith(
            compareBy({ when (it) { "UPI" -> 0; "CC" -> 1; "CCUPI" -> 2; else -> 3 } }, { it })
        )
        for (k in keys) {
            sb.append(field(bucketName(k, cardNames))).append(',').append(cycle.buckets.getValue(k)).append('\n')
        }
        sb.append('\n')
        sb.append("Date,Time,Amount,Category,Card code,Card name,Type\n")
        for (e in cycle.entries) {
            val at = Instant.ofEpochMilli(e.timeMillis).atZone(zone)
            val type = when {
                e.isUndoEntry -> "Undo"
                e.source != null && e.source.contains('@') -> {
                    val name = e.source.substringBefore('@').toLongOrNull()?.let { recurringNames[it] }
                    if (name != null) "Auto: $name" else "Auto"
                }
                else -> "Manual"
            }
            val cat = when (e.category) {
                Category.UPI -> "UPI"
                Category.CC -> "CC"
                Category.CCUPI -> "CC UPI"
            }
            sb.append(DAY.format(at)).append(',').append(TIME.format(at)).append(',')
                .append(e.amount).append(',').append(cat).append(',')
                .append(field(e.card?.uppercase() ?: "")).append(',')
                .append(field(e.card?.let { cardNames[it] ?: "" } ?: "")).append(',')
                .append(field(type)).append('\n')
        }
        return sb.toString()
    }
}
