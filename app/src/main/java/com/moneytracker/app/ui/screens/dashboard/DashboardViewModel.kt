package com.moneytracker.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadAccounts()
        observeMonthData()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accountsList ->
                _state.update { it.copy(accounts = accountsList) }
            }
        }
    }

    fun previousMonth() {
        _currentMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        }
    }

    fun nextMonth() {
        _currentMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        }
    }

    fun selectOverview(type: OverviewType) {
        _state.update { it.copy(selectedOverview = type) }
    }

    // Filter handlers
    fun setFromAccountFilter(accountId: String?) {
        _state.update { it.copy(selectedFromAccountId = accountId) }
    }

    fun setToAccountFilter(accountId: String?) {
        _state.update { it.copy(selectedToAccountId = accountId) }
    }

    private fun observeMonthData() {
        viewModelScope.launch {
            _currentMonth.collectLatest { calendar ->
                val (startDate, endDate) = getMonthRange(calendar)

                // Observe expense
                launch {
                    transactionRepository.getTotalExpense(startDate, endDate).collect { expense ->
                        _state.update {
                            it.copy(
                                totalExpense = expense,
                                total = it.totalIncome - expense,
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
                                total = income - it.totalExpense,
                                isLoading = false
                            )
                        }
                    }
                }

                // Observe transfer total
                launch {
                    transactionRepository.getTotalTransfer(startDate, endDate).collect { transfer ->
                        _state.update { it.copy(totalTransfer = transfer) }
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
