package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.AccountDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import com.moneytracker.app.data.local.database.entities.AccountEntity
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getAllAccountsOnce(): List<Account> =
        accountDao.getAllAccounts().first().map { it.toDomain() }

    fun getAccountsByType(type: AccountType): Flow<List<Account>> =
        accountDao.getAccountsByType(type).map { list ->
            list.map { it.toDomain() }
        }

    fun getTotalBalance(): Flow<Double> =
        accountDao.getTotalBalance().map { it ?: 0.0 }

    fun getTotalBalanceByType(type: AccountType): Flow<Double> =
        accountDao.getTotalBalanceByType(type).map { it ?: 0.0 }

    fun getTotalIncomeAllTime(): Flow<Double> =
        accountDao.getTotalIncomeAllTime().map { it ?: 0.0 }

    fun getTotalExpenseAllTime(): Flow<Double> =
        accountDao.getTotalExpenseAllTime().map { it ?: 0.0 }

    fun getAccountIncome(accountId: String): Flow<Double> =
        accountDao.getAccountIncome(accountId).map { it ?: 0.0 }

    fun getAccountExpense(accountId: String): Flow<Double> =
        accountDao.getAccountExpense(accountId).map { it ?: 0.0 }

    suspend fun getAccountById(id: String): Account? =
        accountDao.getAccountById(id)?.toDomain()

    suspend fun saveAccount(account: Account) {
        accountDao.insertAccount(
            AccountEntity(
                id = account.id,
                name = account.name,
                type = account.type,
                initialBalance = account.initialBalance,
                currentBalance = account.currentBalance,
                currency = account.currency,
                colorHex = account.colorHex,
                iconKey = account.iconKey,
                modifiedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.DIRTY
            )
        )
    }

    suspend fun updateBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }

    suspend fun updateAccount(account: Account) {
        val existing = accountDao.getAccountById(account.id) ?: return
        val balanceDiff = account.initialBalance - existing.initialBalance
        accountDao.updateAccount(
            existing.copy(
                name = account.name,
                type = account.type,
                initialBalance = account.initialBalance,
                currentBalance = existing.currentBalance + balanceDiff,
                colorHex = account.colorHex,
                iconKey = account.iconKey,
                modifiedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.DIRTY
            )
        )
    }

    suspend fun deleteAccount(id: String) {
        accountDao.softDelete(id)
    }

    fun getInactiveAccounts(): Flow<List<Account>> =
        accountDao.getInactiveAccounts().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun deactivateAccount(id: String) {
        accountDao.setActive(id, false)
    }

    suspend fun activateAccount(id: String) {
        accountDao.setActive(id, true)
    }

    suspend fun hardDeleteAccount(id: String) {
        // Reverse balance effects on OTHER accounts for transfer transactions
        // Transfers FROM the deleted account credited the destination — reverse that
        transactionDao.getTransfersFromAccount(id).forEach { txn ->
            txn.toAccountId?.let { toId ->
                accountDao.updateBalance(toId, -txn.totalAmount)
            }
        }
        // Transfers TO the deleted account debited the source — reverse that
        transactionDao.getTransfersToAccount(id).forEach { txn ->
            accountDao.updateBalance(txn.accountId, txn.totalAmount)
        }

        transactionDao.deleteAllByAccount(id)
        accountDao.hardDelete(id)
    }

    suspend fun clearAllAccounts() {
        accountDao.clearAllAccounts()
    }

    private fun AccountEntity.toDomain() = Account(
        id = id,
        name = name,
        type = type,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        currency = currency,
        colorHex = colorHex,
        iconKey = iconKey,
        isActive = isActive
    )
}
