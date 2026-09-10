package com.moneytracker.app.data.local.database.dao

import androidx.room.*
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.data.local.database.entities.InvestmentTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Transaction
    suspend fun upsertInvestment(investment: InvestmentEntity) {
        val rowId = insertInvestment(investment)
        if (rowId == -1L) {
            updateInvestment(investment)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(txn: InvestmentTransactionEntity)

    @Query("SELECT * FROM investments WHERE isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol)) AND isDeleted = 0")
    suspend fun getBySymbol(symbol: String): InvestmentEntity?

    @Query("SELECT * FROM investments WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol))")
    suspend fun getInvestmentBySymbolAny(symbol: String): InvestmentEntity?

    @Query("SELECT * FROM investments WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol)) AND isDeleted = 0")
    fun observeBySymbol(symbol: String): Flow<InvestmentEntity?>

    @Query("SELECT SUM(totalInvestedAmount) FROM investments WHERE isDeleted = 0")
    fun getTotalInvested(): Flow<Double?>

    @Query("SELECT SUM(totalUnits) FROM investments WHERE isDeleted = 0")
    fun getTotalHoldings(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM investments WHERE isDeleted = 0")
    fun getHoldingsCount(): Flow<Int>

    @Query("SELECT * FROM investment_transactions WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol)) ORDER BY date DESC")
    fun getTransactionsForSymbol(symbol: String): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol)) ORDER BY date DESC")
    suspend fun getTransactionsListForSymbol(symbol: String): List<InvestmentTransactionEntity>

    @Query("SELECT * FROM investment_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM investment_transactions 
        WHERE transactionType = 'INVEST' 
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalInvestedForPeriod(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT * 
        FROM investment_transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsForPeriod(startDate: Long, endDate: Long): Flow<List<InvestmentTransactionEntity>>

    @Query("UPDATE investments SET isDeleted = 1, modifiedAt = :timestamp WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol))")
    suspend fun softDelete(symbol: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM investment_transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): InvestmentTransactionEntity?

    @Query("DELETE FROM investment_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Update
    suspend fun updateTransaction(txn: InvestmentTransactionEntity)

    @Query("""
        UPDATE investments 
        SET currentPrice = :currentPrice, 
            dayChangePercent = :dayChangePercent, 
            priceUpdatedAt = :timestamp 
        WHERE UPPER(TRIM(symbol)) = UPPER(TRIM(:symbol))
    """)
    suspend fun updateStockPrice(
        symbol: String, 
        currentPrice: Double, 
        dayChangePercent: Double?, 
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM investments WHERE isDeleted = 0 AND totalUnits > 0")
    suspend fun getActiveHoldings(): List<InvestmentEntity>

    @Query("DELETE FROM investments")
    suspend fun clearAllInvestments()

    @Query("DELETE FROM investment_transactions")
    suspend fun clearAllTransactions()
}
