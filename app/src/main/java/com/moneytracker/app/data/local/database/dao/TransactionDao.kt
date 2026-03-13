package com.moneytracker.app.data.local.database.dao

import androidx.room.*
import com.moneytracker.app.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ---- Basic CRUD ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplitEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteSplitsByTransactionId(transactionId: String)

    @Query("UPDATE transactions SET isDeleted = 1, modifiedAt = :timestamp, syncStatus = 'DIRTY' WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, timestamp: Long = System.currentTimeMillis())

    @androidx.room.Transaction
    suspend fun saveFullTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ) {
        insertTransaction(transaction)
        if (splits.isNotEmpty()) {
            insertSplits(splits)
        }
    }

    @androidx.room.Transaction
    suspend fun updateFullTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ) {
        insertTransaction(transaction)
        deleteSplitsByTransactionId(transaction.id)
        if (splits.isNotEmpty()) {
            insertSplits(splits)
        }
    }

    // ---- Queries ----

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        AND isDeleted = 0 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (accountId = :accountId OR toAccountId = :accountId)
        AND isDeleted = 0 
        ORDER BY date DESC
    """)
    fun getTransactionsByAccountIncludingTransfers(accountId: String): Flow<List<TransactionWithSplits>>

    // FIX: Added period-specific query for accounts
    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (accountId = :accountId OR toAccountId = :accountId)
        AND isDeleted = 0 
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getTransactionsByAccountIncludingTransfersForPeriod(
        accountId: String, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT DISTINCT t.* FROM transactions t
        JOIN transaction_splits s ON s.transactionId = t.id
        WHERE s.categoryId = :categoryId
        AND t.type = :type
        AND t.isDeleted = 0
        ORDER BY t.date DESC
    """)
    fun getTransactionsByCategory(categoryId: String, type: String): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT DISTINCT t.* FROM transactions t
        JOIN transaction_splits s ON s.transactionId = t.id
        WHERE s.categoryId = :categoryId
        AND t.type = :type
        AND t.isDeleted = 0
        AND t.date BETWEEN :startDate AND :endDate
        ORDER BY t.date DESC
    """)
    fun getTransactionsByCategoryForPeriod(
        categoryId: String,
        type: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: String): TransactionWithSplits?

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionByIdInternal(id: String): TransactionWithSplits?

    // ---- Aggregation Queries ----

    @Query("""
        SELECT SUM(t.totalAmount)
        FROM transactions t
        WHERE t.type = 'EXPENSE'
        AND t.date BETWEEN :startDate AND :endDate
        AND t.isDeleted = 0
    """)
    fun getTotalExpenseForPeriod(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT SUM(t.totalAmount)
        FROM transactions t
        WHERE t.type = 'INCOME'
        AND t.date BETWEEN :startDate AND :endDate
        AND t.isDeleted = 0
    """)
    fun getTotalIncomeForPeriod(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT SUM(t.totalAmount)
        FROM transactions t
        WHERE t.type = 'TRANSFER'
        AND t.date BETWEEN :startDate AND :endDate
        AND t.isDeleted = 0
    """)
    fun getTotalTransferForPeriod(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName, 
            c.colorHex as colorHex,
            c.iconKey as iconKey,
            SUM(s.amount) as total 
        FROM transaction_splits s
        JOIN categories c ON s.categoryId = c.id
        JOIN transactions t ON s.transactionId = t.id
        WHERE t.date BETWEEN :startDate AND :endDate
        AND t.type = 'EXPENSE'
        AND t.isDeleted = 0
        GROUP BY c.id
        ORDER BY total DESC
    """)
    fun getCategorySpendingForPeriod(startDate: Long, endDate: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName, 
            c.colorHex as colorHex,
            c.iconKey as iconKey,
            SUM(s.amount) as total 
        FROM transaction_splits s
        JOIN categories c ON s.categoryId = c.id
        JOIN transactions t ON s.transactionId = t.id
        WHERE t.date BETWEEN :startDate AND :endDate
        AND t.type = 'INCOME'
        AND t.isDeleted = 0
        GROUP BY c.id
        ORDER BY total DESC
    """)
    fun getCategoryIncomeForPeriod(startDate: Long, endDate: Long): Flow<List<CategorySpending>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'TRANSFER'
        AND date BETWEEN :startDate AND :endDate
        AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getTransferTransactionsForPeriod(startDate: Long, endDate: Long): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'TRANSFER'
        AND toAccountId = :toAccountId
        AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getTransferTransactionsToAccount(toAccountId: String): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'TRANSFER'
        AND toAccountId IS NULL
        AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getTransferTransactionsToUnknownAccount(): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'TRANSFER'
        AND toAccountId = :toAccountId
        AND date BETWEEN :startDate AND :endDate
        AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getTransferTransactionsToAccountForPeriod(
        toAccountId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionWithSplits>>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'TRANSFER'
        AND toAccountId IS NULL
        AND date BETWEEN :startDate AND :endDate
        AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getTransferTransactionsToUnknownAccountForPeriod(
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionWithSplits>>

    // ---- Sync ----

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionWithSplits>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsIncludingDeletedOnce(): List<TransactionWithSplits>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteAllByAccount(accountId: String)

    @Query("SELECT * FROM transactions WHERE type = 'TRANSFER' AND accountId = :accountId AND isDeleted = 0")
    suspend fun getTransfersFromAccount(accountId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = 'TRANSFER' AND toAccountId = :accountId AND isDeleted = 0")
    suspend fun getTransfersToAccount(accountId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM transaction_splits")
    suspend fun clearAllSplits()

    @Query("""
        UPDATE transactions 
        SET isRecurring = 0, 
            recurringInterval = NULL, 
            recurringUnit = NULL, 
            recurringEndDate = NULL, 
            parentRecurringId = NULL,
            notifyForRecurringEntries = 0,
            syncStatus = 'DIRTY', 
            modifiedAt = :timestamp 
        WHERE id = :seriesId OR parentRecurringId = :seriesId
    """)
    suspend fun removeRecurrenceFromSeries(seriesId: String, timestamp: Long = System.currentTimeMillis())
}
