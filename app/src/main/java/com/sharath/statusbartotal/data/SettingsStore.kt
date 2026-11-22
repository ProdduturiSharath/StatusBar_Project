package com.sharath.statusbartotal.data

import java.io.File

data class AppSettings(
    val autoResetDay: Int?,
    val autoResetSince: Long?,
    /** Monthly budget in whole rupees, or null when off. */
    val budget: Long? = null,
    /** Status bar number: true = budget left (only used when a budget is set), false = total spent. */
    val showLeft: Boolean = false
)

/**
 * settings.txt: "autoResetDay=<n or empty>", "autoResetSince=<millis or empty>",
 * "budget=<rupees or empty>" and "statusBar=<total|left>" (missing = total).
 */
class SettingsStore(private val file: File) {

    fun read(): AppSettings {
        var day: Int? = null
        var since: Long? = null
        var budget: Long? = null
        var showLeft = false
        for (line in AtomicWrite.readLines(file)) {
            val i = line.indexOf('=')
            if (i < 0) continue
            val v = line.substring(i + 1).trim()
            when (line.substring(0, i)) {
                "autoResetDay" -> day = v.toIntOrNull()?.takeIf { it in 1..31 }
                "autoResetSince" -> since = v.toLongOrNull()
                "budget" -> budget = v.toLongOrNull()?.takeIf { it > 0 }
                "statusBar" -> showLeft = v == "left"
            }
        }
        if (day == null || since == null) return AppSettings(null, null, budget, showLeft)
        return AppSettings(day, since, budget, showLeft)
    }

    private fun write(day: Int?, since: Long?, budget: Long?, showLeft: Boolean): Boolean =
        AtomicWrite.write(
            file,
            "autoResetDay=${day ?: ""}\nautoResetSince=${since ?: ""}\nbudget=${budget ?: ""}\n" +
                "statusBar=${if (showLeft) "left" else "total"}\n"
        )

    /** Turning on (or changing the day) saves [nowMillis] as the "enabled since" time. */
    fun setAutoResetDay(day: Int?, nowMillis: Long): Boolean {
        val d = day?.takeIf { it in 1..31 }
        val current = read()
        val since = when {
            d == null -> null
            current.autoResetDay == d && current.autoResetSince != null -> current.autoResetSince
            else -> nowMillis
        }
        return write(d, since, current.budget, current.showLeft)
    }

    /** [budget] in whole rupees (greater than 0), or null to turn the budget off. */
    fun setBudget(budget: Long?): Boolean {
        val b = budget?.takeIf { it > 0 }
        val current = read()
        return write(current.autoResetDay, current.autoResetSince, b, current.showLeft)
    }

    /** Status bar shows budget left (true) or total spent (false). */
    fun setShowLeft(showLeft: Boolean): Boolean {
        val current = read()
        return write(current.autoResetDay, current.autoResetSince, current.budget, showLeft)
    }
}
