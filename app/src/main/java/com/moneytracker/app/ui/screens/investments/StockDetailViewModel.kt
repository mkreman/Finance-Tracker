package com.moneytracker.app.ui.screens.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.data.local.database.entities.InvestmentTransactionEntity
import com.moneytracker.app.data.local.repository.InvestmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockSummaryMetrics(
    val totalInvested: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val totalBoughtUnits: Double = 0.0,
    val totalWithdrawnUnits: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val transactionCount: Int = 0
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rawSymbol: String = checkNotNull(savedStateHandle.get<String>("symbol"))
    val symbol: String = try {
        android.net.Uri.decode(rawSymbol).trim().uppercase()
    } catch (_: Exception) {
        rawSymbol.trim().uppercase()
    }

    val holding: StateFlow<InvestmentEntity?> = investmentRepository.getHolding(symbol)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshPrice()
    }

    fun refreshPrice() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                investmentRepository.refreshPriceForSymbol(symbol)
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val transactions: StateFlow<List<InvestmentTransactionEntity>> = investmentRepository.getTransactionsForSymbol(symbol)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metrics: StateFlow<StockSummaryMetrics> = transactions.map { txns ->
        var invested = 0.0
        var withdrawn = 0.0
        var boughtUnits = 0.0
        var soldUnits = 0.0

        txns.forEach { txn ->
            if (txn.transactionType == "INVEST") {
                invested += txn.amount
                boughtUnits += txn.units
            } else {
                withdrawn += txn.amount
                soldUnits += txn.units
            }
        }

        StockSummaryMetrics(
            totalInvested = invested,
            totalWithdrawn = withdrawn,
            totalBoughtUnits = boughtUnits,
            totalWithdrawnUnits = soldUnits,
            netCashFlow = withdrawn - invested,
            transactionCount = txns.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockSummaryMetrics())
}

