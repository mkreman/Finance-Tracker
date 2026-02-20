package com.moneytracker.app.ui.screens.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.util.Calendar

data class CategorySummary(
    val totalAmount: Double = 0.0,
    val transactionCount: Int = 0,
    val highestAmount: Double = 0.0,
    val averageAmount: Double = 0.0
)

@HiltViewModel
class CategoryTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
    val categoryName: String = savedStateHandle.get<String>("categoryName") ?: "Category"
    val type: String = savedStateHandle.get<String>("type") ?: "EXPENSE"

    val transactions: StateFlow<List<TransactionListItem>> =
        transactionRepository.getTransactionsByCategory(categoryId, type)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<CategorySummary> =
        transactions.map { items ->
            val entries = items.filterIsInstance<TransactionListItem.Entry>()
            val amounts = entries.map { it.transaction.totalAmount }
            CategorySummary(
                totalAmount = amounts.sum(),
                transactionCount = entries.size,
                highestAmount = amounts.maxOrNull() ?: 0.0,
                averageAmount = if (entries.isNotEmpty()) amounts.sum() / entries.size else 0.0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategorySummary())

    // Percentage of this category relative to the month's total for the selected type
    val percentage: StateFlow<Float> = flow {
        // use current month range
        val cal = Calendar.getInstance()
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

        emitAll(if (type.uppercase() == "INCOME") {
            transactionRepository.getCategoryIncome(start, end).map { list ->
                list.firstOrNull { it.categoryId == categoryId }?.percentage ?: 0f
            }
        } else {
            transactionRepository.getCategorySpending(start, end).map { list ->
                list.firstOrNull { it.categoryId == categoryId }?.percentage ?: 0f
            }
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
}
