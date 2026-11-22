package com.sharath.statusbartotal.receivers

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sharath.statusbartotal.R
import com.sharath.statusbartotal.core.EntryParser
import com.sharath.statusbartotal.core.ParseResult
import com.sharath.statusbartotal.data.Cards
import com.sharath.statusbartotal.data.Ledger
import com.sharath.statusbartotal.notify.TotalNotifier
import java.io.File

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(TotalNotifier.REPLY_KEY)?.toString() ?: ""
        val cards = Cards(File(context.filesDir, "cards.txt")).read()
        val known = cards.map { it.code }.toSet()
        val ccUpi = cards.firstOrNull { it.ccUpiDefault }?.code
        val message: String? = when (val result = EntryParser.parseEntry(text, known, ccUpi)) {
            is ParseResult.BadAmount -> context.getString(R.string.error_invalid_entry)
            is ParseResult.NoCard -> context.getString(R.string.error_no_card, result.word)
            is ParseResult.NeedCard -> context.getString(R.string.error_need_card)
            is ParseResult.NoCcUpiCard -> context.getString(R.string.error_no_ccupi_card)
            is ParseResult.Ok -> {
                val ok = Ledger(File(context.filesDir, "ledger.txt")).addEntry(
                    result.amount, System.currentTimeMillis(), result.category, result.card, null
                )
                if (ok) null else context.getString(R.string.error_save_failed)
            }
        }
        TotalNotifier.refresh(context, message)
    }
}
