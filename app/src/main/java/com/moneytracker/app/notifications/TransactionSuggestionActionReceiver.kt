package com.moneytracker.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.CategoryRecommendationRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TransactionSuggestionActionReceiver : BroadcastReceiver() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var categoryRecommendationRepository: CategoryRecommendationRepository
    @Inject lateinit var userPreferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val suggestionId = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_SUGGESTION_ID)

        when (intent.action) {
            BankAlertSuggestionNotifier.ACTION_DISCARD_SUGGESTION -> {
                BankAlertSuggestionNotifier.cancel(context, suggestionId)
            }

            BankAlertSuggestionNotifier.ACTION_SAVE_SUGGESTION -> {
                val typeName = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_TYPE)
                val amount = intent.getDoubleExtra(BankAlertSuggestionNotifier.EXTRA_AMOUNT, 0.0)
                val payee = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_PAYEE).orEmpty().ifBlank { "Transaction" }
                val note = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_NOTE)
                val suggestedCatId = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_SUGGESTED_CAT_ID)
                
                val type = runCatching { TransactionType.valueOf(typeName ?: "") }.getOrNull()
                if (type == null || amount <= 0.0) {
                    Toast.makeText(context, "Unable to save suggestion", Toast.LENGTH_SHORT).show()
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        saveSuggestedTransaction(type, amount, payee, note, suggestedCatId)
                        BankAlertSuggestionNotifier.cancel(context, suggestionId)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Failed to save suggestion", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun saveSuggestedTransaction(
        type: TransactionType,
        amount: Double,
        payee: String,
        note: String?,
        suggestedCatId: String?
    ) {
        val accounts = accountRepository.getAllAccountsOnce()
        if (accounts.isEmpty()) return

        val defaultAccountId = userPreferences.defaultAccountId.first()
        val selectedAccount = accounts.find { it.id == defaultAccountId } ?: accounts.first()

        val categories = categoryRepository.getCategoriesByType(type).first()
        var selectedCategory: com.moneytracker.app.domain.model.Category? = null

        // 1. Try the smart recommendation ID if one was provided
        if (suggestedCatId != null) {
            selectedCategory = categories.firstOrNull { it.id == suggestedCatId }
        }

        // 2. Fallbacks
        if (selectedCategory == null) {
            selectedCategory = categories.firstOrNull { it.name.equals("AutoDetected", ignoreCase = true) }
                ?: categories.firstOrNull { it.name.equals("Other", ignoreCase = true) }
                ?: categories.firstOrNull()
        }

        // 3. Update the recommendation engine!
        if (selectedCategory != null) {
            categoryRecommendationRepository.upsertRecommendation(payee, type, selectedCategory.id)
        }

        val transactionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val transaction = TransactionEntity(
            id = transactionId,
            accountId = selectedAccount.id,
            payee = payee,
            note = note,
            date = now,
            totalAmount = amount,
            type = type,
            toAccountId = null,
            createdAt = now,
            modifiedAt = now,
            syncStatus = SyncStatus.DIRTY,
            isRecurring = false,
            recurringInterval = null,
            recurringUnit = null,
            recurringEndDate = null,
            parentRecurringId = null,
            notifyForRecurringEntries = true
        )

        val splits = if (selectedCategory != null) {
            listOf(
                TransactionSplitEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    categoryId = selectedCategory.id,
                    amount = amount,
                    note = null
                )
            )
        } else {
            emptyList()
        }

        transactionRepository.saveTransaction(transaction, splits)
    }
}
