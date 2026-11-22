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
        val known = Cards(File(context.filesDir, "cards.txt")).read().map { it.code }.toSet()
        val message: String? = when (val result = EntryParser.parseEntry(text, known)) {
            is ParseResult.BadAmount -> context.getString(R.string.error_invalid_entry)
            is ParseResult.NoCard -> context.getString(R.string.error_no_card, result.word)
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
