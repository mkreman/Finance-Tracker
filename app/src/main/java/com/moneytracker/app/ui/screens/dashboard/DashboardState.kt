package com.moneytracker.app.ui.screens.dashboard

import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.domain.model.Transaction

enum class OverviewType { EXPENSE, INCOME, TRANSFER }

data class DashboardState(
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalTransfer: Double = 0.0,
    val total: Double = 0.0,
    val categorySpending: List<ChartData> = emptyList(),
    val categoryIncome: List<ChartData> = emptyList(),
    val transferTransactions: List<Transaction> = emptyList(),
    val selectedOverview: OverviewType = OverviewType.EXPENSE,
    val isLoading: Boolean = true,
    val currentMonthMillis: Long = System.currentTimeMillis(),
    
    // Transfer filters
    val accounts: List<Account> = emptyList(),
    val selectedFromAccountId: String? = null,
    val selectedToAccountId: String? = null
)