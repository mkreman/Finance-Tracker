package com.moneytracker.app.ui.screens.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.Budget
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

data class BudgetTransactionsState(
    val budget: Budget? = null,
    val transactions: List<TransactionListItem> = emptyList()
)

@HiltViewModel
class BudgetTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
    val categoryName: String = savedStateHandle.get<String>("categoryName") ?: "Budget"
    private val initialMonth: Int? = savedStateHandle.get<String>("month")?.toIntOrNull()
    private val initialYear: Int? = savedStateHandle.get<String>("year")?.toIntOrNull()

    private val initialCalendar: Calendar = Calendar.getInstance().apply {
        if (initialMonth != null && initialYear != null) {
            set(Calendar.MONTH, initialMonth - 1)
            set(Calendar.YEAR, initialYear)
        }
    }

    private val _currentMonth = MutableStateFlow(initialCalendar)
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    val state: StateFlow<BudgetTransactionsState> = _currentMonth.flatMapLatest { calendar ->
        val (startDate, endDate) = getMonthRange(calendar)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        val transactionsFlow = transactionRepository.getTransactionsByCategoryForPeriod(
            categoryId, startDate, endDate
        )
        val budgetFlow = budgetRepository.getBudgetsWithSpending(month, year, startDate, endDate)
            .map { budgets -> budgets.find { it.categoryId == categoryId } }

        combine(transactionsFlow, budgetFlow) { transactions, budget ->
            BudgetTransactionsState(
                budget = budget,
                transactions = transactions
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetTransactionsState())

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
