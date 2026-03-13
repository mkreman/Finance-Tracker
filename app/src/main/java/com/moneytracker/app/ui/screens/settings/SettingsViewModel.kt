package com.moneytracker.app.ui.screens.settings

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.backup.BackupResult
import com.moneytracker.app.data.backup.CloudBackupInfo
import com.moneytracker.app.data.backup.CloudBackupScheduler
import com.moneytracker.app.data.backup.GoogleDriveBackupService
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SettingsState(
    val exportMessage: String? = null,
    val importMessage: String? = null,
    val accounts: List<Account> = emptyList(),
    val defaultAccountId: String? = null,
    val currencyCode: String = "INR",
    val firstDayOfWeek: Int = java.util.Calendar.MONDAY,
    val biometricEnabled: Boolean = false,
    val passcodeEnabled: Boolean = false,
    val defaultNotifyForRecurringEntries: Boolean = true,
    val themeMode: Int = 0, // 0=system,1=light,2=dark
    val dailyReminderEnabled: Boolean = false,
    val budgetAlertsEnabled: Boolean = true,
    val autoCloudBackupEnabled: Boolean = false,
    val lastCloudBackupTime: Long = 0L,
    val cloudBackups: List<CloudBackupInfo> = emptyList(),
    val isLoadingCloudBackups: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val cloudBackupScheduler: CloudBackupScheduler,
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    private var currentPasscode: String? = null

    init {
        loadSettingsData()
    }

    private fun loadSettingsData() {
        viewModelScope.launch { accountRepository.getAllAccounts().collect { _state.update { s -> s.copy(accounts = it) } } }
        viewModelScope.launch { userPreferences.defaultAccountId.collect { id -> _state.update { it.copy(defaultAccountId = id) } } }
        viewModelScope.launch { userPreferences.currencyCode.collect { code -> _state.update { it.copy(currencyCode = code) } } }
        viewModelScope.launch { userPreferences.firstDayOfWeek.collect { day -> _state.update { it.copy(firstDayOfWeek = day) } } }
        viewModelScope.launch { userPreferences.biometricEnabled.collect { enabled -> _state.update { it.copy(biometricEnabled = enabled) } } }
        viewModelScope.launch { userPreferences.passcodeEnabled.collect { enabled -> _state.update { it.copy(passcodeEnabled = enabled) } } }
        viewModelScope.launch { userPreferences.passcode.collect { passcode -> currentPasscode = passcode } }
        viewModelScope.launch { userPreferences.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } } }
        viewModelScope.launch { userPreferences.defaultNotifyForRecurringEntries.collect { enabled -> _state.update { it.copy(defaultNotifyForRecurringEntries = enabled) } } }
        viewModelScope.launch {
            userPreferences.dailyReminderEnabled.collect { enabled ->
                _state.update { it.copy(dailyReminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.budgetAlertsEnabled.collect { enabled ->
                _state.update { it.copy(budgetAlertsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.autoCloudBackupEnabled.collect { enabled ->
                _state.update { it.copy(autoCloudBackupEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.lastCloudBackupTime.collect { timestamp ->
                _state.update { it.copy(lastCloudBackupTime = timestamp) }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setBiometricEnabled(enabled) }
    fun setPasscode(pass: String?) = viewModelScope.launch { userPreferences.setPasscode(pass) }
    fun setPasscodeEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setPasscodeEnabled(enabled) }
    fun verifyCurrentPasscode(input: String): Boolean = !currentPasscode.isNullOrBlank() && input == currentPasscode
    fun setThemeMode(mode: Int) = viewModelScope.launch { 
        userPreferences.setThemeMode(mode)
        MoneyTrackerWidget().updateAll(context)
    }
    fun setDefaultNotifyForRecurringEntries(enabled: Boolean) = viewModelScope.launch { userPreferences.setDefaultNotifyForRecurringEntries(enabled) }
    fun setDefaultAccount(accountId: String?) = viewModelScope.launch { userPreferences.setDefaultAccountId(accountId) }
    fun setCurrency(code: String) = viewModelScope.launch { userPreferences.setCurrencyCode(code) }
    fun setFirstDayOfWeek(day: Int) = viewModelScope.launch { userPreferences.setFirstDayOfWeek(day) }
    fun clearMessage() = _state.update { it.copy(exportMessage = null, importMessage = null) }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                transactionRepository.clearAllTransactions()
                budgetRepository.clearAllBudgets()
                accountRepository.clearAllAccounts()
                accountRepository.seedDefaultAccounts()
                categoryRepository.seedDefaultCategories()
                _state.update { it.copy(importMessage = "All data has been cleared and defaults restored") }
            } catch (e: Exception) {
                _state.update { it.copy(importMessage = "Reset failed: ${e.message}") }
            }
        }
    }

    fun exportMonthlyCsv(context: Context, uri: Uri, month: Int, year: Int) {
        viewModelScope.launch {
            try {
                val rows = buildMonthlyExportRows(month, year)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write("TIME,TYPE,AMOUNT,CATEGORY,ACCOUNT,NOTES")
                        writer.newLine()
                        rows.forEach { row ->
                            writer.write(
                                listOf(
                                    csvValue(row.time),
                                    csvValue(row.type),
                                    csvValue(row.amount),
                                    csvValue(row.category),
                                    csvValue(row.account),
                                    csvValue(row.notes)
                                ).joinToString(",")
                            )
                            writer.newLine()
                        }
                    }
                }
                _state.update { it.copy(exportMessage = "CSV exported: ${rows.size} transactions") }
            } catch (e: Exception) {
                _state.update { it.copy(exportMessage = "CSV export failed: ${e.message}") }
            }
        }
    }

    fun exportMonthlyPdf(context: Context, uri: Uri, month: Int, year: Int) {
        viewModelScope.launch {
            try {
                val rows = buildMonthlyExportRows(month, year)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    writeMonthlyPdf(outputStream, rows, month, year)
                }
                _state.update { it.copy(exportMessage = "PDF exported: ${rows.size} transactions") }
            } catch (e: Exception) {
                _state.update { it.copy(exportMessage = "PDF export failed: ${e.message}") }
            }
        }
    }

    // ==========================================
    // EXPORT JSON
    // ==========================================
    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactionsRaw()
                val accounts = accountRepository.getAllAccountsForBackup()
                val allBudgets = budgetRepository.getAllBudgets()
                
                val accountMap = accounts.associateBy { it.id }
                val allCategories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                val categoryMap = allCategories.associateBy { it.id }
                
                val rootObj = JSONObject()

                // 1. Export Accounts
                val accountsArray = JSONArray()
                accounts.forEach { acc ->
                    val accObj = JSONObject().apply {
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
                    accountsArray.put(accObj)
                }
                rootObj.put("accounts", accountsArray)

                // 2. Export Budgets
                val budgetsArray = JSONArray()
                allBudgets.forEach { budget ->
                    val budgetObj = JSONObject().apply {
                        put("categoryName", categoryMap[budget.categoryId]?.name ?: "Unknown")
                        put("limitAmount", budget.limitAmount)
                        put("month", budget.month)
                        put("year", budget.year)
                    }
                    budgetsArray.put(budgetObj)
                }
                rootObj.put("budgets", budgetsArray)

                // 3. Export Transactions
                val txnsArray = JSONArray()
                transactions.forEach { txnWithSplits ->
                    val txn = txnWithSplits.transaction
                    val txnObj = JSONObject().apply {
                        put("date", txn.date) // Saving exact timestamp
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
                            val splitObj = JSONObject().apply {
                                put("categoryName", categoryMap[split.categoryId]?.name ?: "Unknown")
                                put("amount", split.amount)
                            }
                            splitsArray.put(splitObj)
                        }
                        put("splits", splitsArray)
                    }
                    txnsArray.put(txnObj)
                }
                rootObj.put("transactions", txnsArray)

                // Write JSON to file
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(rootObj.toString(4)) // 4 spaces indentation for readability
                    }
                }
                
                val msg = "Exported ${accounts.size} accounts, ${allBudgets.size} budgets, and ${transactions.size} transactions"
                _state.update { it.copy(exportMessage = msg) }
            } catch (e: Exception) {
                _state.update { it.copy(exportMessage = "Export failed: ${e.message}") }
            }
        }
    }

    private data class MonthlyExportRow(
        val time: String,
        val type: String,
        val amount: String,
        val category: String,
        val account: String,
        val notes: String
    )

    private suspend fun buildMonthlyExportRows(month: Int, year: Int): List<MonthlyExportRow> {
        val transactions = transactionRepository.getAllTransactionsRaw()
        val accounts = accountRepository.getAllAccountsOnce()
        val accountMap = accounts.associateBy { it.id }
        val categories = categoryRepository.getAllCategories().firstOrNull().orEmpty()
        val categoryMap = categories.associateBy { it.id }

        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val timeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

        return transactions
            .asSequence()
            .filter { it.transaction.date in start..end }
            .sortedBy { it.transaction.date }
            .map { txnWithSplits ->
                val txn = txnWithSplits.transaction

                val typeText = when (txn.type) {
                    TransactionType.INCOME -> "(+) Income"
                    TransactionType.EXPENSE -> "(-) Expense"
                    TransactionType.TRANSFER -> "(*) Transfer"
                }

                val categoryText = when (txn.type) {
                    TransactionType.TRANSFER -> "-"
                    else -> txnWithSplits.splits
                        .mapNotNull { split -> categoryMap[split.categoryId]?.name }
                        .distinct()
                        .joinToString(" | ")
                        .ifBlank { "Unknown" }
                }

                val fromName = accountMap[txn.accountId]?.name ?: "Unknown"
                val accountText = if (txn.type == TransactionType.TRANSFER) {
                    val toName = txn.toAccountId?.let { accountMap[it]?.name } ?: "Unknown"
                    "$fromName->$toName"
                } else {
                    fromName
                }

                MonthlyExportRow(
                    time = timeFormat.format(Date(txn.date)),
                    type = typeText,
                    amount = String.format(Locale.US, "%.2f", txn.totalAmount),
                    category = categoryText,
                    account = accountText,
                    notes = txn.note.orEmpty()
                )
            }
            .toList()
    }

    private fun csvValue(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun writeMonthlyPdf(
        outputStream: OutputStream,
        rows: List<MonthlyExportRow>,
        month: Int,
        year: Int
    ) {
        val pdf = PdfDocument()
        val titlePaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 9f }

        val pageWidth = 842
        val pageHeight = 595
        val margin = 24
        val rowHeight = 16

        val monthName = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
        }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "$month"

        val colX = intArrayOf(24, 190, 280, 360, 470, 620)

        var pageNumber = 1
        var y = margin
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        fun drawHeader() {
            y = margin
            canvas.drawText("MoneyTracker Monthly Report - $monthName $year", margin.toFloat(), y.toFloat(), titlePaint)
            y += 22
            canvas.drawText("TIME", colX[0].toFloat(), y.toFloat(), headerPaint)
            canvas.drawText("TYPE", colX[1].toFloat(), y.toFloat(), headerPaint)
            canvas.drawText("AMOUNT", colX[2].toFloat(), y.toFloat(), headerPaint)
            canvas.drawText("CATEGORY", colX[3].toFloat(), y.toFloat(), headerPaint)
            canvas.drawText("ACCOUNT", colX[4].toFloat(), y.toFloat(), headerPaint)
            canvas.drawText("NOTES", colX[5].toFloat(), y.toFloat(), headerPaint)
            y += rowHeight
        }

        fun trimToWidth(value: String, maxChars: Int): String =
            if (value.length <= maxChars) value else value.take(maxChars - 1) + "…"

        drawHeader()

        rows.forEach { row ->
            if (y > pageHeight - margin) {
                pdf.finishPage(page)
                pageNumber += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                drawHeader()
            }

            canvas.drawText(trimToWidth(row.time, 25), colX[0].toFloat(), y.toFloat(), textPaint)
            canvas.drawText(trimToWidth(row.type, 12), colX[1].toFloat(), y.toFloat(), textPaint)
            canvas.drawText(trimToWidth(row.amount, 12), colX[2].toFloat(), y.toFloat(), textPaint)
            canvas.drawText(trimToWidth(row.category, 18), colX[3].toFloat(), y.toFloat(), textPaint)
            canvas.drawText(trimToWidth(row.account, 16), colX[4].toFloat(), y.toFloat(), textPaint)
            canvas.drawText(trimToWidth(row.notes, 28), colX[5].toFloat(), y.toFloat(), textPaint)
            y += rowHeight
        }

        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    // ==========================================
    // IMPORT JSON
    // ==========================================
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                fun normalizeKey(value: String): String =
                    value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

                fun categoryKey(name: String, type: TransactionType): Pair<String, TransactionType> =
                    normalizeKey(name) to type

                // Read file
                val jsonString = context.contentResolver.openInputStream(uri)?.use { 
                    it.readBytes().toString(Charsets.UTF_8) 
                } ?: throw Exception("Could not read file")

                val rootObj = JSONObject(jsonString)

                val accounts = accountRepository.getAllAccountsForBackup()
                val accountByName = accounts.associateBy { normalizeKey(it.name) }.toMutableMap()

                val allCategories = mutableMapOf<Pair<String, TransactionType>, com.moneytracker.app.domain.model.Category>()
                categoryRepository.getAllCategories().first().forEach { cat ->
                    allCategories[categoryKey(cat.name, cat.type)] = cat
                }

                var importedAccounts = 0
                var importedBudgets = 0
                var importedTxns = 0

                // 1. Import Accounts
                val accountsArray = rootObj.optJSONArray("accounts")
                if (accountsArray != null) {
                    for (i in 0 until accountsArray.length()) {
                        val accObj = accountsArray.getJSONObject(i)
                        val name = accObj.getString("name").trim()
                        val importedInitial = accObj.getDouble("initialBalance")
                        
                        // FIX: Start the current balance at the initial balance. 
                        // The imported transactions will automatically rebuild the rest.
                        val startingBalance = importedInitial 
                        
                        val typeStr = accObj.getString("type")
                        val customTypeName = accObj.optString("customTypeName", "").ifBlank { null }
                        val currency = accObj.optString("currency", "INR")
                        val colorHex = accObj.getString("colorHex")
                        val iconKey = accObj.getString("iconKey")
                        val isActive = accObj.optBoolean("isActive", true)
                        val isDeleted = accObj.optBoolean("isDeleted", false)
                        
                        val existingAccount = accountByName[normalizeKey(name)]
                        
                        if (existingAccount == null) {
                            val newAccount = Account(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = com.moneytracker.app.data.local.database.entities.AccountType.valueOf(typeStr),
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
                            importedAccounts++
                        } else {
                            val updatedAccount = existingAccount.copy(
                                initialBalance = importedInitial,
                                currentBalance = startingBalance,
                                customTypeName = customTypeName,
                                currency = currency,
                                colorHex = colorHex,
                                iconKey = iconKey,
                                isActive = isActive,
                                isDeleted = isDeleted
                            )
                            accountRepository.saveAccount(updatedAccount)
                            accountByName[normalizeKey(name)] = updatedAccount
                            importedAccounts++ 
                        }
                    }
                }

                // 2. Import Budgets
                val budgetsArray = rootObj.optJSONArray("budgets")
                if (budgetsArray != null) {
                    for (i in 0 until budgetsArray.length()) {
                        val bObj = budgetsArray.getJSONObject(i)
                        val categoryName = bObj.getString("categoryName").trim()
                        val budgetCategoryKey = categoryKey(categoryName, TransactionType.EXPENSE)
                        
                        var category = allCategories[budgetCategoryKey]
                        if (category == null) {
                            category = com.moneytracker.app.domain.model.Category(
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
                            limitAmount = bObj.getDouble("limitAmount"),
                            month = bObj.getInt("month"),
                            year = bObj.getInt("year")
                        )
                        importedBudgets++
                    }
                }

                // 3. Import Transactions
                val txnsArray = rootObj.optJSONArray("transactions")
                if (txnsArray != null) {
                    for (i in 0 until txnsArray.length()) {
                        val txnObj = txnsArray.getJSONObject(i)
                        
                        val type = TransactionType.valueOf(txnObj.getString("type"))
                        val fromAccountName = txnObj.getString("fromAccountName").trim()
                        
                        // Ensure From Account exists
                        var fromAccount = accountByName[normalizeKey(fromAccountName)]
                        if (fromAccount == null) {
                            fromAccount = Account(
                                id = UUID.randomUUID().toString(), name = fromAccountName,
                                type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                initialBalance = 0.0, currentBalance = 0.0, colorHex = "#2196F3", iconKey = "bank"
                            )
                            accountRepository.saveAccount(fromAccount)
                            accountByName[normalizeKey(fromAccountName)] = fromAccount
                        }

                        val transactionId = UUID.randomUUID().toString()
                        var toAccountId: String? = null

                        if (type == TransactionType.TRANSFER) {
                            val toAccountName = txnObj.optString("toAccountName", "Unknown").trim()
                            var toAccount = accountByName[normalizeKey(toAccountName)]
                            if (toAccount == null) {
                                toAccount = Account(
                                    id = UUID.randomUUID().toString(), name = toAccountName,
                                    type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                    initialBalance = 0.0, currentBalance = 0.0, colorHex = "#4CAF50", iconKey = "bank"
                                )
                                accountRepository.saveAccount(toAccount)
                                accountByName[normalizeKey(toAccountName)] = toAccount
                            }
                            toAccountId = toAccount.id
                        }

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
                                val catName = splitObj.getString("categoryName").trim()
                                val splitType = if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
                                val splitCategoryKey = categoryKey(catName, splitType)
                                
                                var cat = allCategories[splitCategoryKey]
                                    ?: allCategories[categoryKey(catName, TransactionType.EXPENSE)]
                                if (cat == null) {
                                    cat = com.moneytracker.app.domain.model.Category(
                                        id = UUID.randomUUID().toString(),
                                        name = catName,
                                        type = splitType,
                                        colorHex = if (splitType == TransactionType.INCOME) "#4CAF50" else "#FF5722",
                                        iconKey = "more_horiz"
                                    )
                                    categoryRepository.saveCategory(cat)
                                    allCategories[splitCategoryKey] = cat
                                }
                                
                                splitsEntities.add(
                                    TransactionSplitEntity(
                                        id = UUID.randomUUID().toString(),
                                        transactionId = transactionId,
                                        categoryId = cat.id,
                                        amount = splitObj.getDouble("amount"),
                                        note = null
                                    )
                                )
                            }
                        }

                        transactionRepository.saveTransaction(transaction, splitsEntities)
                        importedTxns++
                    }
                }

                val msg = "Imported $importedAccounts accounts, $importedBudgets budgets, and $importedTxns transactions."
                _state.update { it.copy(importMessage = msg) }

            } catch (e: org.json.JSONException) {
                // Fallback for old TSV files if a user tries to import an older backup
                _state.update { it.copy(importMessage = "Invalid file format. Please use the new .json format. Error: ${e.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(importMessage = "Import failed: ${e.message}") }
            }
        }
    }
    fun setDailyReminderEnabled(context: android.content.Context, enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDailyReminderEnabled(enabled)
            if (enabled) {
                com.moneytracker.app.notifications.NotificationScheduler.scheduleDailyReminder(context)
            } else {
                com.moneytracker.app.notifications.NotificationScheduler.cancelDailyReminder(context)
            }
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setAutoCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoCloudBackupEnabled(enabled)
            if (enabled) {
                cloudBackupScheduler.scheduleDailyBackup()
            } else {
                cloudBackupScheduler.cancelDailyBackup()
            }
        }
    }

    fun backupNowToCloud() {
        viewModelScope.launch {
            when (val result = googleDriveBackupService.backupNow()) {
                BackupResult.Success -> _state.update { it.copy(exportMessage = "Cloud backup completed") }
                BackupResult.NotSignedIn -> _state.update { it.copy(exportMessage = "Google sign-in required for cloud backup") }
                BackupResult.NoBackupFound -> _state.update { it.copy(exportMessage = "No cloud backup found") }
                is BackupResult.Error -> _state.update { it.copy(exportMessage = "Cloud backup failed: ${result.message}") }
            }
        }
    }

    fun restoreNowFromCloud() {
        viewModelScope.launch {
            when (val result = googleDriveBackupService.restoreLatestBackupFromCloud()) {
                BackupResult.Success -> _state.update { it.copy(importMessage = "Cloud restore completed") }
                BackupResult.NotSignedIn -> _state.update { it.copy(importMessage = "Google sign-in required for cloud restore") }
                BackupResult.NoBackupFound -> _state.update { it.copy(importMessage = "No cloud backup found") }
                is BackupResult.Error -> _state.update { it.copy(importMessage = "Cloud restore failed: ${result.message}") }
            }
        }
    }

    fun loadCloudBackups() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCloudBackups = true, cloudBackups = emptyList()) }
            googleDriveBackupService.listAvailableBackups()
                .onSuccess { backups ->
                    _state.update { it.copy(cloudBackups = backups, isLoadingCloudBackups = false) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoadingCloudBackups = false, importMessage = "Could not load backups: ${err.message}") }
                }
        }
    }

    fun restoreFromCloudById(fileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(cloudBackups = emptyList()) }
            when (val result = googleDriveBackupService.restoreBackupById(fileId)) {
                BackupResult.Success -> _state.update { it.copy(importMessage = "Cloud restore completed") }
                BackupResult.NotSignedIn -> _state.update { it.copy(importMessage = "Google sign-in required") }
                BackupResult.NoBackupFound -> _state.update { it.copy(importMessage = "Backup not found") }
                is BackupResult.Error -> _state.update { it.copy(importMessage = "Restore failed: ${result.message}") }
            }
        }
    }

    fun clearCloudBackups() {
        _state.update { it.copy(cloudBackups = emptyList(), isLoadingCloudBackups = false) }
    }
}
