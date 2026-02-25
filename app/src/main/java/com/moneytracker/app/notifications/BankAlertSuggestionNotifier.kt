package com.moneytracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.util.ParsedBankAlert

object BankAlertSuggestionNotifier {

    const val ACTION_SAVE_SUGGESTION = "com.moneytracker.app.action.SAVE_SUGGESTION"
    const val ACTION_DISCARD_SUGGESTION = "com.moneytracker.app.action.DISCARD_SUGGESTION"

    const val EXTRA_SUGGESTION_ID = "extra_suggestion_id"
    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_PAYEE = "extra_payee"
    const val EXTRA_NOTE = "extra_note"

    private const val CHANNEL_ID = "bank_alert_suggestions"

    fun show(context: Context, suggestion: ParsedBankAlert) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val notificationId = suggestion.suggestionId.hashCode()

        val editIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_type", suggestion.type.name)
            putExtra("suggestion_amount", suggestion.amount.toString())
            putExtra("suggestion_note", suggestion.note)
            putExtra("suggestion_payee", suggestion.payee)
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(context, TransactionSuggestionActionReceiver::class.java).apply {
            action = ACTION_SAVE_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
            putExtra(EXTRA_TYPE, suggestion.type.name)
            putExtra(EXTRA_AMOUNT, suggestion.amount)
            putExtra(EXTRA_PAYEE, suggestion.payee)
            putExtra(EXTRA_NOTE, suggestion.note)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val discardIntent = Intent(context, TransactionSuggestionActionReceiver::class.java).apply {
            action = ACTION_DISCARD_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
        }
        val discardPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            discardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (suggestion.type.name == "EXPENSE") "Suggested Expense" else "Suggested Income"
        val amountText = "₹${"%.2f".format(suggestion.amount)}"
        val amountColor = when (suggestion.type) {
            TransactionType.EXPENSE -> Color.parseColor("#D32F2F")
            TransactionType.INCOME -> Color.parseColor("#2E7D32")
            TransactionType.TRANSFER -> Color.parseColor("#2196F3")
        }
        val content = "${suggestion.payee}: $amountText"
        val contentText = SpannableString(content).apply {
            val amountStart = content.length - amountText.length
            setSpan(
                ForegroundColorSpan(amountColor),
                amountStart,
                content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val expandedText = SpannableStringBuilder("$content\n${suggestion.note}").apply {
            val amountStart = indexOf(amountText)
            if (amountStart >= 0) {
                setSpan(
                    ForegroundColorSpan(amountColor),
                    amountStart,
                    amountStart + amountText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(editPendingIntent)
            .addAction(R.drawable.ic_notification, "Save", savePendingIntent)
            .addAction(R.drawable.ic_notification, "Edit", editPendingIntent)
            .addAction(R.drawable.ic_notification, "Discard", discardPendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    fun cancel(context: Context, suggestionId: String?) {
        if (suggestionId.isNullOrBlank()) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(suggestionId.hashCode())
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bank Alert Suggestions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Suggestions from detected bank debit/credit messages"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
