package com.moneytracker.app.ui.screens.transactions

import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.database.entities.RecurringUnit
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.domain.model.Category

data class AddTransactionState(
    val amount: String = "",
    val note: String = "",
    val payee: String = "", // Used to store the payee for learning algorithms
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: String? = null,
    val toAccountId: String? = null,
    val isSplitMode: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),
    val splits: List<SplitState> = listOf(SplitState()),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isEditMode: Boolean = false,
    val editTransactionId: String? = null,
    val parentRecurringId: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringInterval: String = "1",
    val recurringUnit: RecurringUnit = RecurringUnit.MONTH,
    val recurringEndDate: Long? = null,
    val notifyForRecurringEntries: Boolean = true,
    val receiptUris: List<String> = emptyList()
) {
    val totalAmount: Double
        get() = amount.toDoubleOrNull() ?: 0.0

    val splitsTotal: Double
        get() = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

    val remaining: Double
        get() = totalAmount - splitsTotal

    val isValid: Boolean
        get() {
            if (totalAmount <= 0) return false
            if (selectedAccountId == null) return false
            if (type == TransactionType.TRANSFER) {
                return toAccountId != null && toAccountId != selectedAccountId
            }
            if (isSplitMode) {
                return splits.all { it.categoryId != null && (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                        && kotlin.math.abs(remaining) < 0.01
            }
            return selectedCategoryIds.isNotEmpty() &&
                    splits.any { it.categoryId != null && (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
        }
}

data class SplitState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val categoryId: String? = null,
    val categoryName: String = "",
    val amount: String = ""
)
