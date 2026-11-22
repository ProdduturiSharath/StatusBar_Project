package com.sharath.statusbartotal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sharath.statusbartotal.core.DueEvent
import com.sharath.statusbartotal.core.DuePlanner
import com.sharath.statusbartotal.core.RecurringRules
import com.sharath.statusbartotal.core.Reminders
import com.sharath.statusbartotal.data.Cards
import com.sharath.statusbartotal.notify.AlertNotifier
import com.sharath.statusbartotal.notify.Reports
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.data.RecurringStore
import com.sharath.statusbartotal.data.SettingsStore
import com.sharath.statusbartotal.receivers.RecurringAlarmReceiver
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

object RecurringRunner {
    private const val REQ_ALARM = 10
    private const val RETRY_MILLIS = 15 * 60 * 1000L

    /** Adds every missing recurring month and auto-reset in due order, then sets or cancels the alarm. */
    @Synchronized
    fun catchUpAndSchedule(context: Context) {
        val ledger = Ledger(File(context.filesDir, "ledger.txt"))
        val items = RecurringStore(File(context.filesDir, "recurring.txt")).read()
        val settings = SettingsStore(File(context.filesDir, "settings.txt")).read()
        val snapshot = ledger.snapshot()
        val applied = HashMap<Long, MutableSet<YearMonth>>()
        for (r in items) {
            applied[r.id] = RecurringRules.appliedMonths(r.id, snapshot.appliedSources).toMutableSet()
        }

        val zone = ZoneId.systemDefault()
        val nowMillis = System.currentTimeMillis()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val since = settings.autoResetSince?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone)
        }
        val cards = Cards(File(context.filesDir, "cards.txt")).read()
        val dueDays = HashMap<String, Int>()
        for (c in cards) c.dueDay?.let { dueDays[c.code] = it }
        val notes = snapshot.notes.toMutableSet()
        val events = DuePlanner.plan(
            items, applied, settings.autoResetDay, since, snapshot.autoResetMonths, now, dueDays, notes
        )
        var failed = false
        for (e in events) {
            val ok = when (e) {
                is DueEvent.RecurringDue -> ledger.addEntry(
                    e.item.amount, nowMillis, e.item.category, e.item.card,
                    "${e.item.id}@${e.month}"
                )
                is DueEvent.AutoResetDue -> {
                    val atMillis = e.due.atZone(zone).toInstant().toEpochMilli()
                    // The report of the closing cycle is written first; a failure never blocks the reset.
                    Reports.beforeReset(context, atMillis)
                    ledger.reset(atMillis, e.month)
                }
                is DueEvent.CardReminderDue -> {
                    val rem = e.reminder
                    if (AlertNotifier.canShow(context)) {
                        val amount = ledger.snapshot().buckets["card:" + rem.code] ?: 0L
                        val name = cards.firstOrNull { it.code == rem.code }?.name
                        AlertNotifier.reminder(context, rem.code, name, rem.dueDate, amount)
                        if (ledger.addNote(nowMillis, Reminders.key(rem.code, rem.dueMonth))) {
                            notes.add(Reminders.key(rem.code, rem.dueMonth))
                        }
                    }
                    true
                }
            }
            if (!ok) {
                failed = true
                break
            }
            if (e is DueEvent.RecurringDue) applied.getValue(e.item.id).add(e.month)
        }

        val earliest = DuePlanner.nextWake(items, applied, settings.autoResetDay, now, dueDays, notes)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_ALARM, Intent(context, RecurringAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (earliest == null && !failed) {
            alarmManager.cancel(pending)
            return
        }
        var at = earliest?.atZone(zone)?.toInstant()?.toEpochMilli() ?: 0L
        if (failed || at <= nowMillis) at = nowMillis + RETRY_MILLIS
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }
}
