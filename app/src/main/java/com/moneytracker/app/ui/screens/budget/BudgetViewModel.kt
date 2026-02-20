package com.moneytracker.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.domain.model.Budget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class BudgetState(
    val budgets: List<Budget> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()

    init {
        observeBudgets()
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

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            _currentMonth.collectLatest { calendar ->
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val (startDate, endDate) = getMonthRange(calendar)

                budgetRepository.getBudgetsWithSpending(month, year, startDate, endDate)
                    .collect { budgets ->
                        _state.update {
                            it.copy(
                                budgets = budgets,
                                totalBudget = budgets.sumOf { b -> b.limitAmount },
                                totalSpent = budgets.sumOf { b -> b.spentAmount },
                                isLoading = false
                            )
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
