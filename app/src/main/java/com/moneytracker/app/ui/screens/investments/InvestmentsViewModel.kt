package com.moneytracker.app.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.data.local.repository.InvestmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val totalInvested: StateFlow<Double> = investmentRepository.getTotalInvested()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val holdingsCount: StateFlow<Int> = investmentRepository.getHoldingsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val holdings: StateFlow<List<InvestmentEntity>> = investmentRepository.getAllHoldings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCurrentValue: StateFlow<Double> = holdings.map { list ->
        list.sumOf { it.currentValuation }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPnl: StateFlow<Double> = combine(totalCurrentValue, totalInvested) { currentVal, invested ->
        currentVal - invested
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPnlPercent: StateFlow<Double> = combine(totalPnl, totalInvested) { pnl, invested ->
        if (invested > 0.0) (pnl / invested) * 100.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isRefreshing: StateFlow<Boolean> = investmentRepository.isRefreshingPrices
    val lastRefreshedTime: StateFlow<Long?> = investmentRepository.lastRefreshedTime

    init {
        // Auto-refresh on launch if cache expired
        viewModelScope.launch {
            investmentRepository.refreshAllPrices(force = false)
        }
    }

    val filteredHoldings: StateFlow<List<InvestmentEntity>> = combine(
        holdings,
        _searchQuery
    ) { allHoldings, query ->
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            allHoldings
        } else {
            allHoldings.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.symbol.contains(trimmed, ignoreCase = true) ||
                it.exchange.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refreshPrices() {
        viewModelScope.launch {
            investmentRepository.refreshAllPrices(force = true)
        }
    }
}

