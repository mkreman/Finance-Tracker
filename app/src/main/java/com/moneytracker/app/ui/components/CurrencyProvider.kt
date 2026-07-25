package com.moneytracker.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * CompositionLocal for the active currency symbol (e.g. "₹", "$", "€").
 * Provided at the app root and consumed wherever currency is displayed.
 */
val LocalCurrencySymbol = compositionLocalOf { "₹" }

fun formatAmount(amount: Double): String {
    if (amount == 0.0) return "0"
    
    // Format to 2 decimal places, stripping trailing zeros
    val bd = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
    val parts = bd.toPlainString().split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1 && parts[1].toLong() > 0) ".${parts[1].trimEnd('0')}" else ""

    // Apply Indian Comma System (##,##,##,###) to the integer part
    val formattedInteger = if (integerPart.length > 3) {
        val lastThree = integerPart.takeLast(3)
        val remaining = integerPart.dropLast(3)
        val formattedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$formattedRemaining,$lastThree"
    } else {
        integerPart
    }

    return "$formattedInteger$decimalPart"
}