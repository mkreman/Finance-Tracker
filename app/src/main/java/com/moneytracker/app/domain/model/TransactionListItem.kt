package com.moneytracker.app.domain.model

/**
 * Represents a group header + transactions under that date.
 */
sealed class TransactionListItem {
    data class Header(
        val dateLabel: String,
        val dateMillis: Long,
        val dayExpense: Double = 0.0,
        val dayIncome: Double = 0.0,
        val dayTransfer: Double = 0.0
    ) : TransactionListItem()
    data class Entry(val transaction: Transaction) : TransactionListItem()
}
