package com.moneytracker.app.ui.screens.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.InvestmentRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.SharedMonthManager
import com.moneytracker.app.domain.model.ChartData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository,
    private val sharedMonthManager: SharedMonthManager
) : ViewModel() {

    val currentMonth: StateFlow<Calendar?> = sharedMonthManager.currentMonth

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeMonthData()
        observeAccountBalance()
    }

    private fun observeAccountBalance() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                val totalBalance = accounts.sumOf { it.currentBalance }
                _state.update { it.copy(totalAccountBalance = totalBalance) }
            }
        }
    }

    fun previousMonth() = sharedMonthManager.previousMonth()

    fun nextMonth() = sharedMonthManager.nextMonth()

    fun selectOverview(type: OverviewType) {
        _state.update { it.copy(selectedOverview = type) }
    }

    private fun observeMonthData() {
        viewModelScope.launch {
            currentMonth.filterNotNull().collectLatest { calendar ->
                val (startDate, endDate) = getMonthRange(calendar)

                // Observe expense
                launch {
                    transactionRepository.getTotalExpense(startDate, endDate).collect { expense ->
                        _state.update {
                            it.copy(
                                totalExpense = expense,
                                total = it.totalIncome - expense - it.totalInvestment,
                                isLoading = false,
                                currentMonthMillis = calendar.timeInMillis
                            )
                        }
                    }
                }

                // Observe income
                launch {
                    transactionRepository.getTotalIncome(startDate, endDate).collect { income ->
                        _state.update {
                            it.copy(
                                totalIncome = income,
                                total = income - it.totalExpense - it.totalInvestment,
                                isLoading = false
                            )
                        }
                    }
                }

                // Observe transfer total (excluding investments)
                launch {
                    transactionRepository.getTotalTransfer(startDate, endDate).collect { transfer ->
                        _state.update { it.copy(totalTransfer = transfer) }
                    }
                }

                // Observe investment total
                launch {
                    investmentRepository.getTotalInvestedForPeriod(startDate, endDate).collect { investment ->
                        _state.update {
                            it.copy(
                                totalInvestment = investment,
                                total = it.totalIncome - it.totalExpense - investment
                            )
                        }
                    }
                }

                // Observe investment transactions & chart allocation
                launch {
                    val palette = listOf(
                        Color(0xFFFF9800), // Amber
                        Color(0xFF4CAF50), // Green
                        Color(0xFF2196F3), // Blue
                        Color(0xFF9C27B0), // Purple
                        Color(0xFF00BCD4), // Cyan
                        Color(0xFFFF5722), // Deep Orange
                        Color(0xFFE91E63), // Pink
                        Color(0xFF795548)  // Brown
                    )
                    investmentRepository.getTransactionsForPeriod(startDate, endDate).collect { txns ->
                        val investTxns = txns.filter { it.transactionType == "INVEST" }
                        val grouped = investTxns
                            .groupBy { it.symbol }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                            .toList()
                            .sortedByDescending { it.second }

                        val total = grouped.sumOf { it.second }
                        val chartData = grouped.mapIndexed { index, (symbol, amount) ->
                            ChartData(
                                categoryId = symbol,
                                categoryName = symbol,
                                amount = amount,
                                color = palette[index % palette.size],
                                iconKey = "trending_up",
                                percentage = if (total == 0.0) 0f else ((amount / total) * 100.0).toFloat()
                            )
                        }
                        _state.update {
                            it.copy(
                                investmentTransactions = txns,
                                investmentSpending = chartData
                            )
                        }
                    }
                }

                // Observe active investment holdings for real-time valuation & P&L
                launch {
                    investmentRepository.getAllHoldings().collect { holdings ->
                        val currentVal = holdings.sumOf { it.currentValuation }
                        val invested = holdings.sumOf { it.totalInvestedAmount }
                        val pnl = currentVal - invested
                        val pnlPercent = if (invested > 0) (pnl / invested) * 100.0 else 0.0
                        _state.update {
                            it.copy(
                                investmentHoldings = holdings,
                                investmentCurrentValuation = currentVal,
                                investmentTotalPnl = pnl,
                                investmentTotalPnlPercent = pnlPercent
                            )
                        }
                    }
                }

                // Observe category spending (expense)
                launch {
                    transactionRepository.getCategorySpending(startDate, endDate).collect { data ->
                        _state.update {
                            it.copy(categorySpending = data, isLoading = false)
                        }
                    }
                }

                // Observe category income
                launch {
                    transactionRepository.getCategoryIncome(startDate, endDate).collect { data ->
                        _state.update {
                            it.copy(categoryIncome = data, isLoading = false)
                        }
                    }
                }

                // Observe transfer transactions
                launch {
                    transactionRepository.getTransferTransactions(startDate, endDate).collect { transfers ->
                        _state.update { it.copy(transferTransactions = transfers) }
                    }
                }
            }
        }
    }

    private fun getMonthRange(calendar: Calendar): Pair<Long, Long> {
        val start = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return start.timeInMillis to end.timeInMillis
    }
}
