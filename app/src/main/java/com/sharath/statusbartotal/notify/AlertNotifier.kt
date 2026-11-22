package com.sharath.statusbartotal.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sharath.statusbartotal.MainActivity
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.core.Budget
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.data.ReportWriter
import com.sharath.statusbartotal.data.SettingsStore
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Budget, card reminder and report notifications on the "alerts" channel. */
object AlertNotifier {
    const val CHANNEL_ID = "alerts"
    const val BUDGET_ID = 2
    const val REPORT_SAVED_ID = 3
    const val REPORT_FAILED_ID = 4
    private const val REQ_TAP = 20
    private val DUE_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private fun ensureChannel(context: Context) {
        val nm = manager(context)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.alerts_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /** Permission granted, notifications enabled and the alerts channel not blocked. */
    fun canShow(context: Context): Boolean {
        ensureChannel(context)
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val nm = manager(context)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(CHANNEL_ID) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun show(context: Context, id: Int, title: String) {
        val tap = PendingIntent.getActivity(
            context, REQ_TAP, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        manager(context).notify(id, n)
    }

    fun reminder(context: Context, code: String, name: String?, due: LocalDate, amount: Long) {
        val label = (name ?: code.uppercase()) + " (" + code.uppercase() + ")"
        val title = "$label bill due ${DUE_DATE.format(due)} · ${RupeeFormat.exact(amount)} this cycle"
        show(context, 1000 + Math.abs(code.hashCode()) % 1000, title)
    }

    fun reportSaved(context: Context) {
        if (canShow(context)) show(context, REPORT_SAVED_ID, "Report saved to ${ReportWriter.SHOWN_FOLDER}")
    }

    fun reportFailed(context: Context) {
        if (canShow(context)) show(context, REPORT_FAILED_ID, "Couldn't save the report")
    }

    /**
     * One alert per cycle and budget amount when the total is over the budget. The note is only
     * written if the notification can actually be shown.
     */
    fun checkBudget(context: Context) {
        val budget = SettingsStore(File(context.filesDir, "settings.txt")).read().budget ?: return
        val ledger = Ledger(File(context.filesDir, "ledger.txt"))
        val s = ledger.snapshot()
        if (!Budget.isOver(s.total, budget)) return
        val key = Budget.alertKey(s.cycleKey, budget)
        if (key in s.notes) return
        if (!canShow(context)) return
        show(context, BUDGET_ID, Budget.alertText(s.total, budget))
        ledger.addNote(System.currentTimeMillis(), key)
    }
}
