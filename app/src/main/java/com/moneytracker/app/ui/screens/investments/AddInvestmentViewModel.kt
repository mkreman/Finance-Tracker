package com.moneytracker.app.ui.screens.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.data.local.database.entities.InvestmentTransactionEntity
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.InvestmentRepository
import com.moneytracker.app.domain.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

data class SearchResult(
    val symbol: String,
    val name: String,
    val type: String,
    val exchange: String
)

data class AddInvestmentUiState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class AddInvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialSymbol: String? = savedStateHandle.get<String>("symbol")?.let {
        try {
            android.net.Uri.decode(it).trim()
        } catch (_: Exception) {
            it.trim()
        }
    }
    val initialType: String? = savedStateHandle.get<String>("type")
    val initialTransactionId: String? = savedStateHandle.get<String>("transactionId")
    val isEditMode: Boolean = !initialTransactionId.isNullOrBlank()

    private val _editingTransaction = MutableStateFlow<InvestmentTransactionEntity?>(null)
    val editingTransaction: StateFlow<InvestmentTransactionEntity?> = _editingTransaction.asStateFlow()

    private val _editingAccountId = MutableStateFlow<String?>(null)
    val editingAccountId: StateFlow<String?> = _editingAccountId.asStateFlow()

    init {
        if (isEditMode) {
            val txnId = initialTransactionId!!
            viewModelScope.launch {
                val txn = investmentRepository.getTransactionById(txnId)
                _editingTransaction.value = txn
                val accId = investmentRepository.getLinkedAccountId(txnId)
                _editingAccountId.value = accId
            }
        }
    }

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _uiState = MutableStateFlow(AddInvestmentUiState())
    val uiState: StateFlow<AddInvestmentUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holdings: StateFlow<List<InvestmentEntity>> = investmentRepository.getAllHoldings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvested: StateFlow<Double> = investmentRepository.getTotalInvested()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val holdingsCount: StateFlow<Int> = investmentRepository.getHoldingsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var searchJob: Job? = null

    fun searchAsset(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250) // Small debounce for smooth typing
            _isSearching.value = true
            try {
                val response = fetchQuotes(trimmed)
                val json = JSONObject(response)
                val quotes = json.optJSONArray("quotes")
                val rawResults = mutableListOf<SearchResult>()

                if (quotes != null) {
                    for (i in 0 until quotes.length()) {
                        val quote = quotes.optJSONObject(i) ?: continue
                        val symbol = quote.optString("symbol", "")
                        val shortName = quote.optString("shortname", "")
                        val longName = quote.optString("longname", "")

                        val displayName = when {
                            longName.isNotBlank() -> longName
                            shortName.isNotBlank() -> shortName
                            else -> symbol
                        }
                        val typeDisp = quote.optString("typeDisp", quote.optString("quoteType", "Stock"))
                        val exchange = quote.optString("exchDisp", quote.optString("exchange", ""))

                        if (symbol.isNotBlank() && displayName.isNotBlank()) {
                            rawResults.add(SearchResult(symbol, displayName, typeDisp, exchange))
                        }
                    }
                }

                // Intelligent fuzzy ranking: prioritize Indian stocks/funds (NSE/BSE) and prefix matches
                val qLower = trimmed.lowercase()
                val sortedResults = rawResults.sortedWith(
                    compareBy<SearchResult> { result ->
                        val isIndian = result.symbol.endsWith(".NS", ignoreCase = true) ||
                                       result.symbol.endsWith(".BO", ignoreCase = true) ||
                                       result.exchange.equals("NSE", ignoreCase = true) ||
                                       result.exchange.equals("BSE", ignoreCase = true) ||
                                       result.exchange.contains("Bombay", ignoreCase = true)
                        if (isIndian) 0 else 1
                    }.thenBy { result ->
                        val nameLower = result.name.lowercase()
                        val symbolLower = result.symbol.lowercase()
                        when {
                            symbolLower.startsWith(qLower) -> 0
                            nameLower.startsWith(qLower) -> 1
                            nameLower.split(" ").any { it.startsWith(qLower) } -> 2
                            nameLower.contains(qLower) || symbolLower.contains(qLower) -> 3
                            else -> 4
                        }
                    }
                )

                _searchResults.value = sortedResults
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun saveInvest(
        asset: SearchResult,
        units: String,
        amount: String,
        date: Long,
        accountId: String?
    ) {
        val parsedUnits = units.toDoubleOrNull()
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedUnits == null || parsedUnits <= 0 || parsedAmount == null || parsedAmount <= 0) {
            _uiState.value = _uiState.value.copy(saveError = "Please enter valid units and total amount")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                investmentRepository.saveInvestment(
                    symbol = asset.symbol,
                    name = asset.name,
                    type = asset.type,
                    exchange = asset.exchange,
                    units = parsedUnits,
                    amount = parsedAmount,
                    transactionType = "INVEST",
                    date = date,
                    accountId = accountId
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = "Failed to save: ${e.message}")
            }
        }
    }

    fun saveWithdrawal(
        holding: InvestmentEntity,
        units: String,
        amount: String,
        date: Long,
        accountId: String?
    ) {
        val parsedUnits = units.toDoubleOrNull()
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedUnits == null || parsedUnits <= 0) {
            _uiState.value = _uiState.value.copy(saveError = "Please enter valid units to withdraw")
            return
        }
        if (parsedUnits > holding.totalUnits) {
            _uiState.value = _uiState.value.copy(saveError = "Cannot withdraw more than available ${holding.totalUnits} units")
            return
        }
        if (parsedAmount == null || parsedAmount <= 0) {
            _uiState.value = _uiState.value.copy(saveError = "Please enter valid withdrawal amount")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                investmentRepository.saveInvestment(
                    symbol = holding.symbol,
                    name = holding.name,
                    type = holding.type,
                    exchange = holding.exchange,
                    units = parsedUnits,
                    amount = parsedAmount,
                    transactionType = "WITHDRAW",
                    date = date,
                    accountId = accountId
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = "Failed to save withdrawal: ${e.message}")
            }
        }
    }

    fun deleteInvestmentTransaction() {
        val txnId = initialTransactionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                investmentRepository.deleteInvestmentTransaction(txnId)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = "Failed to delete: ${e.message}")
            }
        }
    }

    fun updateInvestmentTransaction(
        units: String,
        amount: String,
        date: Long,
        accountId: String?
    ) {
        val txnId = initialTransactionId ?: return
        val parsedUnits = units.toDoubleOrNull()
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedUnits == null || parsedUnits <= 0 || parsedAmount == null || parsedAmount <= 0) {
            _uiState.value = _uiState.value.copy(saveError = "Please enter valid units and total amount")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                investmentRepository.updateInvestmentTransaction(
                    transactionId = txnId,
                    units = parsedUnits,
                    amount = parsedAmount,
                    date = date,
                    accountId = accountId
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = "Failed to update: ${e.message}")
            }
        }
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }

    private fun fetchQuotes(query: String): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://query1.finance.yahoo.com/v1/finance/search?q=$encodedQuery&quotesCount=25",
            "https://query2.finance.yahoo.com/v1/finance/search?q=$encodedQuery&quotesCount=25",
            "https://query1.finance.yahoo.com/v1/finance/search?q=$encodedQuery&quotesCount=25&country=IN"
        )

        var lastException: Exception? = null
        for (urlString in endpoints) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode in 200..299) {
                    return connection.inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }
        throw lastException ?: IOException("Failed to fetch quotes")
    }
}