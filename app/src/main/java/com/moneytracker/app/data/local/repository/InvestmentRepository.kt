package com.moneytracker.app.data.local.repository

import androidx.room.withTransaction
import com.moneytracker.app.data.local.database.MoneyTrackerDatabase
import com.moneytracker.app.data.local.database.dao.AccountDao
import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.dao.InvestmentDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import com.moneytracker.app.data.local.database.entities.*
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.remote.StockPriceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestmentRepository @Inject constructor(
    private val database: MoneyTrackerDatabase,
    private val investmentDao: InvestmentDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val stockPriceService: StockPriceService,
    private val userPreferences: UserPreferences
) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureInvestmentTransferCategory()
                transactionDao.convertInvestmentExpensesToTransfers()
                transactionDao.convertInvestmentSplitsToTransfers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllHoldings(): Flow<List<InvestmentEntity>> =
        investmentDao.getAllInvestments()

    fun getTotalInvested(): Flow<Double> =
        investmentDao.getTotalInvested().map { it ?: 0.0 }

    fun getHoldingsCount(): Flow<Int> =
        investmentDao.getHoldingsCount()

    fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>> =
        investmentDao.getAllTransactions()

    fun getTransactionsForSymbol(symbol: String): Flow<List<InvestmentTransactionEntity>> =
        investmentDao.getTransactionsForSymbol(symbol.trim().uppercase())

    fun getHolding(symbol: String): Flow<InvestmentEntity?> =
        investmentDao.observeBySymbol(symbol.trim().uppercase())

    suspend fun getHoldingOnce(symbol: String): InvestmentEntity? =
        investmentDao.getBySymbol(symbol.trim().uppercase())

    suspend fun saveInvestment(
        symbol: String,
        name: String,
        type: String,
        exchange: String,
        units: Double,
        amount: Double,
        transactionType: String,
        date: Long,
        accountId: String? = null
    ) {
        val cleanSymbol = symbol.trim().uppercase()
        val cleanName = name.trim()
        val cleanType = type.trim()
        val cleanExchange = exchange.trim()

        database.withTransaction {
            val now = System.currentTimeMillis()
            val existing = investmentDao.getBySymbol(cleanSymbol)

            // 1. Manage Holding (One entry per stock symbol)
            if (transactionType == "INVEST") {
                val updated = if (existing != null) {
                    existing.copy(
                        name = cleanName,
                        type = cleanType,
                        exchange = cleanExchange,
                        totalUnits = existing.totalUnits + units,
                        totalInvestedAmount = existing.totalInvestedAmount + amount,
                        modifiedAt = now,
                        isDeleted = false
                    )
                } else {
                    InvestmentEntity(
                        symbol = cleanSymbol,
                        name = cleanName,
                        type = cleanType,
                        exchange = cleanExchange,
                        totalUnits = units,
                        totalInvestedAmount = amount,
                        createdAt = now,
                        modifiedAt = now
                    )
                }
                investmentDao.upsertInvestment(updated)
            } else {
                // WITHDRAW
                if (existing != null) {
                    val newUnits = existing.totalUnits - units
                    val newAmount = existing.totalInvestedAmount - amount
                    if (newUnits <= 0.0) {
                        investmentDao.softDelete(cleanSymbol, now)
                    } else {
                        investmentDao.upsertInvestment(
                            existing.copy(
                                name = cleanName,
                                type = cleanType,
                                exchange = cleanExchange,
                                totalUnits = newUnits,
                                totalInvestedAmount = maxOf(0.0, newAmount),
                                modifiedAt = now
                            )
                        )
                    }
                }
            }

            // 2. Log Investment Transaction Record
            val txnId = UUID.randomUUID().toString()
            investmentDao.insertTransaction(
                InvestmentTransactionEntity(
                    id = txnId,
                    symbol = cleanSymbol,
                    transactionType = transactionType,
                    units = units,
                    amount = amount,
                    date = date,
                    createdAt = now
                )
            )

            // 3. Create Main Transaction Entry for Transactions List & Account Balance
            val allAccounts = accountDao.getAllAccounts().first()
            val targetAccountId = if (!accountId.isNullOrBlank() && allAccounts.any { it.id == accountId }) {
                accountId
            } else {
                // Prefer Investment account if exists, then Bank, then Wallet, then first active
                allAccounts.firstOrNull { it.type == AccountType.INVESTMENT }?.id
                    ?: allAccounts.firstOrNull { it.type == AccountType.BANK }?.id
                    ?: allAccounts.firstOrNull { it.type == AccountType.WALLET }?.id
                    ?: allAccounts.firstOrNull()?.id
            }

            if (targetAccountId != null) {
                val txnType = TransactionType.TRANSFER
                val categoryId = ensureInvestmentTransferCategory()
                val unitPrice = if (units > 0) amount / units else 0.0
                val noteText = "${if (transactionType == "INVEST") "Bought" else "Sold"} $units units @ ₹${String.format(Locale.US, "%.2f", unitPrice)} ($exchange)"

                val transactionEntity = TransactionEntity(
                    id = txnId,
                    accountId = targetAccountId,
                    payee = name,
                    note = noteText,
                    date = date,
                    totalAmount = amount,
                    type = txnType,
                    createdAt = now,
                    modifiedAt = now,
                    syncStatus = SyncStatus.PENDING
                )

                val splitEntity = TransactionSplitEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = txnId,
                    categoryId = categoryId,
                    amount = amount,
                    note = noteText
                )

                transactionDao.insertTransaction(transactionEntity)
                transactionDao.insertSplits(listOf(splitEntity))

                // Adjust balance of the account
                val balanceDelta = if (transactionType == "INVEST") -amount else amount
                accountDao.updateBalance(targetAccountId, balanceDelta)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshPriceForSymbol(cleanSymbol)
            } catch (_: Exception) {}
        }
    }

    suspend fun getTransactionById(id: String): InvestmentTransactionEntity? =
        investmentDao.getTransactionById(id)

    suspend fun getLinkedAccountId(transactionId: String): String? =
        transactionDao.getTransactionById(transactionId)?.transaction?.accountId

    suspend fun deleteInvestmentTransaction(transactionId: String) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            val txn = investmentDao.getTransactionById(transactionId) ?: return@withTransaction
            val symbol = txn.symbol

            // 1. Delete from investment_transactions
            investmentDao.deleteTransactionById(transactionId)

            // 2. Recompute holding totals from remaining transactions
            val remainingTxns = investmentDao.getTransactionsListForSymbol(symbol)
            val totalUnits = remainingTxns.filter { it.transactionType == "INVEST" }.sumOf { it.units } -
                    remainingTxns.filter { it.transactionType == "WITHDRAW" }.sumOf { it.units }
            val totalInvested = remainingTxns.filter { it.transactionType == "INVEST" }.sumOf { it.amount } -
                    remainingTxns.filter { it.transactionType == "WITHDRAW" }.sumOf { it.amount }

            if (totalUnits <= 0.0) {
                investmentDao.softDelete(symbol, now)
            } else {
                val existing = investmentDao.getBySymbol(symbol)
                if (existing != null) {
                    investmentDao.updateInvestment(
                        existing.copy(
                            totalUnits = totalUnits,
                            totalInvestedAmount = maxOf(0.0, totalInvested),
                            modifiedAt = now,
                            isDeleted = false
                        )
                    )
                }
            }

            // 3. Delete matching ledger transaction & restore account balance
            val generalTxnWithSplits = transactionDao.getTransactionById(transactionId)
            if (generalTxnWithSplits != null) {
                val generalTxn = generalTxnWithSplits.transaction
                transactionDao.deleteSplitsByTransactionId(transactionId)
                transactionDao.softDeleteTransaction(transactionId, now)
                val balanceDelta = if (txn.transactionType == "WITHDRAW") -generalTxn.totalAmount else generalTxn.totalAmount
                accountDao.updateBalance(generalTxn.accountId, balanceDelta)
            }
        }
    }

    suspend fun ensureInvestmentTransferCategory(): String {
        val preferredCatId = "cat-investment-transfer"
        val existing = categoryDao.getCategoryById(preferredCatId)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val newCategory = CategoryEntity(
                id = preferredCatId,
                name = "Investment",
                iconKey = "trending_up",
                type = TransactionType.TRANSFER,
                colorHex = "#FF9800",
                sortOrder = 12,
                isDeleted = false,
                createdAt = now,
                modifiedAt = now
            )
            categoryDao.insertCategory(newCategory)
        }
        return preferredCatId
    }

    suspend fun updateInvestmentTransaction(
        transactionId: String,
        units: Double,
        amount: Double,
        date: Long,
        accountId: String? = null
    ) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            val oldTxn = investmentDao.getTransactionById(transactionId) ?: return@withTransaction
            val symbol = oldTxn.symbol

            // 1. Update investment transaction
            val updatedTxn = oldTxn.copy(
                units = units,
                amount = amount,
                date = date
            )
            investmentDao.updateTransaction(updatedTxn)

            // 2. Recompute holding totals from all transactions
            val allTxns = investmentDao.getTransactionsListForSymbol(symbol)
            val totalUnits = allTxns.filter { it.transactionType == "INVEST" }.sumOf { it.units } -
                    allTxns.filter { it.transactionType == "WITHDRAW" }.sumOf { it.units }
            val totalInvested = allTxns.filter { it.transactionType == "INVEST" }.sumOf { it.amount } -
                    allTxns.filter { it.transactionType == "WITHDRAW" }.sumOf { it.amount }

            if (totalUnits <= 0.0) {
                investmentDao.softDelete(symbol, now)
            } else {
                val existing = investmentDao.getInvestmentBySymbolAny(symbol)
                if (existing != null) {
                    investmentDao.updateInvestment(
                        existing.copy(
                            totalUnits = totalUnits,
                            totalInvestedAmount = maxOf(0.0, totalInvested),
                            modifiedAt = now,
                            isDeleted = false
                        )
                    )
                }
            }

            // 3. Update matching ledger transaction & adjust account balance
            val categoryId = ensureInvestmentTransferCategory()
            val generalTxnWithSplits = transactionDao.getTransactionById(transactionId)
            val allAccounts = accountDao.getAllAccounts().first()

            if (generalTxnWithSplits != null) {
                val oldGeneralTxn = generalTxnWithSplits.transaction
                val oldBalanceDelta = if (oldTxn.transactionType == "WITHDRAW") -oldGeneralTxn.totalAmount else oldGeneralTxn.totalAmount
                accountDao.updateBalance(oldGeneralTxn.accountId, oldBalanceDelta)

                val targetAccount = (accountId?.let { id -> allAccounts.firstOrNull { it.id == id } })
                    ?: allAccounts.firstOrNull { it.id == oldGeneralTxn.accountId }
                    ?: allAccounts.firstOrNull { it.type == AccountType.INVESTMENT }
                    ?: allAccounts.firstOrNull { it.type == AccountType.BANK }
                    ?: allAccounts.firstOrNull()
                val targetAccountId = targetAccount?.id ?: oldGeneralTxn.accountId

                val unitPrice = if (units > 0) amount / units else 0.0
                val noteText = "${if (oldTxn.transactionType == "INVEST") "Bought" else "Sold"} $units units @ ₹${String.format(Locale.US, "%.2f", unitPrice)}"

                val targetType = TransactionType.TRANSFER
                val newGeneralTxn = oldGeneralTxn.copy(
                    accountId = targetAccountId,
                    totalAmount = amount,
                    type = targetType,
                    date = date,
                    note = noteText,
                    modifiedAt = now
                )
                transactionDao.updateTransaction(newGeneralTxn)

                transactionDao.deleteSplitsByTransactionId(transactionId)
                val split = TransactionSplitEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    categoryId = categoryId,
                    amount = amount,
                    note = noteText
                )
                transactionDao.insertSplits(listOf(split))

                val newBalanceDelta = if (oldTxn.transactionType == "INVEST") -amount else amount
                accountDao.updateBalance(targetAccountId, newBalanceDelta)
            } else {
                val targetAccount = (accountId?.let { id -> allAccounts.firstOrNull { it.id == id } })
                    ?: allAccounts.firstOrNull { it.type == AccountType.INVESTMENT }
                    ?: allAccounts.firstOrNull { it.type == AccountType.BANK }
                    ?: allAccounts.firstOrNull { it.type == AccountType.WALLET }
                    ?: allAccounts.firstOrNull()

                if (targetAccount != null) {
                    val unitPrice = if (units > 0) amount / units else 0.0
                    val noteText = "${if (oldTxn.transactionType == "INVEST") "Bought" else "Sold"} $units units @ ₹${String.format(Locale.US, "%.2f", unitPrice)}"

                    val newGeneralTxn = TransactionEntity(
                        id = transactionId,
                        accountId = targetAccount.id,
                        payee = symbol,
                        note = noteText,
                        date = date,
                        totalAmount = amount,
                        type = TransactionType.TRANSFER,
                        createdAt = now,
                        modifiedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                    transactionDao.insertTransaction(newGeneralTxn)

                    val split = TransactionSplitEntity(
                        id = UUID.randomUUID().toString(),
                        transactionId = transactionId,
                        categoryId = categoryId,
                        amount = amount,
                        note = noteText
                    )
                    transactionDao.insertSplits(listOf(split))

                    val newBalanceDelta = if (oldTxn.transactionType == "INVEST") -amount else amount
                    accountDao.updateBalance(targetAccount.id, newBalanceDelta)
                }
            }
        }
    }

    fun getTotalInvestedForPeriod(startDate: Long, endDate: Long): Flow<Double> =
        investmentDao.getTotalInvestedForPeriod(startDate, endDate).map { it ?: 0.0 }

    fun getTransactionsForPeriod(startDate: Long, endDate: Long): Flow<List<InvestmentTransactionEntity>> =
        investmentDao.getTransactionsForPeriod(startDate, endDate)

    suspend fun clearAllData() {
        database.withTransaction {
            investmentDao.clearAllTransactions()
            investmentDao.clearAllInvestments()
        }
    }

    private val _isRefreshingPrices = MutableStateFlow(false)
    val isRefreshingPrices: StateFlow<Boolean> = _isRefreshingPrices.asStateFlow()

    private val _lastRefreshedTime = MutableStateFlow<Long?>(null)
    val lastRefreshedTime: StateFlow<Long?> = _lastRefreshedTime.asStateFlow()

    suspend fun refreshAllPrices(force: Boolean = false) {
        if (_isRefreshingPrices.value) return
        val holdings = investmentDao.getActiveHoldings()
        if (holdings.isEmpty()) return

        val now = System.currentTimeMillis()
        if (!force) {
            val intervalMinutes = userPreferences.stockAutoRefreshInterval.first()
            if (intervalMinutes <= 0) return // Manual only
            val lastTime = _lastRefreshedTime.value ?: holdings.mapNotNull { it.priceUpdatedAt }.minOrNull() ?: 0L
            if (now - lastTime < intervalMinutes * 60 * 1000L) {
                return // Cache still valid
            }
        }

        _isRefreshingPrices.value = true
        try {
            val symbols = holdings.map { it.symbol }
            val quotes = stockPriceService.fetchQuotesBatch(symbols)
            for ((symbol, quote) in quotes) {
                investmentDao.updateStockPrice(
                    symbol = symbol,
                    currentPrice = quote.currentPrice,
                    dayChangePercent = quote.dayChangePercent,
                    timestamp = now
                )
            }
            _lastRefreshedTime.value = now
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isRefreshingPrices.value = false
        }
    }

    suspend fun refreshPriceForSymbol(symbol: String) {
        val cleanSymbol = symbol.trim().uppercase()
        val quote = stockPriceService.fetchQuote(cleanSymbol) ?: return
        investmentDao.updateStockPrice(
            symbol = cleanSymbol,
            currentPrice = quote.currentPrice,
            dayChangePercent = quote.dayChangePercent,
            timestamp = System.currentTimeMillis()
        )
    }
}
