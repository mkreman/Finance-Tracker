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
import com.moneytracker.app.data.local.database.entities.AccountType
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
    @Inject lateinit var budgetAlertManager: BudgetAlertManager

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
                val suggestedAccountId = intent.getStringExtra(BankAlertSuggestionNotifier.EXTRA_SUGGESTED_ACCOUNT_ID)
                
                val type = runCatching { TransactionType.valueOf(typeName ?: "") }.getOrNull()
                if (type == null || amount <= 0.0) {
                    Toast.makeText(context, "Unable to save suggestion", Toast.LENGTH_SHORT).show()
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        saveSuggestedTransaction(type, amount, payee, note, suggestedCatId, suggestedAccountId)
                        BankAlertSuggestionNotifier.cancel(context, suggestionId)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            // Prints the exact error reason now!
                            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
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
        suggestedCatId: String?,
        suggestedAccountId: String?
    ) {
        val accounts = accountRepository.getAllAccountsOnce()
        if (accounts.isEmpty()) throw IllegalStateException("No accounts exist")

        var finalAccountId = suggestedAccountId
        var finalToAccountId: String? = null
        
        val isSplitwise = note?.contains("Splitwise", ignoreCase = true) == true
        val myDefaultAccount = userPreferences.defaultAccountId.first() ?: accounts.first().id

        // Direct Save Account Auto-Creation and Routing for Splitwise
        if (isSplitwise) {
            var personAccountId: String? = null
            val matchingAccount = accounts.find { it.name.equals(payee, ignoreCase = true) && it.type == AccountType.PEOPLE }
            if (matchingAccount != null) {
                personAccountId = matchingAccount.id
            } else {
                val newAccountId = UUID.randomUUID().toString()
                val newAccount = com.moneytracker.app.domain.model.Account(
                    id = newAccountId,
                    name = payee,
                    type = AccountType.PEOPLE,
                    initialBalance = 0.0,
                    currentBalance = 0.0,
                    colorHex = "#4CAF50",
                    iconKey = "person"
                )
                accountRepository.saveAccount(newAccount)
                personAccountId = newAccountId
            }

            // Smart Routing
            if (type == TransactionType.TRANSFER) {
                // "You paid Kaku" -> Transfer FROM me TO Kaku
                finalAccountId = myDefaultAccount
                finalToAccountId = personAccountId
            } else {
                // "You owe Kaku" -> Expense sourced from Kaku's account
                finalAccountId = personAccountId
            }
        }

        // Standard Fallbacks if not caught by Splitwise router
        if (finalAccountId == null) {
            finalAccountId = categoryRecommendationRepository.getRecommendedAccountId(payee, type)
                ?: myDefaultAccount
        }
        
        // Final database failsafe: Transfers MUST have a destination
        if (type == TransactionType.TRANSFER && finalToAccountId == null) {
            finalToAccountId = accounts.firstOrNull { it.id != finalAccountId }?.id ?: myDefaultAccount
        }

        // Fetch categories safely (transfers don't inherently have categories)
        val categories = categoryRepository.getCategoriesByType(if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type).first()
        var selectedCategory: com.moneytracker.app.domain.model.Category? = null

        if (suggestedCatId != null) {
            selectedCategory = categories.firstOrNull { it.id == suggestedCatId }
        }
        if (selectedCategory == null) {
            selectedCategory = categories.firstOrNull { it.name.equals("AutoDetected", ignoreCase = true) }
                ?: categories.firstOrNull { it.name.equals("Other", ignoreCase = true) }
                ?: categories.firstOrNull()
        }

        if (selectedCategory != null) {
            categoryRecommendationRepository.upsertRecommendation(
                payee = payee,
                type = type,
                categoryId = selectedCategory.id,
                accountId = finalAccountId
            )
        }

        val transactionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val transaction = TransactionEntity(
            id = transactionId,
            accountId = finalAccountId,
            payee = payee,
            note = note,
            date = now,
            totalAmount = amount,
            type = type,
            toAccountId = finalToAccountId,
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

        val splits = if (selectedCategory != null && type != TransactionType.TRANSFER) {
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

        if (type == TransactionType.EXPENSE && selectedCategory != null) {
            budgetAlertManager.checkBudgets(now, mapOf(selectedCategory.id to selectedCategory.name))
        }
    }
}