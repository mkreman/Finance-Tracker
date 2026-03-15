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
    val evaluatedAmount: Double?
        get() = evaluateAmountExpression(amount)

    val totalAmount: Double
        get() = evaluatedAmount ?: 0.0

    val splitsTotal: Double
        get() = splits.sumOf { evaluateAmountExpression(it.amount) ?: 0.0 }

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
                return splits.all { it.categoryId != null && (evaluateAmountExpression(it.amount) ?: 0.0) > 0 }
                        && kotlin.math.abs(remaining) < 0.01
            }
            return selectedCategoryIds.isNotEmpty() &&
                    splits.any { it.categoryId != null }
        }
}

data class SplitState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val categoryId: String? = null,
    val categoryName: String = "",
    val amount: String = ""
)

fun evaluateAmountExpression(input: String): Double? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val normalized = trimmed.replace(" ", "")
    if (normalized.isEmpty()) return null

    fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        else -> 0
    }

    fun apply(values: MutableList<Double>, op: Char): Boolean {
        if (values.size < 2) return false
        val right = values.removeAt(values.lastIndex)
        val left = values.removeAt(values.lastIndex)
        val result = when (op) {
            '+' -> left + right
            '-' -> left - right
            '*' -> left * right
            '/' -> {
                if (kotlin.math.abs(right) < 1e-12) return false
                left / right
            }
            else -> return false
        }
        if (!result.isFinite()) return false
        values.add(result)
        return true
    }

    val values = mutableListOf<Double>()
    val operators = mutableListOf<Char>()
    var index = 0
    var expectNumber = true

    while (index < normalized.length) {
        if (expectNumber) {
            if (index >= normalized.length) return null

            var sign = 1.0
            if (normalized[index] == '+' || normalized[index] == '-') {
                if (normalized[index] == '-') sign = -1.0
                index++
            }

            if (index >= normalized.length) return null

            val start = index
            while (index < normalized.length && (normalized[index].isDigit() || normalized[index] == '.')) {
                index++
            }
            if (start == index) return null

            val parsed = normalized.substring(start, index).toDoubleOrNull() ?: return null
            val value = sign * parsed
            if (!value.isFinite()) return null
            values.add(value)
            expectNumber = false
        } else {
            val op = normalized[index]
            if (op != '+' && op != '-' && op != '*' && op != '/') return null

            while (operators.isNotEmpty() && precedence(operators.last()) >= precedence(op)) {
                if (!apply(values, operators.removeAt(operators.lastIndex))) return null
            }
            operators.add(op)
            index++
            expectNumber = true
        }
    }

    if (expectNumber) return null

    while (operators.isNotEmpty()) {
        if (!apply(values, operators.removeAt(operators.lastIndex))) return null
    }

    if (values.size != 1) return null
    return values.first().takeIf { it.isFinite() }
}
