package com.moneytracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
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
    const val EXTRA_SUGGESTED_CAT_ID = "extra_suggested_cat_id"
    const val EXTRA_SUGGESTED_ACCOUNT_ID = "extra_suggested_account_id"

    private const val CHANNEL_ID = "bank_alert_suggestions_v2"

    fun show(context: Context, suggestion: ParsedBankAlert) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ensureChannel(manager)

        val notificationId = suggestion.suggestionId.hashCode()

        // 1. Edit (Opens App)
        val editIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_type", suggestion.type.name)
            putExtra("suggestion_amount", suggestion.amount.toString())
            putExtra("suggestion_note", suggestion.note)
            putExtra("suggestion_payee", suggestion.payee)
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
            putExtra("suggested_cat_id", suggestion.suggestedCategoryId)
            putExtra("suggested_account_id", suggestion.suggestedAccountId)
        }
        val editPendingIntent = PendingIntent.getActivity(
            context, notificationId, editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Direct Quick Save
        val saveIntent = Intent(context, TransactionSuggestionActionReceiver::class.java).apply {
            action = ACTION_SAVE_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
            putExtra(EXTRA_TYPE, suggestion.type.name)
            putExtra(EXTRA_AMOUNT, suggestion.amount)
            putExtra(EXTRA_PAYEE, suggestion.payee)
            putExtra(EXTRA_NOTE, suggestion.note)
            putExtra(EXTRA_SUGGESTED_CAT_ID, suggestion.suggestedCategoryId)
            putExtra(EXTRA_SUGGESTED_ACCOUNT_ID, suggestion.suggestedAccountId)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Discard
        val discardIntent = Intent(context, TransactionSuggestionActionReceiver::class.java).apply {
            action = ACTION_DISCARD_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, suggestion.suggestionId)
        }
        val discardPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2, discardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- FORMATTING THE CONTENT ---
        
        val title = when (suggestion.type) {
            TransactionType.EXPENSE -> "Expense Detected"
            TransactionType.INCOME -> "Income Detected"
            TransactionType.TRANSFER -> "Transfer Detected"
        }
        
        val amountText = "₹${"%.2f".format(suggestion.amount)}"
        val amountColor = when (suggestion.type) {
            TransactionType.EXPENSE -> Color.parseColor("#D32F2F")
            TransactionType.INCOME -> Color.parseColor("#2E7D32")
            TransactionType.TRANSFER -> Color.parseColor("#2196F3")
        }

        val directionPrefix = if (suggestion.type == TransactionType.INCOME) "From" else "To"
        val categoryName = suggestion.suggestedCategoryName ?: "AutoDetected"
        val accountName = suggestion.suggestedAccountName

        // Short text (When notification is collapsed) - Category in 2nd place, No Note
        val accountPart = accountName?.let { " | Acc: $it" } ?: ""
        val shortText = "$amountText | Cat: $categoryName$accountPart | $directionPrefix: ${suggestion.payee}"
        val contentText = SpannableString(shortText).apply {
            val amountStart = shortText.indexOf(amountText)
            if (amountStart >= 0) {
                setSpan(ForegroundColorSpan(amountColor), amountStart, amountStart + amountText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Expanded text (When user pulls down on the notification) - Category in 2nd place, No Note
        val expandedText = SpannableStringBuilder().apply {
            append("Amount: ")
            val amountStart = length
            append(amountText)
            setSpan(ForegroundColorSpan(amountColor), amountStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append("\n")
            
            append("Category: $categoryName\n")
            accountName?.let {
                append("Account: $it\n")
            }
            append("$directionPrefix: ${suggestion.payee}")
        }

        // --- COLORED BUTTONS ---
        
        val saveActionText = SpannableString("Save").apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#2E7D32")), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // Green
        }
        val editActionText = SpannableString("Edit").apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#1976D2")), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // Blue
        }
        val discardActionText = SpannableString("Discard").apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // Red
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .setContentIntent(editPendingIntent) // Tapping the body acts like "Edit"
            .addAction(R.drawable.ic_notification, saveActionText, savePendingIntent)
            .addAction(R.drawable.ic_notification, editActionText, editPendingIntent)
            .addAction(R.drawable.ic_notification, discardActionText, discardPendingIntent)
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
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bank Alert Suggestions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Suggestions from detected bank debit/credit messages"
                setSound(
                    defaultSoundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }
    }
}
