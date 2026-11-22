package com.sharath.statusbartotal.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sharath.statusbartotal.RecurringRunner
import com.sharath.statusbartotal.core.Category
import com.sharath.statusbartotal.core.CycleInfo
import com.sharath.statusbartotal.core.Cycles
import com.sharath.statusbartotal.core.Recurring
import com.sharath.statusbartotal.core.RecurringRules
import com.sharath.statusbartotal.core.Upcoming
import com.sharath.statusbartotal.core.UpcomingDue
import com.sharath.statusbartotal.data.AppSettings
import com.sharath.statusbartotal.data.Card
import com.sharath.statusbartotal.data.Cards
import com.sharath.statusbartotal.data.HistoryItem
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.data.RecurringStore
import com.sharath.statusbartotal.data.SettingsStore
import com.sharath.statusbartotal.notify.Reports
import com.sharath.statusbartotal.notify.TotalNotifier
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Result of a manual reset: [ok] is the reset itself, [reportFailed] a report that could not be saved. */
class ResetResult(val ok: Boolean, val reportFailed: Boolean)

/** Plain holder of everything the screens show. Every action refreshes the notification and reloads. */
class AppState(private val context: Context) {
    private val ledger get() = Ledger(File(context.filesDir, "ledger.txt"))
    private val cardStore get() = Cards(File(context.filesDir, "cards.txt"))
    private val recurringStore get() = RecurringStore(File(context.filesDir, "recurring.txt"))
    private val settingsStore get() = SettingsStore(File(context.filesDir, "settings.txt"))

    var snapshot by mutableStateOf(Ledger.Snapshot(0L, null, emptyMap(), emptySet()))
        private set
    var history by mutableStateOf<List<HistoryItem>>(emptyList())
        private set
    var cycles by mutableStateOf<List<CycleInfo>>(emptyList())
        private set
    var cards by mutableStateOf<List<Card>>(emptyList())
        private set
    var recurring by mutableStateOf<List<Recurring>>(emptyList())
        private set
    var settings by mutableStateOf(AppSettings(null, null))
        private set
    var upcoming by mutableStateOf<List<UpcomingDue>>(emptyList())
        private set
    var canNotify by mutableStateOf(true)
        private set

    val cardNames: Map<String, String> get() = cards.associate { it.code to it.name }

    val ccUpiDefault: Card? get() = cards.firstOrNull { it.ccUpiDefault }

    /** The card of the most recent CC entry whose card still exists. */
    val lastCcCard: String?
        get() {
            val codes = cards.map { it.code }.toSet()
            return history.lastOrNull {
                it is HistoryItem.Entry && it.category == Category.CC && !it.isUndoEntry &&
                    it.card != null && it.card in codes
            }.let { (it as? HistoryItem.Entry)?.card }
        }

    /** Catch-up and notification refresh first, then reload every list. */
    fun reload() {
        TotalNotifier.refresh(context)
        canNotify = TotalNotifier.canNotify(context)
        snapshot = ledger.snapshot()
        history = ledger.history()
        cycles = Cycles.build(history)
        cards = cardStore.read()
        recurring = recurringStore.read()
        settings = settingsStore.read()
        val applied = recurring.associate {
            it.id to RecurringRules.appliedMonths(it.id, snapshot.appliedSources)
        }
        upcoming = Upcoming.compute(recurring, applied, LocalDate.now())
    }

    /** App open: normal reload, then retry a report that failed to save earlier. */
    fun onOpen() {
        reload()
        Reports.retryMissing(context)
    }

    fun appliedMonths(r: Recurring): Set<YearMonth> =
        RecurringRules.appliedMonths(r.id, snapshot.appliedSources)

    /** Returns the new entry's id, or null if saving failed. */
    fun add(amount: Long, category: Category, card: String?): Int? {
        val ok = ledger.addEntry(amount, System.currentTimeMillis(), category, card, null)
        if (!ok) return null
        reload()
        return history.lastOrNull { it is HistoryItem.Entry }?.id
    }

    fun undo(id: Int): Boolean {
        val ok = ledger.undo(id, System.currentTimeMillis())
        reload()
        return ok
    }

    /**
     * The current total before and after a tombstone over [fromId]..[toId]: the same snapshot
     * function run with and without the proposed tombstone.
     */
    fun previewDelete(fromId: Int, toId: Int): Pair<Long, Long> {
        val l = ledger
        return l.snapshot().total to l.snapshot(fromId..toId).total
    }

    fun deleteRange(fromId: Int, toId: Int): Boolean {
        val ok = ledger.deleteRange(System.currentTimeMillis(), fromId, toId)
        reload()
        return ok
    }

    /** Anything owed lands in the old period first; returns the total to confirm. */
    fun prepareReset(): Long {
        RecurringRunner.catchUpAndSchedule(context)
        reload()
        return snapshot.total
    }

    fun reset(saveReport: Boolean): ResetResult {
        val now = System.currentTimeMillis()
        val reportOk = if (saveReport) Reports.beforeReset(context, now) else true
        val ok = ledger.reset(now)
        reload()
        return ResetResult(ok, !reportOk)
    }

    /** Exports the current cycle to Downloads/StatusBarTotal. */
    fun exportReport(): Boolean = Reports.export(context)

    fun addCard(code: String, name: String): Boolean {
        val ok = cardStore.add(code, name)
        reload()
        return ok
    }

    fun updateCard(
        code: String, newCode: String, name: String, dueDay: Int?, ccUpiDefault: Boolean
    ): Boolean {
        // Renaming a code: move old entries and recurring payments first, then the card itself.
        if (newCode != code) {
            if (!Cards.isValidCode(newCode) || cardStore.read().any { it.code == newCode }) return false
            if (!Ledger(File(context.filesDir, "ledger.txt")).renameCard(code, newCode)) return false
            if (!RecurringStore(File(context.filesDir, "recurring.txt")).renameCard(code, newCode)) return false
        }
        val ok = cardStore.update(code, newCode, name, dueDay, ccUpiDefault)
        reload()
        return ok
    }

    fun deleteCard(code: String): Boolean {
        val ok = cardStore.delete(code)
        reload()
        return ok
    }

    /** Recurring payments that use the card. */
    fun recurringUsing(code: String): List<Recurring> = recurringStore.read().filter { it.card == code }

    fun addRecurring(r: Recurring): Boolean {
        val ok = recurringStore.add(r)
        reload()
        return ok
    }

    fun deleteRecurring(id: Long): Boolean {
        val ok = recurringStore.delete(id)
        reload()
        return ok
    }

    fun setAutoResetDay(day: Int?): Boolean {
        val ok = settingsStore.setAutoResetDay(day, System.currentTimeMillis())
        reload()
        return ok
    }

    fun setBudget(budget: Long?): Boolean {
        val ok = settingsStore.setBudget(budget)
        reload()
        return ok
    }

    fun setShowLeft(showLeft: Boolean): Boolean {
        val ok = settingsStore.setShowLeft(showLeft)
        reload()
        return ok
    }
}
