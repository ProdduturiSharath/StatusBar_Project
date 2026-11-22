package com.sharath.statusbartotal.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sharath.statusbartotal.RecurringRunner
import com.sharath.statusbartotal.core.Category
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
import com.sharath.statusbartotal.notify.TotalNotifier
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

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

    /** Catch-up and notification refresh first, then reload every list. */
    fun reload() {
        TotalNotifier.refresh(context)
        canNotify = TotalNotifier.canNotify(context)
        snapshot = ledger.snapshot()
        history = ledger.history()
        cards = cardStore.read()
        recurring = recurringStore.read()
        settings = settingsStore.read()
        val applied = recurring.associate {
            it.id to RecurringRules.appliedMonths(it.id, snapshot.appliedSources)
        }
        upcoming = Upcoming.compute(recurring, applied, LocalDate.now())
    }

    fun appliedMonths(r: Recurring): Set<YearMonth> =
        RecurringRules.appliedMonths(r.id, snapshot.appliedSources)

    /** Returns the new entry's id, or null if saving failed. */
    fun add(amount: Long, category: Category, card: String?): Int? {
        val ok = ledger.addEntry(amount, System.currentTimeMillis(), category, card, null)
        if (!ok) return null
        reload()
        return history.lastOrNull()?.id
    }

    fun undo(id: Int): Boolean {
        val ok = ledger.undo(id, System.currentTimeMillis())
        reload()
        return ok
    }

    /** Anything owed lands in the old period first; returns the total to confirm. */
    fun prepareReset(): Long {
        RecurringRunner.catchUpAndSchedule(context)
        reload()
        return snapshot.total
    }

    fun reset(): Boolean {
        val ok = ledger.reset(System.currentTimeMillis())
        reload()
        return ok
    }

    fun addCard(code: String, name: String): Boolean {
        val ok = cardStore.add(code, name)
        reload()
        return ok
    }

    fun deleteCard(code: String): Boolean {
        val ok = cardStore.delete(code)
        reload()
        return ok
    }

    fun cardInUse(code: String): Boolean = recurringStore.read().any { it.card == code }

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
}
