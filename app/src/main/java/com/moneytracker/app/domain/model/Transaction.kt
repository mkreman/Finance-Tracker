package com.moneytracker.app.domain.model

import com.moneytracker.app.data.local.database.entities.RecurringUnit
import com.moneytracker.app.data.local.database.entities.TransactionType

data class Transaction(
    val id: String,
    val accountId: String,
    val accountName: String,
    val payee: String,
    val note: String?,
    val date: Long,
    val totalAmount: Double,
    val type: TransactionType,
    val isRecurring: Boolean = false,
    val recurringInterval: Int? = null,
    val recurringUnit: RecurringUnit? = null,
    val recurringEndDate: Long? = null,
    val parentRecurringId: String? = null,
    val notifyForRecurringEntries: Boolean = true,
    val receiptUri: String? = null,
    val toAccountId: String? = null,
    val toAccountName: String? = null,
    val splits: List<TransactionSplit>
) {
    val isInvestment: Boolean
        get() = splits.any {
            it.categoryId.startsWith("cat-investment") || it.categoryName.equals("Investment", ignoreCase = true)
        } || (type == TransactionType.TRANSFER && (note?.contains("Bought") == true || note?.contains("Sold") == true))
}

data class TransactionSplit(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val amount: Double,
    val note: String? = null
)
