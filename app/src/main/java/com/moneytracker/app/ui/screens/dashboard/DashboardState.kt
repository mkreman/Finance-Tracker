package com.moneytracker.app.ui.screens.dashboard

import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.data.local.database.entities.InvestmentTransactionEntity
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.domain.model.Transaction

enum class OverviewType { EXPENSE, INCOME, TRANSFER, INVESTMENT }

data class DashboardState(
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalTransfer: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val totalAccountBalance: Double = 0.0,
    val total: Double = 0.0,
    val categorySpending: List<ChartData> = emptyList(),
    val categoryIncome: List<ChartData> = emptyList(),
    val investmentSpending: List<ChartData> = emptyList(),
    val transferTransactions: List<Transaction> = emptyList(),
    val investmentTransactions: List<InvestmentTransactionEntity> = emptyList(),
    val investmentHoldings: List<InvestmentEntity> = emptyList(),
    val investmentCurrentValuation: Double = 0.0,
    val investmentTotalPnl: Double = 0.0,
    val investmentTotalPnlPercent: Double = 0.0,
    val selectedOverview: OverviewType = OverviewType.EXPENSE,
    val isLoading: Boolean = true,
    val currentMonthMillis: Long = System.currentTimeMillis()
)