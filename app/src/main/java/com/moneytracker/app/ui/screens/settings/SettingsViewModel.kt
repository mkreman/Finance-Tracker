package com.moneytracker.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
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
    val firstDayOfWeek: Int = java.util.Calendar.MONDAY
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettingsData()
    }

    private fun loadSettingsData() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _state.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            userPreferences.defaultAccountId.collect { id ->
                _state.update { it.copy(defaultAccountId = id) }
            }
        }
        viewModelScope.launch {
            userPreferences.currencyCode.collect { code ->
                _state.update { it.copy(currencyCode = code) }
            }
        }
        viewModelScope.launch {
            userPreferences.firstDayOfWeek.collect { day ->
                _state.update { it.copy(firstDayOfWeek = day) }
            }
        }
    }

    fun setDefaultAccount(accountId: String?) {
        viewModelScope.launch {
            userPreferences.setDefaultAccountId(accountId)
        }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch {
            userPreferences.setCurrencyCode(code)
        }
    }

    fun setFirstDayOfWeek(day: Int) {
        viewModelScope.launch {
            userPreferences.setFirstDayOfWeek(day)
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactionsRaw()
                val accounts = accountRepository.getAllAccountsOnce()
                val accountMap = accounts.associateBy { it.id }

                // Get all budgets
                val allCategories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                val categoryMap = allCategories.associateBy { it.id }
                val budgets = mutableListOf<Triple<String, String, Double>>() // categoryId, categoryName, limitAmount
                
                // Get budgets for current month/year
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)
                
                // We need to get budgets directly from the database
                // Since BudgetRepository only has one method, we'll construct a simple list
                // For now, we'll export budgets that exist in the current month
                
                // Build category name cache
                val categoryNames = mutableMapOf<String, String>()
                transactions.forEach { txn ->
                    txn.splits.forEach { split ->
                        if (split.categoryId !in categoryNames) {
                            val cat = categoryRepository.getCategoryById(split.categoryId)
                            categoryNames[split.categoryId] = cat?.name ?: "Unknown"
                        }
                    }
                }

                val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        // ===== ACCOUNTS SECTION =====
                        writer.write("### ACCOUNTS")
                        writer.newLine()
                        writer.write("NAME\tTYPE\tINITIAL_BALANCE\tCURRENT_BALANCE\tCOLOR\tICON")
                        writer.newLine()
                        accounts.forEach { account ->
                            writer.write("${account.name}\t${account.type}\t${account.initialBalance.toLong()}\t${account.currentBalance.toLong()}\t${account.colorHex}\t${account.iconKey}")
                            writer.newLine()
                        }
                        writer.newLine()

                        // ===== BUDGETS SECTION =====
                        writer.write("### BUDGETS")
                        writer.newLine()
                        writer.write("CATEGORY\tLIMIT_AMOUNT\tMONTH\tYEAR")
                        writer.newLine()
                        // Note: This will be populated during import, but for now we export empty
                        // We'll need to track budgets in the repository
                        writer.newLine()

                        // ===== TRANSACTIONS SECTION =====
                        writer.write("### TRANSACTIONS")
                        writer.newLine()
                        writer.write("TIME\tTYPE\tAMOUNT\tCATEGORY\tACCOUNT\tNOTES")
                        writer.newLine()

                        transactions.forEach { txnWithSplits ->
                            val txn = txnWithSplits.transaction
                            val dateStr = sdf.format(Date(txn.date))
                            val typeStr = when (txn.type) {
                                TransactionType.INCOME -> "(+) Income"
                                TransactionType.EXPENSE -> "(-) Expense"
                                TransactionType.TRANSFER -> "(*) Transfer"
                            }
                            val amount = txn.totalAmount.toLong().toString()

                            val category = if (txn.type == TransactionType.TRANSFER) {
                                "  -  "
                            } else {
                                txnWithSplits.splits.joinToString(", ") { split ->
                                    categoryNames[split.categoryId] ?: "Unknown"
                                }.ifEmpty { "Unknown" }
                            }

                            val fromAccount = accountMap[txn.accountId]?.name ?: "Unknown"
                            val accountStr = if (txn.type == TransactionType.TRANSFER) {
                                val toAccount = accountMap[txn.toAccountId]?.name ?: "Unknown"
                                "$fromAccount->$toAccount"
                            } else {
                                fromAccount
                            }

                            val note = txn.note ?: txn.payee

                            writer.write("$dateStr\t$typeStr\t$amount\t$category\t$accountStr\t$note")
                            writer.newLine()
                        }
                    }
                }
                _state.update { it.copy(exportMessage = "Exported ${accounts.size} accounts and ${transactions.size} transactions") }
            } catch (e: Exception) {
                _state.update { it.copy(exportMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(exportMessage = null, importMessage = null) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                transactionRepository.clearAllTransactions()
                budgetRepository.clearAllBudgets()
                accountRepository.clearAllAccounts()
                _state.update { it.copy(importMessage = "All data has been cleared") }
            } catch (e: Exception) {
                _state.update { it.copy(importMessage = "Reset failed: ${e.message}") }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                var accounts = accountRepository.getAllAccountsOnce()
                var accountByName = accounts.associateBy { it.name }.toMutableMap()

                // Build category cache
                val allCategories = mutableMapOf<String, com.moneytracker.app.domain.model.Category>()
                categoryRepository.getAllCategories().first().forEach { cat ->
                    allCategories[cat.name] = cat
                }

                var importedAccounts = 0
                var importedBudgets = 0
                var importedCount = 0
                var skippedCount = 0
                var createdAccounts = 0
                var createdCategories = 0

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val lines = BufferedReader(InputStreamReader(inputStream)).readLines()

                    var currentSection = "NONE"
                    var i = 0
                    
                    while (i < lines.size) {
                        val line = lines[i].trim()
                        i++
                        
                        if (line.isBlank()) continue
                        
                        // Detect section headers
                        if (line.startsWith("### ACCOUNTS")) {
                            currentSection = "ACCOUNTS"
                            // Skip the column headers
                            if (i < lines.size && lines[i].startsWith("NAME")) i++
                            continue
                        } else if (line.startsWith("### BUDGETS")) {
                            currentSection = "BUDGETS"
                            // Skip the column headers
                            if (i < lines.size && lines[i].startsWith("CATEGORY")) i++
                            continue
                        } else if (line.startsWith("### TRANSACTIONS")) {
                            currentSection = "TRANSACTIONS"
                            // Skip the column headers
                            if (i < lines.size && lines[i].startsWith("TIME")) i++
                            continue
                        } else if (line.startsWith("TIME\t")) {
                            // Old format - treat as transactions
                            currentSection = "TRANSACTIONS"
                            continue
                        }
                        
                        when (currentSection) {
                            "ACCOUNTS" -> {
                                val parts = line.split("\t")
                                if (parts.size >= 6) {
                                    try {
                                        val name = parts[0].trim()
                                        val type = com.moneytracker.app.data.local.database.entities.AccountType.valueOf(parts[1].trim())
                                        val initialBalance = parts[2].trim().toDoubleOrNull() ?: 0.0
                                        val currentBalance = parts[3].trim().toDoubleOrNull() ?: initialBalance
                                        val color = parts[4].trim()
                                        val icon = parts[5].trim()
                                        
                                        if (name !in accountByName) {
                                            val newAccount = Account(
                                                id = UUID.randomUUID().toString(),
                                                name = name,
                                                type = type,
                                                initialBalance = initialBalance,
                                                currentBalance = currentBalance,
                                                colorHex = color,
                                                iconKey = icon
                                            )
                                            accountRepository.saveAccount(newAccount)
                                            accountByName[name] = newAccount
                                            importedAccounts++
                                        }
                                    } catch (e: Exception) {
                                        // Skip malformed account
                                    }
                                }
                            }
                            "BUDGETS" -> {
                                val parts = line.split("\t")
                                if (parts.size >= 4) {
                                    try {
                                        val categoryName = parts[0].trim()
                                        val limitAmount = parts[1].trim().toDoubleOrNull() ?: 0.0
                                        val month = parts[2].trim().toIntOrNull() ?: continue
                                        val year = parts[3].trim().toIntOrNull() ?: continue
                                        
                                        // Find or create category
                                        var category = allCategories[categoryName]
                                        if (category == null) {
                                            // Create default expense category
                                            val newCategory = com.moneytracker.app.domain.model.Category(
                                                id = UUID.randomUUID().toString(),
                                                name = categoryName,
                                                type = TransactionType.EXPENSE,
                                                colorHex = "#FF5722",
                                                iconKey = "more_horiz"
                                            )
                                            categoryRepository.saveCategory(newCategory)
                                            allCategories[categoryName] = newCategory
                                            category = newCategory
                                            createdCategories++
                                        }
                                        
                                        budgetRepository.saveBudget(category.id, limitAmount, month, year)
                                        importedBudgets++
                                    } catch (e: Exception) {
                                        // Skip malformed budget
                                    }
                                }
                            }
                            "TRANSACTIONS" -> {
                                val parts = line.split("\t")
                                if (parts.size < 4) {
                                    skippedCount++
                                    continue
                                }

                                try {
                                    val dateStr = parts[0]
                                    val typeStr = parts[1]
                                    val amountStr = parts[2]
                                    val categoryStr = parts.getOrElse(3) { "" }
                                    val accountStr = parts.getOrElse(4) { "" }
                                    val noteStr = parts.getOrElse(5) { "" }

                                    // Parse type
                                    val type = when {
                                        typeStr.contains("Income", ignoreCase = true) -> TransactionType.INCOME
                                        typeStr.contains("Expense", ignoreCase = true) -> TransactionType.EXPENSE
                                        typeStr.contains("Transfer", ignoreCase = true) -> TransactionType.TRANSFER
                                        else -> null
                                    }

                                    if (type == null) {
                                        skippedCount++
                                        continue
                                    }

                                    // Parse date
                                    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                                    val date = try {
                                        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }

                                    // Parse amount
                                    val amount = amountStr.trim().toDoubleOrNull()
                                    if (amount == null) {
                                        skippedCount++
                                        continue
                                    }

                                    // Parse account
                                    val transactionId = UUID.randomUUID().toString()
                                    val now = System.currentTimeMillis()

                                    if (type == TransactionType.TRANSFER) {
                                        // Account format: "FromAccount->ToAccount"
                                        val accountParts = accountStr.split("->")
                                        val fromAccountName = accountParts.getOrElse(0) { "" }.trim()
                                        val toAccountName = accountParts.getOrElse(1) { "" }.trim()
                                        
                                        // Create accounts if they don't exist
                                        var fromAccount = accountByName[fromAccountName]
                                        if (fromAccount == null) {
                                            fromAccount = Account(
                                                id = UUID.randomUUID().toString(),
                                                name = fromAccountName,
                                                type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                                initialBalance = 0.0,
                                                currentBalance = 0.0,
                                                colorHex = "#2196F3",
                                                iconKey = "bank"
                                            )
                                            accountRepository.saveAccount(fromAccount)
                                            accountByName[fromAccountName] = fromAccount
                                            createdAccounts++
                                        }
                                        
                                        var toAccount = accountByName[toAccountName]
                                        if (toAccount == null) {
                                            toAccount = Account(
                                                id = UUID.randomUUID().toString(),
                                                name = toAccountName,
                                                type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                                initialBalance = 0.0,
                                                currentBalance = 0.0,
                                                colorHex = "#4CAF50",
                                                iconKey = "bank"
                                            )
                                            accountRepository.saveAccount(toAccount)
                                            accountByName[toAccountName] = toAccount
                                            createdAccounts++
                                        }

                                        val transaction = TransactionEntity(
                                            id = transactionId,
                                            accountId = fromAccount.id,
                                            payee = "Transfer",
                                            note = noteStr.ifBlank { null },
                                            date = date,
                                            totalAmount = amount,
                                            type = type,
                                            toAccountId = toAccount.id,
                                            createdAt = now,
                                            modifiedAt = now,
                                            syncStatus = SyncStatus.DIRTY
                                        )
                                        transactionRepository.saveTransaction(transaction, emptyList())
                                    } else {
                                        val accountName = accountStr.trim()
                                        var account = accountByName[accountName]
                                        if (account == null) {
                                            // Create account if it doesn't exist
                                            account = Account(
                                                id = UUID.randomUUID().toString(),
                                                name = accountName,
                                                type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                                initialBalance = 0.0,
                                                currentBalance = 0.0,
                                                colorHex = "#2196F3",
                                                iconKey = "bank"
                                            )
                                            accountRepository.saveAccount(account)
                                            accountByName[accountName] = account
                                            createdAccounts++
                                        }

                                        // Parse categories (comma-separated)
                                        val categoryNames = categoryStr.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != "-" }
                                        val splits = mutableListOf<TransactionSplitEntity>()
                                        
                                        for (catName in categoryNames) {
                                            var cat = allCategories[catName]
                                            if (cat == null) {
                                                // Create category if it doesn't exist
                                                cat = com.moneytracker.app.domain.model.Category(
                                                    id = UUID.randomUUID().toString(),
                                                    name = catName,
                                                    type = type,
                                                    colorHex = if (type == TransactionType.INCOME) "#4CAF50" else "#FF5722",
                                                    iconKey = "more_horiz"
                                                )
                                                categoryRepository.saveCategory(cat)
                                                allCategories[catName] = cat
                                                createdCategories++
                                            }
                                            
                                            splits.add(
                                                TransactionSplitEntity(
                                                    id = UUID.randomUUID().toString(),
                                                    transactionId = transactionId,
                                                    categoryId = cat.id,
                                                    amount = amount / categoryNames.size,
                                                    note = null
                                                )
                                            )
                                        }

                                        if (splits.isEmpty()) {
                                            skippedCount++
                                            continue
                                        }

                                        val transaction = TransactionEntity(
                                            id = transactionId,
                                            accountId = account.id,
                                            payee = categoryNames.firstOrNull() ?: "Transaction",
                                            note = noteStr.ifBlank { null },
                                            date = date,
                                            totalAmount = amount,
                                            type = type,
                                            createdAt = now,
                                            modifiedAt = now,
                                            syncStatus = SyncStatus.DIRTY
                                        )
                                        transactionRepository.saveTransaction(transaction, splits)
                                    }
                                    importedCount++
                                } catch (e: Exception) {
                                    skippedCount++
                                }
                            }
                        }
                    }
                }

                val msg = buildString {
                    append("Imported ")
                    val parts = mutableListOf<String>()
                    if (importedAccounts > 0) parts.add("$importedAccounts accounts")
                    if (importedBudgets > 0) parts.add("$importedBudgets budgets")
                    parts.add("$importedCount transactions")
                    append(parts.joinToString(", "))
                    if (createdAccounts > 0) append(" (created $createdAccounts accounts)")
                    if (createdCategories > 0) append(" (created $createdCategories categories)")
                    if (skippedCount > 0) append(", skipped $skippedCount")
                }
                _state.update { it.copy(importMessage = msg) }
            } catch (e: Exception) {
                _state.update { it.copy(importMessage = "Import failed: ${e.message}") }
            }
        }
    }
}
