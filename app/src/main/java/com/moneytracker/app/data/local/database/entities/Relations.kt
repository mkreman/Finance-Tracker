package com.moneytracker.app.data.local.database.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Transaction with its splits and account details.
 */
data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val splits: List<TransactionSplitEntity>,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity
)

/**
 * Transaction with just its splits (for simpler queries).
 */
data class TransactionWithSplits(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val splits: List<TransactionSplitEntity>
)

/**
 * Category spending aggregation result.
 */
data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val colorHex: String,
    val iconKey: String,
    val total: Double
)

/**
 * Budget with its current spending.
 */
data class BudgetWithSpending(
    val budgetId: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val limitAmount: Double,
    val sortOrder: Int,
    val spentAmount: Double
)
