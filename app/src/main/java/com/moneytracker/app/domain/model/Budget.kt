package com.moneytracker.app.domain.model

data class Budget(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val limitAmount: Double,
    val sortOrder: Int,
    val spentAmount: Double
) {
    val progress: Float
        get() = if (limitAmount > 0) (spentAmount / limitAmount).toFloat() else 0f

    val remaining: Double
        get() = limitAmount - spentAmount

    val isOverBudget: Boolean
        get() = spentAmount > limitAmount
}
