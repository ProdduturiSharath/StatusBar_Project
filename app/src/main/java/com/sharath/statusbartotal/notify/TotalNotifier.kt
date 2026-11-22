package com.sharath.statusbartotal.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import com.sharath.statusbartotal.MainActivity
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.RecurringRunner
import com.sharath.statusbartotal.core.Breakdown
import com.sharath.statusbartotal.core.Budget
import com.sharath.statusbartotal.data.SettingsStore
import com.sharath.statusbartotal.data.Cards
import com.sharath.statusbartotal.core.IconLabel
import com.sharath.statusbartotal.core.RupeeFormat
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.receivers.DismissReceiver
import com.sharath.statusbartotal.receivers.ReplyReceiver
import java.io.File

object TotalNotifier {
    const val CHANNEL_ID = "running_total"
    const val NOTIFICATION_ID = 1
    const val REPLY_KEY = "amount"
    private const val REQ_TAP = 0
    private const val REQ_REPLY = 1
    private const val REQ_DISMISS = 2

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private fun ensureChannel(context: Context) {
        val nm = manager(context)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        ensureChannel(context)
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val nm = manager(context)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(CHANNEL_ID) ?: return false
        return channel.importance >= NotificationManager.IMPORTANCE_LOW
    }

    fun refresh(context: Context, message: String? = null) {
        RecurringRunner.catchUpAndSchedule(context)
        AlertNotifier.checkBudget(context)
        if (!canNotify(context)) return
        val snapshot = Ledger(File(context.filesDir, "ledger.txt")).snapshot()
        val cardNames = Cards(File(context.filesDir, "cards.txt")).read().associate { it.code to it.name }
        val breakdown = Breakdown.lines(snapshot.buckets, cardNames, snapshot.cardCcUpi)

        val settings = SettingsStore(File(context.filesDir, "settings.txt")).read()
        val budget = settings.budget
        // Status bar number: total spent, or budget left when chosen and a budget is set.
        val iconValue = if (settings.showLeft && budget != null) budget - snapshot.total else snapshot.total
        val icon = Icon.createWithBitmap(IconRenderer.render(IconLabel.lines(iconValue)))
        val lastLine = snapshot.lastEntry?.let {
            context.getString(R.string.notif_last_entry, RupeeFormat.signed(it))
        } ?: context.getString(R.string.notif_no_entries)
        // With a budget, its status is the first line and the last entry follows.
        val lines = when {
            message != null -> listOf(message)
            budget != null -> listOf(Budget.notificationLine(snapshot.total, budget), lastLine)
            else -> listOf(lastLine)
        }
        val text = lines.first()

        val tapIntent = PendingIntent.getActivity(
            context, REQ_TAP, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val replyIntent = PendingIntent.getBroadcast(
            context, REQ_REPLY, Intent(context, ReplyReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context, REQ_DISMISS, Intent(context, DismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remoteInput = RemoteInput.Builder(REPLY_KEY)
            .setLabel(context.getString(R.string.notif_reply_hint))
            .build()
        val action = Notification.Action.Builder(
            null as Icon?, context.getString(R.string.notif_action_add), replyIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(
                context.getString(R.string.notif_title, RupeeFormat.exact(snapshot.total))
            )
            .setContentText(text)
            .setStyle(
                Notification.BigTextStyle().bigText((lines + breakdown).joinToString("\n"))
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(tapIntent)
            .setDeleteIntent(dismissIntent)
            .addAction(action)
            .build()
        manager(context).notify(NOTIFICATION_ID, notification)
    }
}
