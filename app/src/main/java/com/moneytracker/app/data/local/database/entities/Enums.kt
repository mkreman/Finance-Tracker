package com.moneytracker.app.data.local.database.entities

/**
 * Enums used across the data layer.
 */

enum class AccountType {
    CASH,
    BANK,
    INVESTMENT,
    WALLET,
    PEOPLE
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    DIRTY
}
