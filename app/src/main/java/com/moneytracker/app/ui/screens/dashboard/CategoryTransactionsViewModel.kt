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
import java.text.SimpleDateFormat
import java.util.Locale

data class CategorySummary(
    val totalAmount: Double = 0.0,
    val transactionCount: Int = 0,
    val highestAmount: Double = 0.0,
    val averageAmount: Double = 0.0
)

data class CategoryTrendPoint(
    val label: String,
    val value: Double
)

@HiltViewModel
class CategoryTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
    val categoryName: String = savedStateHandle.get<String>("categoryName") ?: "Category"
    val type: String = savedStateHandle.get<String>("type") ?: "EXPENSE"
    private val initialMonth: Int? = savedStateHandle.get<String>("month")?.toIntOrNull()
    private val initialYear: Int? = savedStateHandle.get<String>("year")?.toIntOrNull()

    private val initialCalendar: Calendar = Calendar.getInstance().apply {
        if (initialMonth != null && initialYear != null) {
            set(Calendar.MONTH, initialMonth - 1)
            set(Calendar.YEAR, initialYear)
        }
    }

    // Null represents "All Time". By default, start on the current month.
    private val _currentMonth = MutableStateFlow<Calendar?>(initialCalendar)
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
        if (type.uppercase() == "TRANSFER") {
            if (cal == null) {
                transactionRepository.getTransferTransactionsByToAccount(categoryId)
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

                transactionRepository.getTransferTransactionsByToAccountForPeriod(categoryId, start, end)
            }
        } else {
            if (cal == null) {
                transactionRepository.getTransactionsByCategory(categoryId, type)
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

                transactionRepository.getTransactionsByCategoryForPeriod(categoryId, type, start, end)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val percentage: StateFlow<Float> = _currentMonth.flatMapLatest { cal ->
        val start = if (cal == null) 0L else (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = if (cal == null) Long.MAX_VALUE else (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        if (type.uppercase() == "INCOME") {
            transactionRepository.getCategoryIncome(start, end).map { list ->
                list.firstOrNull { it.categoryId == categoryId }?.percentage ?: 0f
            }
        } else if (type.uppercase() == "TRANSFER") {
            combine(summary, transactionRepository.getTotalTransfer(start, end)) { currentSummary, totalTransfer ->
                if (totalTransfer > 0.0) ((currentSummary.totalAmount / totalTransfer) * 100.0).toFloat() else 0f
            }
        } else {
            transactionRepository.getCategorySpending(start, end).map { list ->
                list.firstOrNull { it.categoryId == categoryId }?.percentage ?: 0f
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val trendPoints: StateFlow<List<CategoryTrendPoint>> = combine(_currentMonth, transactions) { cal, items ->
        val entries = items.filterIsInstance<TransactionListItem.Entry>().map { it.transaction }
        if (entries.isEmpty()) return@combine emptyList()

        if (cal != null) {
            val daysInMonth = (cal.clone() as Calendar).getActualMaximum(Calendar.DAY_OF_MONTH)
            val daily = DoubleArray(daysInMonth)

            entries.forEach { txn ->
                val day = Calendar.getInstance().apply { timeInMillis = txn.date }.get(Calendar.DAY_OF_MONTH)
                if (day in 1..daysInMonth) {
                    daily[day - 1] += txn.totalAmount
                }
            }

            daily.mapIndexed { index, value ->
                CategoryTrendPoint(label = (index + 1).toString(), value = value)
            }
        } else {
            val monthFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
            entries
                .groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.date }
                    c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
                }
                .toList()
                .sortedWith(compareBy({ it.first.first }, { it.first.second }))
                .takeLast(12)
                .map { (yearMonth, txns) ->
                    val labelCalendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearMonth.first)
                        set(Calendar.MONTH, yearMonth.second)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    CategoryTrendPoint(
                        label = monthFormat.format(labelCalendar.time),
                        value = txns.sumOf { it.totalAmount }
                    )
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
