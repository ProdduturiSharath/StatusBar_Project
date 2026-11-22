package com.sharath.statusbartotal.ui

import com.sharath.statusbartotal.core.RupeeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)

    fun date(d: LocalDate): String = DATE.format(d)

    fun date(millis: Long): String =
        DATE.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    fun dateTime(millis: Long): String = DATE_TIME
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
        .replace("AM", "am").replace("PM", "pm")

    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    fun bucketLabel(key: String, cardNames: Map<String, String>): String = when (key) {
        "UPI" -> "UPI"
        "CC" -> "CC (no card)"
        "CCUPI" -> "CC UPI (no card)"
        else -> {
            val code = key.removePrefix("card:")
            val name = cardNames[code]
            if (name != null) "$name (${code.uppercase()})" else "${code.uppercase()} (deleted)"
        }
    }

    fun rupees(v: Long): String = RupeeFormat.exact(v)
}
