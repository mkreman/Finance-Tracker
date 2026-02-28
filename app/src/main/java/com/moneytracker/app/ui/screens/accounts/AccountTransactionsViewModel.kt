package com.moneytracker.app.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AccountTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val accountId: String = savedStateHandle.get<String>("accountId") ?: ""
    val accountName: String = savedStateHandle.get<String>("accountName") ?: "Account"

    // Null represents "All Time". Default starts at the current month.
    private val _currentMonth = MutableStateFlow<Calendar?>(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar?> = _currentMonth.asStateFlow()

    fun previousMonth() {
        _currentMonth.update { cal ->
            if (cal == null) {
                Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            } else {
                (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }
        }
    }

    fun nextMonth() {
        _currentMonth.update { cal ->
            if (cal == null) {
                Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            } else {
                (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }
        }
    }

    fun selectAll() {
        _currentMonth.value = null
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionListItem>> = _currentMonth.flatMapLatest { cal ->
        if (cal == null) {
            transactionRepository.getTransactionsByAccount(accountId)
        } else {
            val start = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            transactionRepository.getTransactionsByAccountForPeriod(accountId, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
