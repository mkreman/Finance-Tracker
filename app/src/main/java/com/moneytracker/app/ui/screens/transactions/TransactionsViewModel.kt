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
    private val sharedMonthManager: SharedMonthManager // Inject the singleton[cite: 6]
) : ViewModel() {

    // Expose the shared state to the UI[cite: 6]
    val currentMonth: StateFlow<Calendar?> = sharedMonthManager.currentMonth

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val transactions: StateFlow<List<TransactionListItem>> = combine(
        currentMonth,
        _searchQuery
    ) { calendar, query ->
        calendar to query
    }.flatMapLatest { (calendar, query) ->
        val sourceFlow = if (calendar == null) {
            transactionRepository.getAllTransactions() //[cite: 6]
        } else {
            val (startDate, endDate) = getMonthRange(calendar) //[cite: 6]
            transactionRepository.getTransactionsByDateRange(startDate, endDate) //[cite: 6]
        }

        sourceFlow.map { list ->
            if (query.isBlank()) {
                list
            } else {
                val filtered = mutableListOf<TransactionListItem>()
                var currentHeader: TransactionListItem.Header? = null
                var hasEntriesForHeader = false

                for (item in list) {
                    when (item) {
                        is TransactionListItem.Header -> {
                            currentHeader = item
                            hasEntriesForHeader = false
                        }
                        is TransactionListItem.Entry -> {
                            val noteMatches = item.transaction.note?.contains(query.trim(), ignoreCase = true) == true
                            val payeeMatches = item.transaction.payee.contains(query.trim(), ignoreCase = true)
                            
                            if (noteMatches || payeeMatches) {
                                if (!hasEntriesForHeader && currentHeader != null) {
                                    filtered.add(currentHeader)
                                    hasEntriesForHeader = true
                                }
                                filtered.add(item)
                            }
                        }
                    }
                }
                filtered
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) //[cite: 6]

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Delegate actions to the shared manager[cite: 6]
    fun previousMonth() = sharedMonthManager.previousMonth() //[cite: 6]
    fun nextMonth() = sharedMonthManager.nextMonth() //[cite: 6]
    fun selectAllTime() = sharedMonthManager.selectAllTime() //[cite: 6]

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id) //[cite: 6]
        }
    }

    private fun getMonthRange(calendar: Calendar): Pair<Long, Long> {
        val start = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        } //[cite: 6]
        val end = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        } //[cite: 6]
        return start.timeInMillis to end.timeInMillis //[cite: 6]
    }
}
