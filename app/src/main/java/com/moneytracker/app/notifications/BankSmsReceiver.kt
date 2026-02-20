package com.moneytracker.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.moneytracker.app.util.BankAlertParser

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullMessage = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val parsed = BankAlertParser.parse(fullMessage) ?: return
        BankAlertSuggestionNotifier.show(context, parsed)
    }
}
