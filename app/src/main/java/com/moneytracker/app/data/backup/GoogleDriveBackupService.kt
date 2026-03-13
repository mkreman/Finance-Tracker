package com.moneytracker.app.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.database.entities.RecurringUnit
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data object Success : BackupResult()
    data object NotSignedIn : BackupResult()
    data object NoBackupFound : BackupResult()
    data class Error(val message: String) : BackupResult()
}

data class CloudBackupInfo(
    val fileId: String,
    val name: String,
    val modifiedTimeMs: Long
)

@Singleton
class GoogleDriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val userPreferences: UserPreferences
) {

    companion object {
        private const val MAX_RECEIPT_FILE_BYTES = 2 * 1024 * 1024
        private const val MAX_RECEIPT_TOTAL_BYTES = 15 * 1024 * 1024
        private const val RECEIPT_JPEG_QUALITY = 80
    }

    suspend fun hasAnyLocalData(): Boolean = withContext(Dispatchers.IO) {
        accountRepository.getAllAccountsIncludingInactiveOnce().isNotEmpty() ||
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

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "moneytracker_backup_$timestamp.json"
                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                drive.files().create(fileMetadata, mediaContent).setFields("id").execute()

                // Keep only the 5 most recent backups; delete older ones
                val allBackups = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name contains 'moneytracker_backup_' and trashed=false")
                    .setOrderBy("modifiedTime desc")
                    .setFields("files(id,name,modifiedTime)")
                    .execute()
                    .files.orEmpty()

                allBackups.drop(5).forEach { old ->
                    runCatching { drive.files().delete(old.id).execute() }
                }

                userPreferences.setLastCloudBackupTime(System.currentTimeMillis())
                BackupResult.Success
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "Unknown Drive backup error")
            }
        }
    }

    suspend fun listAvailableBackups(): Result<List<CloudBackupInfo>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: error("Not signed in")
                val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
                if (!account.grantedScopes.contains(requiredScope)) error("Drive scope not granted")

                val drive = createDriveService(account.account?.name ?: error("No account name"))

                drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name contains 'moneytracker_backup_' and trashed=false")
                    .setOrderBy("modifiedTime desc")
                    .setPageSize(5)
                    .setFields("files(id,name,modifiedTime)")
                    .execute()
                    .files.orEmpty()
                    .map { f ->
                        CloudBackupInfo(
                            fileId = f.id,
                            name = f.name,
                            modifiedTimeMs = f.modifiedTime?.value ?: 0L
                        )
                    }
            }
        }
    }

    suspend fun restoreBackupById(fileId: String): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext BackupResult.NotSignedIn
                val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
                if (!account.grantedScopes.contains(requiredScope)) return@withContext BackupResult.NotSignedIn

                val drive = createDriveService(account.account?.name ?: return@withContext BackupResult.NotSignedIn)

                val jsonString = drive.files().get(fileId)
                    .executeMediaAsInputStream()
                    .use { it.readBytes().toString(Charsets.UTF_8) }

                importFromJson(jsonString)
                BackupResult.Success
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "Unknown Drive restore error")
            }
        }
    }

    suspend fun restoreLatestBackupFromCloud(): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val backups = listAvailableBackups().getOrNull()
                if (backups.isNullOrEmpty()) return@withContext BackupResult.NoBackupFound
                restoreBackupById(backups.first().fileId)
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
        fun decodeReceiptUris(serialized: String?): List<String> {
            return serialized
                ?.split("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }

        fun encodeReceiptBlob(receiptUri: String): JSONObject? {
            val uriObj = runCatching { Uri.parse(receiptUri) }.getOrNull() ?: return null
            val rawBytes = runCatching {
                context.contentResolver.openInputStream(uriObj)?.use { it.readBytes() }
            }.getOrNull() ?: return null
            if (rawBytes.isEmpty()) return null

            val sourceMimeType = context.contentResolver.getType(uriObj) ?: "application/octet-stream"
            val (bytes, mimeType) = if (sourceMimeType.startsWith("image/")) {
                val bitmap = runCatching {
                    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                }.getOrNull()

                if (bitmap != null) {
                    val output = ByteArrayOutputStream()
                    val compressed = runCatching {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, RECEIPT_JPEG_QUALITY, output)
                    }.getOrDefault(false)
                    val jpegBytes = if (compressed) output.toByteArray() else ByteArray(0)
                    bitmap.recycle()

                    if (jpegBytes.isNotEmpty() && jpegBytes.size < rawBytes.size) {
                        jpegBytes to "image/jpeg"
                    } else {
                        rawBytes to sourceMimeType
                    }
                } else {
                    rawBytes to sourceMimeType
                }
            } else {
                rawBytes to sourceMimeType
            }

            if (bytes.size > MAX_RECEIPT_FILE_BYTES) return null

            return JSONObject().apply {
                put("mimeType", mimeType)
                put("dataBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                put("sizeBytes", bytes.size)
            }
        }

        val transactions = transactionRepository.getAllTransactionsRaw()
        // Use ALL accounts (including soft-deleted) so transaction account names resolve correctly
        val allAccounts = accountRepository.getAllAccountsForBackup()
        val budgets = budgetRepository.getAllBudgets()
        val categories = categoryRepository.getAllCategories().firstOrNull().orEmpty()

        val accountMap = allAccounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        val rootObj = JSONObject()

        val accountsArray = JSONArray()
        allAccounts.forEach { acc ->
            accountsArray.put(
                JSONObject().apply {
                    put("name", acc.name)
                    put("type", acc.type.name)
                    put("customTypeName", acc.customTypeName ?: "")
                    put("initialBalance", acc.initialBalance)
                    put("currentBalance", acc.currentBalance)
                    put("currency", acc.currency)
                    put("colorHex", acc.colorHex)
                    put("iconKey", acc.iconKey)
                    put("isActive", acc.isActive)
                    put("isDeleted", acc.isDeleted)
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
        val receiptFilesObj = JSONObject()
        var totalReceiptBytes = 0
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
                    put("isRecurring", txn.isRecurring)
                    put("recurringInterval", txn.recurringInterval)
                    put("recurringUnit", txn.recurringUnit?.name ?: "")
                    put("recurringEndDate", txn.recurringEndDate)
                    put("parentRecurringId", txn.parentRecurringId ?: "")
                    put("notifyForRecurringEntries", txn.notifyForRecurringEntries)
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

                    // Backup actual attachment bytes so restores on another device can recreate files.
                    decodeReceiptUris(txn.receiptUri).forEach { receiptUri ->
                        if (!receiptFilesObj.has(receiptUri)) {
                            encodeReceiptBlob(receiptUri)?.let { receiptBlob ->
                                val sizeBytes = receiptBlob.optInt("sizeBytes", 0)
                                if (sizeBytes > 0 && totalReceiptBytes + sizeBytes <= MAX_RECEIPT_TOTAL_BYTES) {
                                    receiptFilesObj.put(receiptUri, receiptBlob)
                                    totalReceiptBytes += sizeBytes
                                }
                            }
                        }
                    }
                }
            )
        }
        rootObj.put("transactions", txnsArray)
        rootObj.put("receiptFiles", receiptFilesObj)

        return rootObj.toString(4)
    }

    private suspend fun importFromJson(jsonString: String) {
        val rootObj = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Invalid cloud backup format")
        }

        fun normalizeKey(value: String): String =
            value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

        fun categoryKey(name: String, type: TransactionType): Pair<String, TransactionType> =
            normalizeKey(name) to type

        fun restoreReceiptFile(dataBase64: String, mimeType: String): String? {
            val bytes = runCatching { Base64.decode(dataBase64, Base64.DEFAULT) }.getOrNull() ?: return null
            if (bytes.isEmpty()) return null

            val extension = when {
                mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                mimeType.contains("heic", ignoreCase = true) || mimeType.contains("heif", ignoreCase = true) -> "heic"
                mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                else -> "bin"
            }

            val directory = File(context.filesDir, "restored_receipts").apply { mkdirs() }
            val file = File(directory, "receipt_${UUID.randomUUID()}.$extension")
            return runCatching {
                file.writeBytes(bytes)
                Uri.fromFile(file).toString()
            }.getOrNull()
        }

        fun remapReceiptUris(serialized: String?, restoredMap: Map<String, String>): String? {
            val uris = serialized
                ?.split("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            if (uris.isEmpty()) return null

            val remapped = uris.map { original -> restoredMap[original] ?: original }
            return remapped.joinToString("\n").ifBlank { null }
        }

        val restoredReceiptUriMap = mutableMapOf<String, String>()
        val receiptFilesObj = rootObj.optJSONObject("receiptFiles")
        if (receiptFilesObj != null) {
            val keys = receiptFilesObj.keys()
            while (keys.hasNext()) {
                val originalUri = keys.next()
                val blobObj = receiptFilesObj.optJSONObject(originalUri) ?: continue
                val dataBase64 = blobObj.optString("dataBase64", "")
                if (dataBase64.isBlank()) continue
                val mimeType = blobObj.optString("mimeType", "application/octet-stream")
                restoreReceiptFile(dataBase64, mimeType)?.let { localUri ->
                    restoredReceiptUriMap[originalUri] = localUri
                }
            }
        }

        // Use ALL accounts (including deleted) to avoid duplicate creation
        val accounts = accountRepository.getAllAccountsForBackup()
        val accountByName = accounts.associateBy { normalizeKey(it.name) }.toMutableMap()

        val allCategories = mutableMapOf<Pair<String, TransactionType>, Category>()
        categoryRepository.getAllCategories().firstOrNull().orEmpty().forEach { category ->
            allCategories[categoryKey(category.name, category.type)] = category
        }

        val accountsArray = rootObj.optJSONArray("accounts")
        if (accountsArray != null) {
            for (i in 0 until accountsArray.length()) {
                val accObj = accountsArray.getJSONObject(i)
                val name = accObj.getString("name").trim()
                val importedInitial = accObj.getDouble("initialBalance")
                val startingBalance = importedInitial
                val typeStr = accObj.getString("type")
                val customTypeName = accObj.optString("customTypeName", "").ifBlank { null }
                val currency = accObj.optString("currency", "INR")
                val colorHex = accObj.optString("colorHex", "#2196F3")
                val iconKey = accObj.optString("iconKey", "bank")
                val isActive = accObj.optBoolean("isActive", true)
                val isDeleted = accObj.optBoolean("isDeleted", false)

                val existingAccount = accountByName[normalizeKey(name)]
                if (existingAccount == null) {
                    val newAccount = Account(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = AccountType.valueOf(typeStr),
                        customTypeName = customTypeName,
                        initialBalance = importedInitial,
                        currentBalance = startingBalance,
                        currency = currency,
                        colorHex = colorHex,
                        iconKey = iconKey,
                        isActive = isActive,
                        isDeleted = isDeleted
                    )
                    accountRepository.saveAccount(newAccount)
                    accountByName[normalizeKey(name)] = newAccount
                } else {
                    val updated = existingAccount.copy(
                        initialBalance = importedInitial,
                        currentBalance = startingBalance,
                        customTypeName = customTypeName,
                        currency = currency,
                        colorHex = colorHex,
                        iconKey = iconKey,
                        isActive = isActive,
                        isDeleted = isDeleted
                    )
                    accountRepository.saveAccount(updated)
                    accountByName[normalizeKey(name)] = updated
                }
            }
        }

        val budgetsArray = rootObj.optJSONArray("budgets")
        if (budgetsArray != null) {
            for (i in 0 until budgetsArray.length()) {
                val budgetObj = budgetsArray.getJSONObject(i)
                val categoryName = budgetObj.getString("categoryName").trim()
                val budgetCategoryKey = categoryKey(categoryName, TransactionType.EXPENSE)
                var category = allCategories[budgetCategoryKey]

                if (category == null) {
                    category = Category(
                        id = UUID.randomUUID().toString(),
                        name = categoryName,
                        type = TransactionType.EXPENSE,
                        colorHex = "#FF5722",
                        iconKey = "more_horiz"
                    )
                    categoryRepository.saveCategory(category)
                    allCategories[budgetCategoryKey] = category
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
                val fromAccountName = txnObj.getString("fromAccountName").trim()

                var fromAccount = accountByName[normalizeKey(fromAccountName)]
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
                    accountByName[normalizeKey(fromAccountName)] = fromAccount
                }

                var toAccountId: String? = null
                if (type == TransactionType.TRANSFER) {
                    val toAccountName = txnObj.optString("toAccountName", "Unknown").trim()
                    var toAccount = accountByName[normalizeKey(toAccountName)]
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
                        accountByName[normalizeKey(toAccountName)] = toAccount
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
                    receiptUri = remapReceiptUris(
                        txnObj.optString("receiptUri", "").ifBlank { null },
                        restoredReceiptUriMap
                    ),
                    date = txnObj.getLong("date"),
                    totalAmount = txnObj.getDouble("totalAmount"),
                    type = type,
                    isRecurring = txnObj.optBoolean("isRecurring", false),
                    recurringInterval = if (txnObj.has("recurringInterval") && !txnObj.isNull("recurringInterval")) {
                        txnObj.optInt("recurringInterval").takeIf { it > 0 }
                    } else {
                        null
                    },
                    recurringUnit = txnObj.optString("recurringUnit", "")
                        .ifBlank { null }
                        ?.let { value -> runCatching { RecurringUnit.valueOf(value) }.getOrNull() },
                    recurringEndDate = if (txnObj.has("recurringEndDate") && !txnObj.isNull("recurringEndDate")) {
                        txnObj.optLong("recurringEndDate")
                    } else {
                        null
                    },
                    parentRecurringId = txnObj.optString("parentRecurringId", "").ifBlank { null },
                    notifyForRecurringEntries = txnObj.optBoolean("notifyForRecurringEntries", true),
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.DIRTY
                )

                val splitsEntities = mutableListOf<TransactionSplitEntity>()
                val splitsArray = txnObj.optJSONArray("splits")
                if (splitsArray != null) {
                    for (j in 0 until splitsArray.length()) {
                        val splitObj = splitsArray.getJSONObject(j)
                        val categoryName = splitObj.getString("categoryName").trim()
                        val splitType = if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
                        val splitCategoryKey = categoryKey(categoryName, splitType)

                        var category = allCategories[splitCategoryKey]
                            ?: allCategories[categoryKey(categoryName, TransactionType.EXPENSE)]
                        if (category == null) {
                            category = Category(
                                id = UUID.randomUUID().toString(),
                                name = categoryName,
                                type = splitType,
                                colorHex = if (splitType == TransactionType.INCOME) "#4CAF50" else "#FF5722",
                                iconKey = "more_horiz"
                            )
                            categoryRepository.saveCategory(category)
                            allCategories[splitCategoryKey] = category
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
