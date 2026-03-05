package com.moneytracker.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
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
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
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

    // ==========================================
    // EXPORT JSON
    // ==========================================
    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactionsRaw()
                val accounts = accountRepository.getAllAccountsOnce()
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
                        put("initialBalance", acc.initialBalance)
                        put("currentBalance", acc.currentBalance)
                        put("colorHex", acc.colorHex)
                        put("iconKey", acc.iconKey)
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

    // ==========================================
    // IMPORT JSON
    // ==========================================
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Read file
                val jsonString = context.contentResolver.openInputStream(uri)?.use { 
                    it.readBytes().toString(Charsets.UTF_8) 
                } ?: throw Exception("Could not read file")

                val rootObj = JSONObject(jsonString)

                val accounts = accountRepository.getAllAccountsOnce()
                val accountByName = accounts.associateBy { it.name }.toMutableMap()

                val allCategories = mutableMapOf<String, com.moneytracker.app.domain.model.Category>()
                categoryRepository.getAllCategories().first().forEach { cat ->
                    allCategories[cat.name] = cat
                }

                var importedAccounts = 0
                var importedBudgets = 0
                var importedTxns = 0

                // 1. Import Accounts
                val accountsArray = rootObj.optJSONArray("accounts")
                if (accountsArray != null) {
                    for (i in 0 until accountsArray.length()) {
                        val accObj = accountsArray.getJSONObject(i)
                        val name = accObj.getString("name")
                        val importedInitial = accObj.getDouble("initialBalance")
                        
                        // FIX: Start the current balance at the initial balance. 
                        // The imported transactions will automatically rebuild the rest.
                        val startingBalance = importedInitial 
                        
                        val typeStr = accObj.getString("type")
                        val colorHex = accObj.getString("colorHex")
                        val iconKey = accObj.getString("iconKey")
                        
                        val existingAccount = accountByName[name]
                        
                        if (existingAccount == null) {
                            val newAccount = Account(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = com.moneytracker.app.data.local.database.entities.AccountType.valueOf(typeStr),
                                initialBalance = importedInitial,
                                currentBalance = startingBalance, // Set to initial
                                colorHex = colorHex,
                                iconKey = iconKey
                            )
                            accountRepository.saveAccount(newAccount)
                            accountByName[name] = newAccount
                            importedAccounts++
                        } else {
                            val updatedAccount = existingAccount.copy(
                                initialBalance = importedInitial,
                                currentBalance = startingBalance, // Set to initial
                                colorHex = colorHex,
                                iconKey = iconKey
                            )
                            accountRepository.saveAccount(updatedAccount)
                            accountByName[name] = updatedAccount
                            importedAccounts++ 
                        }
                    }
                }

                // 2. Import Budgets
                val budgetsArray = rootObj.optJSONArray("budgets")
                if (budgetsArray != null) {
                    for (i in 0 until budgetsArray.length()) {
                        val bObj = budgetsArray.getJSONObject(i)
                        val categoryName = bObj.getString("categoryName")
                        
                        var category = allCategories[categoryName]
                        if (category == null) {
                            category = com.moneytracker.app.domain.model.Category(
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
                        val fromAccountName = txnObj.getString("fromAccountName")
                        
                        // Ensure From Account exists
                        var fromAccount = accountByName[fromAccountName]
                        if (fromAccount == null) {
                            fromAccount = Account(
                                id = UUID.randomUUID().toString(), name = fromAccountName,
                                type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                initialBalance = 0.0, currentBalance = 0.0, colorHex = "#2196F3", iconKey = "bank"
                            )
                            accountRepository.saveAccount(fromAccount)
                            accountByName[fromAccountName] = fromAccount
                        }

                        val transactionId = UUID.randomUUID().toString()
                        var toAccountId: String? = null

                        if (type == TransactionType.TRANSFER) {
                            val toAccountName = txnObj.optString("toAccountName", "Unknown")
                            var toAccount = accountByName[toAccountName]
                            if (toAccount == null) {
                                toAccount = Account(
                                    id = UUID.randomUUID().toString(), name = toAccountName,
                                    type = com.moneytracker.app.data.local.database.entities.AccountType.BANK,
                                    initialBalance = 0.0, currentBalance = 0.0, colorHex = "#4CAF50", iconKey = "bank"
                                )
                                accountRepository.saveAccount(toAccount)
                                accountByName[toAccountName] = toAccount
                            }
                            toAccountId = toAccount.id
                        }

                        val transaction = TransactionEntity(
                            id = transactionId,
                            accountId = fromAccount.id,
                            toAccountId = toAccountId,
                            payee = txnObj.optString("payee", "Transaction"),
                            note = txnObj.optString("note", "").ifBlank { null },
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
                                val catName = splitObj.getString("categoryName")
                                
                                var cat = allCategories[catName]
                                if (cat == null) {
                                    cat = com.moneytracker.app.domain.model.Category(
                                        id = UUID.randomUUID().toString(),
                                        name = catName,
                                        type = type,
                                        colorHex = if (type == TransactionType.INCOME) "#4CAF50" else "#FF5722",
                                        iconKey = "more_horiz"
                                    )
                                    categoryRepository.saveCategory(cat)
                                    allCategories[catName] = cat
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
}
