package com.moneytracker.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.CategoryRecommendationRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.util.BankAlertParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var accountRepo: AccountRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var categoryRecommendationRepo: CategoryRecommendationRepository
    @Inject lateinit var categoryRepo: CategoryRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullMessage = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val parsed = BankAlertParser.parse(fullMessage) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch smart recommendation passing both payee AND transaction type.
                val recommendation = categoryRecommendationRepo.getRecommendationEntity(parsed.payee, parsed.type)
                val catId = recommendation?.categoryId
                if (catId != null) {
                    val category = categoryRepo.getCategoryById(catId)
                    if (category != null) {
                        parsed.suggestedCategoryId = category.id
                        parsed.suggestedCategoryName = category.name
                    }
                }

                val accounts = accountRepo.getAllAccountsOnce()
                val defaultAccountId = userPreferences.defaultAccountId.first()
                val predictedAccountId = recommendation?.accountId
                    ?.takeIf { recId -> accounts.any { it.id == recId } }
                    ?: accounts.firstOrNull { it.id == defaultAccountId }?.id
                    ?: accounts.firstOrNull()?.id

                parsed.suggestedAccountId = predictedAccountId
                parsed.suggestedAccountName = accounts.firstOrNull { it.id == predictedAccountId }?.name
                BankAlertSuggestionNotifier.show(context, parsed)
            } catch (e: Exception) {
                Log.e("BankSmsReceiver", "Error processing SMS for suggestions", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
