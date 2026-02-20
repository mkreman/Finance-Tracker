package com.moneytracker.app.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for the active currency symbol (e.g. "₹", "$", "€").
 * Provided at the app root and consumed wherever currency is displayed.
 */
val LocalCurrencySymbol = compositionLocalOf { "₹" }
