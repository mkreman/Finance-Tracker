package com.moneytracker.app.data.local.database.dao

import androidx.room.*
import com.moneytracker.app.data.local.database.entities.AccountEntity
import com.moneytracker.app.data.local.database.entities.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllAccountsIncludingInactive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAllAccountsForBackup(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 AND isActive = 0 ORDER BY name ASC")
    fun getInactiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE type = :type AND isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getAccountsByType(type: AccountType): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE isDeleted = 0")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type = :type AND isDeleted = 0")
    fun getTotalBalanceByType(type: AccountType): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(t.totalAmount), 0.0) FROM transactions t
        WHERE t.type = 'INCOME' AND t.isDeleted = 0
    """)
    fun getTotalIncomeAllTime(): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(t.totalAmount), 0.0) FROM transactions t
        WHERE t.type = 'EXPENSE' AND t.isDeleted = 0
    """)
    fun getTotalExpenseAllTime(): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(t.totalAmount), 0.0) FROM transactions t
        WHERE t.type = 'INCOME' AND t.accountId = :accountId AND t.isDeleted = 0
    """)
    fun getAccountIncome(accountId: String): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(t.totalAmount), 0.0) FROM transactions t
        WHERE t.type = 'EXPENSE' AND t.accountId = :accountId AND t.isDeleted = 0
    """)
    fun getAccountExpense(accountId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalance = currentBalance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, amount: Double)

    @Query("UPDATE accounts SET isDeleted = 1, modifiedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET isActive = :active, modifiedAt = :timestamp WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM accounts WHERE syncStatus = 'DIRTY' OR syncStatus = 'PENDING'")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>

    @Query("UPDATE accounts SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()
}
