package com.moneytracker.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.domain.SharedMonthManager
import com.moneytracker.app.domain.model.Budget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.Collections
import javax.inject.Inject

data class BudgetState(
    val budgets: List<Budget> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sharedMonthManager: SharedMonthManager
) : ViewModel() {

    val currentMonth: StateFlow<Calendar?> = sharedMonthManager.currentMonth

    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()
    private val _copyMessage = MutableStateFlow<String?>(null)
    val copyMessage: StateFlow<String?> = _copyMessage.asStateFlow()

    init {
        observeBudgets()
    }

    fun previousMonth() {
        sharedMonthManager.previousMonth()
    }

    fun nextMonth() {
        sharedMonthManager.nextMonth()
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }

    fun moveBudget(index: Int, direction: Int) {
        val current = _state.value.budgets
        if (current.isEmpty()) return

        val targetIndex = index + direction
        if (targetIndex !in current.indices) return

        val reordered = current.map { it.id }.toMutableList()
        Collections.swap(reordered, index, targetIndex)

        viewModelScope.launch {
            budgetRepository.updateBudgetOrder(reordered)
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val calendar = currentMonth.value ?: return@launch
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val copied = budgetRepository.copyMissingBudgetsFromPreviousMonth(month, year)
            _copyMessage.value = when {
                copied > 0 -> "Copied $copied budget${if (copied > 1) "s" else ""} from last month"
                else -> "No new budgets to copy from last month"
            }
        }
    }

    fun clearCopyMessage() {
        _copyMessage.value = null
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            currentMonth.filterNotNull().collectLatest { calendar ->
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
