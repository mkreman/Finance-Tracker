package com.moneytracker.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.SharedMonthManager
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val sharedMonthManager: SharedMonthManager // Inject the singleton
) : ViewModel() {

    // Expose the shared state to the UI
    val currentMonth: StateFlow<Calendar?> = sharedMonthManager.currentMonth

    val transactions: StateFlow<List<TransactionListItem>> = currentMonth
        .flatMapLatest { calendar ->
            if (calendar == null) {
                transactionRepository.getAllTransactions()
            } else {
                val (startDate, endDate) = getMonthRange(calendar)
                transactionRepository.getTransactionsByDateRange(startDate, endDate)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Delegate actions to the shared manager
    fun previousMonth() = sharedMonthManager.previousMonth()
    fun nextMonth() = sharedMonthManager.nextMonth()
    fun selectAllTime() = sharedMonthManager.selectAllTime()

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
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
