package com.sharath.statusbartotal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sharath.statusbartotal.notify.TotalNotifier

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                TotalNotifier.refresh(context)
        }
    }
}
