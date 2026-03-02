package com.moneytracker.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.moneytracker.app.data.local.repository.CategoryRecommendationRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.util.BankAlertParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var categoryRecommendationRepo: CategoryRecommendationRepository
    @Inject lateinit var categoryRepo: CategoryRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Combine multipart messages
        val fullMessage = messages.joinToString(separator = "") { it.messageBody ?: "" }
        
        // Let the parser decide if this is a valid transaction message, completely
        // removing the overly-strict sender ID filter that was blocking real bank alerts.
        val parsed = BankAlertParser.parse(fullMessage) ?: return

        // Process the database lookup asynchronously so we don't block the main thread
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch smart recommendation
                val catId = categoryRecommendationRepo.getRecommendation(parsed.payee)
                if (catId != null) {
                    val category = categoryRepo.getCategoryById(catId)
                    if (category != null) {
                        parsed.suggestedCategoryId = category.id
                        parsed.suggestedCategoryName = category.name
                    }
                }
                
                // Show the notification with the suggested category (if found)
                BankAlertSuggestionNotifier.show(context, parsed)
                
            } catch (e: Exception) {
                Log.e("BankSmsReceiver", "Error processing SMS for suggestions", e)
            } finally {
                // Must call finish() to tell Android the receiver is done
                pendingResult.finish()
            }
        }
    }
}
