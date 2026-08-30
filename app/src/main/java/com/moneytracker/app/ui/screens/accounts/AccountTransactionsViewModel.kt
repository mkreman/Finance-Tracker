package com.moneytracker.app.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.SharedMonthManager
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AccountSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transferIn: Double = 0.0,
    val transferOut: Double = 0.0
) {
    val netTransfer: Double get() = transferIn - transferOut
    val netFlow: Double get() = income + transferIn - expense - transferOut
}

@HiltViewModel
class AccountTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val sharedMonthManager: SharedMonthManager, // Uses the synchronized month manager
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val accountId: String = savedStateHandle.get<String>("accountId") ?: ""
    val accountName: String = savedStateHandle.get<String>("accountName") ?: ""

    // Expose the shared state to the UI
    val currentMonth: StateFlow<Calendar?> = sharedMonthManager.currentMonth

    // Get all transactions for the selected month and filter for this specific account
    val transactions: StateFlow<List<TransactionListItem>> = currentMonth
        .flatMapLatest { calendar ->
            val sourceFlow = if (calendar == null) {
                transactionRepository.getAllTransactions()
            } else {
                val (startDate, endDate) = getMonthRange(calendar)
                transactionRepository.getTransactionsByDateRange(startDate, endDate)
            }
            
            // Filter ensuring the account is involved while preserving the Date Headers properly
            sourceFlow.map { list ->
                val filteredList = mutableListOf<TransactionListItem>()
                var currentHeader: TransactionListItem.Header? = null
                var hasEntriesForHeader = false

                for (item in list) {
                    if (item is TransactionListItem.Header) {
                        currentHeader = item
                        hasEntriesForHeader = false
                    } else if (item is TransactionListItem.Entry) {
                        val t = item.transaction
                        if (t.accountId == accountId || t.toAccountId == accountId) {
                            if (!hasEntriesForHeader && currentHeader != null) {
                                filteredList.add(currentHeader)
                                hasEntriesForHeader = true
                            }
                            filteredList.add(item)
                        }
                    }
                }
                filteredList
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically calculate the summary whenever transactions change
    val summary: StateFlow<AccountSummary> = transactions.map { list ->
        var inc = 0.0
        var exp = 0.0
        var trfIn = 0.0
        var trfOut = 0.0
        
        list.forEach { item ->
            if (item is TransactionListItem.Entry) {
                val t = item.transaction
                if (t.accountId == accountId) {
                    // This account initiated the transaction
                    when (t.type) {
                        TransactionType.INCOME -> inc += t.totalAmount
                        TransactionType.EXPENSE -> exp += t.totalAmount
                        TransactionType.TRANSFER -> trfOut += t.totalAmount // Outbound transfers
                    }
                } else if (t.toAccountId == accountId && t.type == TransactionType.TRANSFER) {
                    // Incoming transfers targeted to this account
                    trfIn += t.totalAmount
                }
            }
        }
        AccountSummary(inc, exp, trfIn, trfOut)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountSummary())

    // Delegate month actions to the shared manager
    fun previousMonth() = sharedMonthManager.previousMonth()
    fun nextMonth() = sharedMonthManager.nextMonth()
    fun selectAll() = sharedMonthManager.selectAllTime()

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
