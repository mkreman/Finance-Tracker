package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.MoneyTrackerDatabase
import com.moneytracker.app.data.local.database.dao.AccountDao
import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import com.moneytracker.app.data.local.database.entities.*
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.domain.model.Transaction
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.domain.model.TransactionSplit
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: MoneyTrackerDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao
) {
    fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionListItem>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate).map { list ->
            toGroupedList(list)
        }

    fun getAllTransactions(): Flow<List<TransactionListItem>> =
        transactionDao.getAllTransactionsWithDetails().map { list ->
            toGroupedList(list)
        }

    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalExpenseForPeriod(startDate, endDate).map { it ?: 0.0 }

    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalIncomeForPeriod(startDate, endDate).map { it ?: 0.0 }

    fun getTotalTransfer(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalTransferForPeriod(startDate, endDate).map { it ?: 0.0 }

    fun getCategoryIncome(startDate: Long, endDate: Long): Flow<List<ChartData>> =
        transactionDao.getCategoryIncomeForPeriod(startDate, endDate).map { list ->
            val total = list.sumOf { it.total }
            list.map { spending ->
                ChartData(
                    categoryId = spending.categoryId,
                    categoryName = spending.categoryName,
                    amount = spending.total,
                    color = parseColor(spending.colorHex),
                    iconKey = spending.iconKey,
                    percentage = if (total > 0) (spending.total / total * 100).toFloat() else 0f
                )
            }
        }

    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionListItem>> =
        transactionDao.getTransactionsByAccountIncludingTransfers(accountId).map { list ->
            toGroupedList(list)
        }

    fun getTransactionsByCategory(categoryId: String, type: String): Flow<List<TransactionListItem>> =
        transactionDao.getTransactionsByCategory(categoryId, type).map { list ->
            toGroupedList(list)
        }

    fun getTransactionsByCategoryForPeriod(
        categoryId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionListItem>> =
        transactionDao.getTransactionsByCategoryForPeriod(categoryId, startDate, endDate).map { list ->
            toGroupedList(list)
        }

    fun getTransferTransactions(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransferTransactionsForPeriod(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }

    fun getCategorySpending(startDate: Long, endDate: Long): Flow<List<ChartData>> =
        transactionDao.getCategorySpendingForPeriod(startDate, endDate).map { list ->
            val total = list.sumOf { it.total }
            list.map { spending ->
                ChartData(
                    categoryId = spending.categoryId,
                    categoryName = spending.categoryName,
                    amount = spending.total,
                    color = parseColor(spending.colorHex),
                    iconKey = spending.iconKey,
                    percentage = if (total > 0) (spending.total / total * 100).toFloat() else 0f
                )
            }
        }

    suspend fun getTransactionById(id: String): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    suspend fun getAllTransactionsRaw() =
        transactionDao.getAllTransactionsOnce()

    suspend fun getAllTransactionsRawIncludingDeleted() =
        transactionDao.getAllTransactionsIncludingDeletedOnce()

    suspend fun saveTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ) {
        database.withTransaction {
            // Insert transaction and splits directly (avoid DAO default methods
            // to ensure proper coroutine context propagation within withTransaction)
            transactionDao.insertTransaction(transaction)
            if (splits.isNotEmpty()) {
                transactionDao.insertSplits(splits)
            }

            // Update account balance
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    accountDao.updateBalance(transaction.accountId, -transaction.totalAmount)
                }
                TransactionType.INCOME -> {
                    accountDao.updateBalance(transaction.accountId, transaction.totalAmount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(transaction.accountId, -transaction.totalAmount)
                    transaction.toAccountId?.let { toId ->
                        accountDao.updateBalance(toId, transaction.totalAmount)
                    }
                }
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            val txn = transactionDao.getTransactionByIdInternal(id) ?: return@withTransaction
            // Reverse the balance effect
            when (txn.transaction.type) {
                TransactionType.EXPENSE -> {
                    accountDao.updateBalance(txn.transaction.accountId, txn.transaction.totalAmount)
                }
                TransactionType.INCOME -> {
                    accountDao.updateBalance(txn.transaction.accountId, -txn.transaction.totalAmount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(txn.transaction.accountId, txn.transaction.totalAmount)
                    txn.transaction.toAccountId?.let { toId ->
                        accountDao.updateBalance(toId, -txn.transaction.totalAmount)
                    }
                }
            }
            transactionDao.softDeleteTransaction(id)
        }
    }

    suspend fun updateTransaction(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity,
        newSplits: List<TransactionSplitEntity>
    ) {
        database.withTransaction {
            // Reverse old balance
            when (oldTransaction.type) {
                TransactionType.EXPENSE -> accountDao.updateBalance(oldTransaction.accountId, oldTransaction.totalAmount)
                TransactionType.INCOME -> accountDao.updateBalance(oldTransaction.accountId, -oldTransaction.totalAmount)
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(oldTransaction.accountId, oldTransaction.totalAmount)
                    oldTransaction.toAccountId?.let { accountDao.updateBalance(it, -oldTransaction.totalAmount) }
                }
            }
            // Apply new balance
            when (newTransaction.type) {
                TransactionType.EXPENSE -> accountDao.updateBalance(newTransaction.accountId, -newTransaction.totalAmount)
                TransactionType.INCOME -> accountDao.updateBalance(newTransaction.accountId, newTransaction.totalAmount)
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(newTransaction.accountId, -newTransaction.totalAmount)
                    newTransaction.toAccountId?.let { accountDao.updateBalance(it, newTransaction.totalAmount) }
                }
            }
            // Update transaction and splits directly (avoid DAO default methods
            // to ensure proper coroutine context propagation within withTransaction)
            transactionDao.insertTransaction(newTransaction)
            transactionDao.deleteSplitsByTransactionId(newTransaction.id)
            if (newSplits.isNotEmpty()) {
                transactionDao.insertSplits(newSplits)
            }
        }
    }

    suspend fun updateTransactionFull(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ) {
        val old = transactionDao.getTransactionById(transaction.id)
        if (old != null) {
            val preservedTransaction = transaction.copy(createdAt = old.transaction.createdAt)
            updateTransaction(old.transaction, preservedTransaction, splits)
        } else {
            saveTransaction(transaction, splits)
        }
    }

    private suspend fun toGroupedList(list: List<TransactionWithSplits>): List<TransactionListItem> {
        val sdf = SimpleDateFormat("MMM dd, EEEE", Locale.getDefault())
        val grouped = list.groupBy { txn ->
            sdf.format(Date(txn.transaction.date))
        }
        return buildList {
            grouped.forEach { (dateLabel, transactions) ->
                val dayExpense = transactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.totalAmount }
                val dayIncome = transactions
                    .filter { it.transaction.type == TransactionType.INCOME }
                    .sumOf { it.transaction.totalAmount }
                val dayTransfer = transactions
                    .filter { it.transaction.type == TransactionType.TRANSFER }
                    .sumOf { it.transaction.totalAmount }
                add(TransactionListItem.Header(dateLabel, transactions.first().transaction.date, dayExpense, dayIncome, dayTransfer))
                transactions.forEach { txnWithSplits ->
                    runCatching {
                        txnWithSplits.toDomain()
                    }.onSuccess { transaction ->
                        add(TransactionListItem.Entry(transaction))
                    }
                }
            }
        }
    }

    private suspend fun TransactionWithSplits.toDomain(): Transaction {
        val account = accountDao.getAccountById(transaction.accountId)
        val toAccount = transaction.toAccountId?.let { accountDao.getAccountById(it) }
        return Transaction(
            id = transaction.id,
            accountId = transaction.accountId,
            accountName = account?.name ?: "Unknown",
            payee = transaction.payee,
            note = transaction.note,
            date = transaction.date,
            totalAmount = transaction.totalAmount,
            type = transaction.type,
            isRecurring = transaction.isRecurring,
            recurringInterval = transaction.recurringInterval,
            recurringUnit = transaction.recurringUnit,
            recurringEndDate = transaction.recurringEndDate,
            parentRecurringId = transaction.parentRecurringId,
            notifyForRecurringEntries = transaction.notifyForRecurringEntries,
            toAccountId = transaction.toAccountId,
            toAccountName = toAccount?.name,
            splits = splits.map { split ->
                val category = categoryDao.getCategoryById(split.categoryId)
                TransactionSplit(
                    id = split.id,
                    categoryId = split.categoryId,
                    categoryName = category?.name ?: "Unknown",
                    categoryColor = category?.colorHex ?: "#9E9E9E",
                    categoryIcon = category?.iconKey ?: "more_horiz",
                    amount = split.amount,
                    note = split.note
                )
            }
        )
    }

    private fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
        return try {
            val colorInt = android.graphics.Color.parseColor(hex)
            androidx.compose.ui.graphics.Color(colorInt)
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color.Gray
        }
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
        transactionDao.clearAllSplits()
    }

    suspend fun stopRecurringSeries(parentRecurringId: String) {
        database.withTransaction {
            // This safely removes the recurrence icon/status from the parent 
            // AND all historical child entries at the same time.
            transactionDao.removeRecurrenceFromSeries(parentRecurringId)
        }
    }

    suspend fun updateRecurringSeriesFromOccurrence(
        parentRecurringId: String,
        sourceTransaction: TransactionEntity,
        sourceSplits: List<TransactionSplitEntity>,
        recurringInterval: Int?,
        recurringUnit: RecurringUnit?,
        recurringEndDate: Long?,
        notifyForRecurringEntries: Boolean
    ) {
        database.withTransaction {
            val parentWithSplits = transactionDao.getTransactionByIdInternal(parentRecurringId) ?: return@withTransaction
            val parent = parentWithSplits.transaction
            if (!parent.isRecurring) return@withTransaction

            val now = System.currentTimeMillis()
            val updatedParent = parent.copy(
                accountId = sourceTransaction.accountId,
                payee = sourceTransaction.payee,
                note = sourceTransaction.note,
                totalAmount = sourceTransaction.totalAmount,
                type = sourceTransaction.type,
                toAccountId = sourceTransaction.toAccountId,
                modifiedAt = now,
                syncStatus = SyncStatus.DIRTY,
                isRecurring = true,
                recurringInterval = recurringInterval,
                recurringUnit = recurringUnit,
                recurringEndDate = recurringEndDate,
                notifyForRecurringEntries = notifyForRecurringEntries,
                parentRecurringId = null
            )

            val updatedParentSplits = if (updatedParent.type == TransactionType.TRANSFER) {
                emptyList()
            } else {
                sourceSplits.map { split ->
                    split.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        transactionId = parentRecurringId
                    )
                }
            }

            updateTransaction(parent, updatedParent, updatedParentSplits)
        }
    }
}
