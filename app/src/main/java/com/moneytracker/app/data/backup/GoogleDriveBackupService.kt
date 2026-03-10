package com.moneytracker.app.data.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.domain.model.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data object Success : BackupResult()
    data object NotSignedIn : BackupResult()
    data object NoBackupFound : BackupResult()
    data class Error(val message: String) : BackupResult()
}

@Singleton
class GoogleDriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val userPreferences: UserPreferences
) {

    suspend fun hasAnyLocalData(): Boolean = withContext(Dispatchers.IO) {
        accountRepository.getAllAccountsOnce().isNotEmpty() ||
            budgetRepository.getAllBudgets().isNotEmpty() ||
            transactionRepository.getAllTransactionsRaw().isNotEmpty()
    }

    suspend fun backupNow(): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext BackupResult.NotSignedIn
                val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
                if (!account.grantedScopes.contains(requiredScope)) return@withContext BackupResult.NotSignedIn

                val drive = createDriveService(account.account?.name ?: return@withContext BackupResult.NotSignedIn)
                val json = buildBackupJson()
                val mediaContent = ByteArrayContent.fromString("application/json", json)

                val existing = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name='moneytracker_backup.json' and trashed=false")
                    .setFields("files(id,name)")
                    .execute()
                    .files
                    ?.firstOrNull()

                if (existing != null) {
                    drive.files().update(existing.id, null, mediaContent).execute()
                } else {
                    val fileMetadata = File().apply {
                        name = "moneytracker_backup.json"
                        parents = listOf("appDataFolder")
                    }
                    drive.files().create(fileMetadata, mediaContent).setFields("id").execute()
                }

                userPreferences.setLastCloudBackupTime(System.currentTimeMillis())
                BackupResult.Success
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "Unknown Drive backup error")
            }
        }
    }

    suspend fun restoreLatestBackupFromCloud(): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext BackupResult.NotSignedIn
                val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
                if (!account.grantedScopes.contains(requiredScope)) return@withContext BackupResult.NotSignedIn

                val drive = createDriveService(account.account?.name ?: return@withContext BackupResult.NotSignedIn)

                val latestBackup = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name='moneytracker_backup.json' and trashed=false")
                    .setOrderBy("modifiedTime desc")
                    .setPageSize(1)
                    .setFields("files(id,name,modifiedTime)")
                    .execute()
                    .files
                    ?.firstOrNull()
                    ?: return@withContext BackupResult.NoBackupFound

                val jsonString = drive.files().get(latestBackup.id)
                    .executeMediaAsInputStream()
                    .use { it.readBytes().toString(Charsets.UTF_8) }

                importFromJson(jsonString)
                BackupResult.Success
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "Unknown Drive restore error")
            }
        }
    }

    private fun createDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA)).apply {
            selectedAccountName = accountName
        }
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        return Drive.Builder(transport, GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("MoneyTracker")
            .build()
    }

    private suspend fun buildBackupJson(): String {
        val transactions = transactionRepository.getAllTransactionsRaw()
        val accounts = accountRepository.getAllAccountsOnce()
        val budgets = budgetRepository.getAllBudgets()
        val categories = categoryRepository.getAllCategories().firstOrNull().orEmpty()

        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        val rootObj = JSONObject()

        val accountsArray = JSONArray()
        accounts.forEach { acc ->
            accountsArray.put(
                JSONObject().apply {
                    put("name", acc.name)
                    put("type", acc.type.name)
                    put("initialBalance", acc.initialBalance)
                    put("currentBalance", acc.currentBalance)
                    put("colorHex", acc.colorHex)
                    put("iconKey", acc.iconKey)
                }
            )
        }
        rootObj.put("accounts", accountsArray)

        val budgetsArray = JSONArray()
        budgets.forEach { budget ->
            budgetsArray.put(
                JSONObject().apply {
                    put("categoryName", categoryMap[budget.categoryId]?.name ?: "Unknown")
                    put("limitAmount", budget.limitAmount)
                    put("month", budget.month)
                    put("year", budget.year)
                }
            )
        }
        rootObj.put("budgets", budgetsArray)

        val txnsArray = JSONArray()
        transactions.forEach { txnWithSplits ->
            val txn = txnWithSplits.transaction
            txnsArray.put(
                JSONObject().apply {
                    put("date", txn.date)
                    put("type", txn.type.name)
                    put("totalAmount", txn.totalAmount)
                    put("note", txn.note ?: "")
                    put("payee", txn.payee)
                    put("receiptUri", txn.receiptUri ?: "")
                    put("fromAccountName", accountMap[txn.accountId]?.name ?: "Unknown")
                    if (txn.type == TransactionType.TRANSFER) {
                        put("toAccountName", accountMap[txn.toAccountId]?.name ?: "Unknown")
                    }

                    val splitsArray = JSONArray()
                    txnWithSplits.splits.forEach { split ->
                        splitsArray.put(
                            JSONObject().apply {
                                put("categoryName", categoryMap[split.categoryId]?.name ?: "Unknown")
                                put("amount", split.amount)
                            }
                        )
                    }
                    put("splits", splitsArray)
                }
            )
        }
        rootObj.put("transactions", txnsArray)

        return rootObj.toString(4)
    }

    private suspend fun importFromJson(jsonString: String) {
        val rootObj = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Invalid cloud backup format")
        }

        val accounts = accountRepository.getAllAccountsOnce()
        val accountByName = accounts.associateBy { it.name }.toMutableMap()

        val allCategories = mutableMapOf<String, Category>()
        categoryRepository.getAllCategories().firstOrNull().orEmpty().forEach { category ->
            allCategories[category.name] = category
        }

        val accountsArray = rootObj.optJSONArray("accounts")
        if (accountsArray != null) {
            for (i in 0 until accountsArray.length()) {
                val accObj = accountsArray.getJSONObject(i)
                val name = accObj.getString("name")
                val importedInitial = accObj.getDouble("initialBalance")
                val startingBalance = importedInitial
                val typeStr = accObj.getString("type")
                val colorHex = accObj.optString("colorHex", "#2196F3")
                val iconKey = accObj.optString("iconKey", "bank")

                val existingAccount = accountByName[name]
                if (existingAccount == null) {
                    val newAccount = Account(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = AccountType.valueOf(typeStr),
                        initialBalance = importedInitial,
                        currentBalance = startingBalance,
                        colorHex = colorHex,
                        iconKey = iconKey
                    )
                    accountRepository.saveAccount(newAccount)
                    accountByName[name] = newAccount
                } else {
                    val updated = existingAccount.copy(
                        initialBalance = importedInitial,
                        currentBalance = startingBalance,
                        colorHex = colorHex,
                        iconKey = iconKey
                    )
                    accountRepository.saveAccount(updated)
                    accountByName[name] = updated
                }
            }
        }

        val budgetsArray = rootObj.optJSONArray("budgets")
        if (budgetsArray != null) {
            for (i in 0 until budgetsArray.length()) {
                val budgetObj = budgetsArray.getJSONObject(i)
                val categoryName = budgetObj.getString("categoryName")
                var category = allCategories[categoryName]

                if (category == null) {
                    category = Category(
                        id = UUID.randomUUID().toString(),
                        name = categoryName,
                        type = TransactionType.EXPENSE,
                        colorHex = "#FF5722",
                        iconKey = "more_horiz"
                    )
                    categoryRepository.saveCategory(category)
                    allCategories[categoryName] = category
                }

                budgetRepository.saveBudget(
                    categoryId = category.id,
                    limitAmount = budgetObj.getDouble("limitAmount"),
                    month = budgetObj.getInt("month"),
                    year = budgetObj.getInt("year")
                )
            }
        }

        val txnsArray = rootObj.optJSONArray("transactions")
        if (txnsArray != null) {
            for (i in 0 until txnsArray.length()) {
                val txnObj = txnsArray.getJSONObject(i)
                val type = TransactionType.valueOf(txnObj.getString("type"))
                val fromAccountName = txnObj.getString("fromAccountName")

                var fromAccount = accountByName[fromAccountName]
                if (fromAccount == null) {
                    fromAccount = Account(
                        id = UUID.randomUUID().toString(),
                        name = fromAccountName,
                        type = AccountType.BANK,
                        initialBalance = 0.0,
                        currentBalance = 0.0,
                        colorHex = "#2196F3",
                        iconKey = "bank"
                    )
                    accountRepository.saveAccount(fromAccount)
                    accountByName[fromAccountName] = fromAccount
                }

                var toAccountId: String? = null
                if (type == TransactionType.TRANSFER) {
                    val toAccountName = txnObj.optString("toAccountName", "Unknown")
                    var toAccount = accountByName[toAccountName]
                    if (toAccount == null) {
                        toAccount = Account(
                            id = UUID.randomUUID().toString(),
                            name = toAccountName,
                            type = AccountType.BANK,
                            initialBalance = 0.0,
                            currentBalance = 0.0,
                            colorHex = "#4CAF50",
                            iconKey = "bank"
                        )
                        accountRepository.saveAccount(toAccount)
                        accountByName[toAccountName] = toAccount
                    }
                    toAccountId = toAccount.id
                }

                val transactionId = UUID.randomUUID().toString()
                val transaction = TransactionEntity(
                    id = transactionId,
                    accountId = fromAccount.id,
                    toAccountId = toAccountId,
                    payee = txnObj.optString("payee", "Transaction"),
                    note = txnObj.optString("note", "").ifBlank { null },
                    receiptUri = txnObj.optString("receiptUri", "").ifBlank { null },
                    date = txnObj.getLong("date"),
                    totalAmount = txnObj.getDouble("totalAmount"),
                    type = type,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.DIRTY
                )

                val splitsEntities = mutableListOf<TransactionSplitEntity>()
                val splitsArray = txnObj.optJSONArray("splits")
                if (splitsArray != null) {
                    for (j in 0 until splitsArray.length()) {
                        val splitObj = splitsArray.getJSONObject(j)
                        val categoryName = splitObj.getString("categoryName")

                        var category = allCategories[categoryName]
                        if (category == null) {
                            category = Category(
                                id = UUID.randomUUID().toString(),
                                name = categoryName,
                                type = type,
                                colorHex = if (type == TransactionType.INCOME) "#4CAF50" else "#FF5722",
                                iconKey = "more_horiz"
                            )
                            categoryRepository.saveCategory(category)
                            allCategories[categoryName] = category
                        }

                        splitsEntities.add(
                            TransactionSplitEntity(
                                id = UUID.randomUUID().toString(),
                                transactionId = transactionId,
                                categoryId = category.id,
                                amount = splitObj.getDouble("amount"),
                                note = null
                            )
                        )
                    }
                }

                transactionRepository.saveTransaction(transaction, splitsEntities)
            }
        }
    }
}
