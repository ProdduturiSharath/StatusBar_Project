package com.sharath.statusbartotal.notify

import android.content.Context
import com.sharath.statusbartotal.core.CycleInfo
import com.sharath.statusbartotal.core.Cycles
import com.sharath.statusbartotal.core.ReportBuilder
import com.sharath.statusbartotal.data.Cards
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.data.RecurringStore
import com.sharath.statusbartotal.data.ReportWriter
import com.sharath.statusbartotal.data.SettingsStore
import java.io.File
import java.time.ZoneId

/** Cycle reports in Downloads/StatusBarTotal. */
object Reports {
    private fun ledger(context: Context) = Ledger(File(context.filesDir, "ledger.txt"))

    /** Writes the report of [cycle]; shows the "saved" notification on success. */
    fun write(context: Context, cycle: CycleInfo, endMillis: Long, exportedAtMillis: Long?): Boolean {
        val zone = ZoneId.systemDefault()
        val cardNames = Cards(File(context.filesDir, "cards.txt")).read().associate { it.code to it.name }
        val recurringNames = RecurringStore(File(context.filesDir, "recurring.txt")).read()
            .associate { it.id to it.name }
        val budget = SettingsStore(File(context.filesDir, "settings.txt")).read().budget
        val name = ReportBuilder.fileName(cycle.startMillis ?: endMillis, endMillis, zone, exportedAtMillis)
        val text = ReportBuilder.csv(cycle, endMillis, budget, cardNames, recurringNames, zone)
        val ok = ReportWriter.write(context, name, text)
        if (ok) AlertNotifier.reportSaved(context)
        return ok
    }

    /** On-demand export of the current cycle. */
    fun export(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val cycle = Cycles.build(ledger(context).history()).last()
        return write(context, cycle, now, now)
    }

    /**
     * Called just before a reset line is appended: reports the cycle that is closing (once per
     * cycle, recorded with a note after success). A failure shows "Couldn't save the report".
     * Returns false only when saving was attempted and failed.
     */
    fun beforeReset(context: Context, endMillis: Long): Boolean {
        val ledger = ledger(context)
        val snapshot = ledger.snapshot()
        val key = "report@" + snapshot.cycleKey
        if (key in snapshot.notes) return true
        val cycle = Cycles.build(ledger.history()).last()
        if (cycle.entries.isEmpty()) return true
        val ok = write(context, cycle, endMillis, null)
        if (ok) ledger.addNote(System.currentTimeMillis(), key) else AlertNotifier.reportFailed(context)
        return ok
    }

    /**
     * On app open: retries the most recent closed auto-reset cycle that has entries but no
     * "report@" note. Silent on failure.
     */
    fun retryMissing(context: Context) {
        val ledger = ledger(context)
        val notes = ledger.snapshot().notes
        val cycle = Cycles.build(ledger.history())
            .lastOrNull { it.endReset?.auto == true && it.entries.isNotEmpty() } ?: return
        if ("report@${cycle.key}" in notes) return
        if (write(context, cycle, cycle.endReset!!.timeMillis, null)) {
            ledger.addNote(System.currentTimeMillis(), "report@${cycle.key}")
        }
    }
}
