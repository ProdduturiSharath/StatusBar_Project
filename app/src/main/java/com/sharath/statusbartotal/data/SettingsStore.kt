package com.sharath.statusbartotal.data

import java.io.File

data class AppSettings(val autoResetDay: Int?, val autoResetSince: Long?)

/** settings.txt: "autoResetDay=<n or empty>" and "autoResetSince=<millis or empty>". */
class SettingsStore(private val file: File) {

    fun read(): AppSettings {
        var day: Int? = null
        var since: Long? = null
        for (line in AtomicWrite.readLines(file)) {
            val i = line.indexOf('=')
            if (i < 0) continue
            val v = line.substring(i + 1).trim()
            when (line.substring(0, i)) {
                "autoResetDay" -> day = v.toIntOrNull()?.takeIf { it in 1..31 }
                "autoResetSince" -> since = v.toLongOrNull()
            }
        }
        if (day == null || since == null) return AppSettings(null, null)
        return AppSettings(day, since)
    }

    /** Turning on (or changing the day) saves [nowMillis] as the "enabled since" time. */
    fun setAutoResetDay(day: Int?, nowMillis: Long): Boolean {
        val d = day?.takeIf { it in 1..31 }
        val current = read()
        val since = when {
            d == null -> null
            current.autoResetDay == d && current.autoResetSince != null -> current.autoResetSince
            else -> nowMillis
        }
        return AtomicWrite.write(
            file, "autoResetDay=${d ?: ""}\nautoResetSince=${since ?: ""}\n"
        )
    }
}
