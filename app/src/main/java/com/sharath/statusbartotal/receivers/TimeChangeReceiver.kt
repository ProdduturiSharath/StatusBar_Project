package com.sharath.statusbartotal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sharath.statusbartotal.notify.TotalNotifier

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                TotalNotifier.refresh(context)
        }
    }
}
