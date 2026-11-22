package com.sharath.statusbartotal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sharath.statusbartotal.notify.TotalNotifier

class RecurringAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TotalNotifier.refresh(context)
    }
}
