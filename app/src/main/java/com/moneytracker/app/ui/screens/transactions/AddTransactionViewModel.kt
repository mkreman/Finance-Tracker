package com.moneytracker.app.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.database.entities.RecurringUnit
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.CategoryRecommendationRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.data.recurring.RecurringTransactionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryRecommendationRepository: CategoryRecommendationRepository,
    private val userPreferences: UserPreferences,
    private val recurringTransactionManager: RecurringTransactionManager,
    private val budgetAlertManager: com.moneytracker.app.notifications.BudgetAlertManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun decodeReceiptUris(serialized: String?): List<String> {
        return serialized?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun encodeReceiptUris(uris: List<String>): String? {
        val cleaned = uris.map { it.trim() }.filter { it.isNotBlank() }
        return if (cleaned.isEmpty()) null else cleaned.joinToString("\n")
    }

    private fun toEditableAmount(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun autoDivideAmounts(splits: List<SplitState>, totalAmount: Double): List<SplitState> {
        if (splits.isEmpty() || totalAmount <= 0.0) return splits
        val divided = BigDecimal.valueOf(totalAmount / splits.size)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return splits.map { it.copy(amount = divided) }
    }

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    // NEW: State for managing the Splitwise account creation prompt
    private val _showCreateAccountPrompt = MutableStateFlow<String?>(null)
    val showCreateAccountPrompt: StateFlow<String?> = _showCreateAccountPrompt.asStateFlow()

    private val editTransactionId: String? = savedStateHandle.get<String>("transactionId")
    private val initialType: String? = savedStateHandle.get<String>("type")
    private val initialAmount: String? = savedStateHandle.get<String>("amount")
    private val initialNote: String? = savedStateHandle.get<String>("note")
    private val initialPayee: String? = savedStateHandle.get<String>("payee")
    private val suggestedCategoryId: String? = savedStateHandle.get<String>("suggestedCategoryId")
    private val suggestedAccountId: String? = savedStateHandle.get<String>("suggestedAccountId")

    init {
        loadData()
        if (editTransactionId != null) {
            loadTransaction(editTransactionId)
        } else if (initialType != null) {
            val txnType = try { TransactionType.valueOf(initialType) } catch (_: Exception) { null }
            if (txnType != null) {
                _state.update { it.copy(type = txnType) }
            }
        }

        if (editTransactionId == null) {
            _state.update { state ->
                val amountValue = initialAmount?.takeIf { it.isNotBlank() }
                state.copy(
                    amount = amountValue ?: state.amount,
                    note = initialNote ?: state.note,
                    payee = initialPayee ?: state.payee
                )
            }
        }
    }

    private fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId) ?: return@launch
            val parentRecurring = transaction.parentRecurringId?.let { parentId ->
                transactionRepository.getTransactionById(parentId)
            }
            val recurrenceSource = if (transaction.isRecurring) transaction else parentRecurring
            val primarySplit = transaction.splits.firstOrNull()

            _state.update { state ->
                state.copy(
                    isEditMode = true,
                    editTransactionId = transactionId,
                    type = transaction.type,
                    amount = toEditableAmount(transaction.totalAmount),
                    selectedAccountId = transaction.accountId,
                    toAccountId = transaction.toAccountId,
                    note = transaction.note ?: "",
                    payee = transaction.payee, 
                    date = transaction.date,
                    isSplitMode = false,
                    parentRecurringId = transaction.parentRecurringId,
                    isRecurring = recurrenceSource?.isRecurring ?: false,
                    recurringInterval = recurrenceSource?.recurringInterval?.toString() ?: "1",
                    recurringUnit = recurrenceSource?.recurringUnit ?: RecurringUnit.MONTH,
                    recurringEndDate = recurrenceSource?.recurringEndDate,
                    notifyForRecurringEntries = recurrenceSource?.notifyForRecurringEntries ?: transaction.notifyForRecurringEntries,
                    receiptUris = decodeReceiptUris(transaction.receiptUri),
                    selectedCategoryIds = primarySplit?.categoryId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { setOf(it) }
                        ?: emptySet(),
                    splits = listOf(
                        SplitState(
                            targetType = SplitTargetType.CATEGORY,
                            targetId = primarySplit?.categoryId,
                            targetName = primarySplit?.categoryName.orEmpty(),
                            amount = toEditableAmount(transaction.totalAmount)
                        )
                    )
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val defaultAccountId = userPreferences.defaultAccountId.first()
            val initialTxnType = runCatching { TransactionType.valueOf(initialType ?: "") }.getOrNull() ?: _state.value.type
            val recommendedAccountId = if (editTransactionId == null && !initialPayee.isNullOrBlank()) {
                categoryRecommendationRepository.getRecommendedAccountId(initialPayee, initialTxnType)
            } else {
                null
            }

            accountRepository.getAllAccounts().collect { accounts ->
                val sortedAccounts = accounts.sortedWith(compareBy<com.moneytracker.app.domain.model.Account> { 
                    when (it.type) {
                        AccountType.CASH -> 0
                        AccountType.WALLET -> 1
                        AccountType.BANK -> 2
                        AccountType.INVESTMENT -> 3
                        AccountType.PEOPLE -> 4
                        AccountType.CUSTOM -> 5
                    }
                }.thenBy { it.name })

                val peopleAccounts = accounts.filter { it.type == AccountType.PEOPLE }
                
                // NEW: Trigger account creation prompt if Splitwise payee doesn't exist
                val isPersonAccountFound = sortedAccounts.any { it.name.equals(initialPayee, ignoreCase = true) }
                if (editTransactionId == null && !initialPayee.isNullOrBlank() && !isPersonAccountFound && initialNote?.contains("Splitwise") == true) {
                    _showCreateAccountPrompt.value = initialPayee
                }

                _state.update { currentState ->
                    val accountId = when {
                        currentState.isEditMode -> currentState.selectedAccountId
                        currentState.selectedAccountId != null -> currentState.selectedAccountId
                        suggestedAccountId != null && sortedAccounts.any { it.id == suggestedAccountId } -> suggestedAccountId
                        recommendedAccountId != null && sortedAccounts.any { it.id == recommendedAccountId } -> recommendedAccountId
                        defaultAccountId != null && sortedAccounts.any { it.id == defaultAccountId } -> defaultAccountId
                        else -> sortedAccounts.firstOrNull()?.id
                    }
                    currentState.copy(
                        accounts = sortedAccounts,
                        peopleAccounts = peopleAccounts,
                        selectedAccountId = accountId
                    )
                }
            }
        }
        viewModelScope.launch {
            userPreferences.defaultNotifyForRecurringEntries.collect { enabled ->
                _state.update { currentState ->
                    if (currentState.isEditMode) currentState else currentState.copy(notifyForRecurringEntries = enabled)
                }
            }
        }
        viewModelScope.launch {
            _state.map { it.type }.distinctUntilChanged().collectLatest { type ->
                categoryRepository.getCategoriesByType(
                    if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
                ).collect { categories ->
                    val sortedCategories = categories.sortedBy { it.name.lowercase() }
                    _state.update { state ->
                        var newSelectedIds = state.selectedCategoryIds
                        var newSplits = state.splits
                        
                        if (suggestedCategoryId != null && newSelectedIds.isEmpty() && !state.isEditMode) {
                            val cat = sortedCategories.find { it.id == suggestedCategoryId }
                            if (cat != null) {
                                newSelectedIds = setOf(cat.id)
                                newSplits = listOf(SplitState(targetId = cat.id, targetName = cat.name, amount = state.amount))
                            }
                        }

                        state.copy(
                            categories = sortedCategories,
                            selectedCategoryIds = newSelectedIds,
                            splits = newSplits
                        )
                    }
                }
            }
        }
    }
    
    // NEW: Handle the prompt response
    fun confirmCreatePersonAccount(name: String) {
        viewModelScope.launch {
            val newAccountId = UUID.randomUUID().toString()
            val newAccount = com.moneytracker.app.domain.model.Account(
                id = newAccountId,
                name = name,
                type = AccountType.PEOPLE,
                initialBalance = 0.0,
                currentBalance = 0.0,
                colorHex = "#4CAF50",
                iconKey = "person"
            )
            accountRepository.saveAccount(newAccount)
            _state.update { it.copy(selectedAccountId = newAccountId) }
            _showCreateAccountPrompt.value = null
        }
    }

    // NEW: Handle the prompt response
    fun declineCreatePersonAccount() {
        _showCreateAccountPrompt.value = null
        // State naturally falls back to defaultAccount via loadData flow
    }

    fun onAmountChange(value: String) {
        _state.update { state ->
            val newState = state.copy(amount = value)
            if (!state.isSplitMode) {
                val splitAmount = newState.evaluatedAmount?.let(::toEditableAmount) ?: value
                newState.copy(
                    splits = state.splits.map { it.copy(amount = splitAmount) }
                )
            } else {
                newState
            }
        }
    }

    fun toggleCategory(categoryId: String, categoryName: String) {
        _state.update { state ->
            val isDeselect = categoryId in state.selectedCategoryIds
            val newIds = if (isDeselect) emptySet() else setOf(categoryId)
            val newSplits = if (isDeselect) {
                listOf(SplitState(amount = state.amount))
            } else {
                listOf(
                    SplitState(
                        targetType = SplitTargetType.CATEGORY,
                        targetId = categoryId,
                        targetName = state.categories.find { it.id == categoryId }?.name ?: categoryName,
                        amount = state.amount
                    )
                )
            }
            state.copy(
                isSplitMode = false,
                selectedCategoryIds = newIds,
                splits = newSplits
            )
        }
    }

    fun toggleSplitMode() {
        _state.update { state ->
            if (state.isSplitMode) {
                val categoryIds = state.splits.filter { it.targetType == SplitTargetType.CATEGORY }.mapNotNull { it.targetId }.toSet()
                val newSplits = if (categoryIds.isEmpty()) {
                    listOf(SplitState(amount = state.amount))
                } else {
                    state.splits.filter { it.targetType == SplitTargetType.CATEGORY && it.targetId != null }.map { it.copy(amount = state.amount) }
                }
                state.copy(
                    isSplitMode = false,
                    selectedCategoryIds = categoryIds,
                    splits = newSplits
                )
            } else {
                val newSplits = if (state.splits.isEmpty()) listOf(SplitState()) else state.splits
                state.copy(
                    isSplitMode = true,
                    selectedCategoryIds = emptySet(),
                    splits = autoDivideAmounts(newSplits, state.totalAmount)
                )
            }
        }
    }

    fun addSplit() {
        _state.update { state -> 
            val newSplits = state.splits + SplitState(targetType = SplitTargetType.CATEGORY)
            state.copy(splits = autoDivideAmounts(newSplits, state.totalAmount)) 
        }
    }

    fun addPersonSplit() {
        _state.update { state -> 
            val newSplits = state.splits + SplitState(targetType = SplitTargetType.PERSON)
            state.copy(splits = autoDivideAmounts(newSplits, state.totalAmount)) 
        }
    }

    fun onCategorySelected(index: Int, categoryId: String, categoryName: String) {
        _state.update { state ->
            val newSplits = state.splits.toMutableList()
            if (index < newSplits.size) {
                newSplits[index] = newSplits[index].copy(targetId = categoryId, targetName = categoryName)
            }
            state.copy(splits = newSplits)
        }
    }

    fun onPersonSplitSelected(index: Int, accountId: String, accountName: String) {
        _state.update { state ->
            val newSplits = state.splits.toMutableList()
            if (index < newSplits.size) {
                newSplits[index] = newSplits[index].copy(targetId = accountId, targetName = accountName)
            }
            state.copy(splits = newSplits)
        }
    }
    
    fun onPersonAccountCreated(accountId: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _state.update { state ->
                val account = state.peopleAccounts.find { it.id == accountId } 
                           ?: state.accounts.find { it.id == accountId }
                val accountName = account?.name ?: "Unknown"
                
                val splits = state.splits.toMutableList()
                val targetIndex = splits.indexOfLast { it.targetType == SplitTargetType.PERSON && it.targetId == null }
                
                if (targetIndex != -1) {
                    splits[targetIndex] = splits[targetIndex].copy(targetId = accountId, targetName = accountName)
                } else {
                    val lastPersonIndex = splits.indexOfLast { it.targetType == SplitTargetType.PERSON }
                    if (lastPersonIndex != -1) {
                        splits[lastPersonIndex] = splits[lastPersonIndex].copy(targetId = accountId, targetName = accountName)
                    }
                }
                state.copy(splits = splits)
            }
        }
    }

    fun onSplitAmountChange(index: Int, value: String) {
        _state.update { state ->
            val newSplits = state.splits.toMutableList()
            if (index < newSplits.size) {
                newSplits[index] = newSplits[index].copy(amount = value)
            }
            state.copy(splits = newSplits)
        }
    }

    fun removeSplit(index: Int) {
        _state.update { state ->
            if (state.splits.size > 1) {
                val updatedSplits = state.splits.toMutableList().apply { removeAt(index) }
                state.copy(splits = autoDivideAmounts(updatedSplits, state.totalAmount))
            } else state
        }
    }

    fun onTypeChange(type: TransactionType) {
        _state.update { current ->
            if (type == current.type) return@update current
            current.copy(
                type = type,
                isSplitMode = false,
                selectedCategoryIds = emptySet(),
                splits = listOf(SplitState(amount = current.amount)),
                toAccountId = if (type == TransactionType.TRANSFER) current.toAccountId else null
            )
        }
    }

    fun onAccountSelected(accountId: String) { _state.update { it.copy(selectedAccountId = accountId) } }
    fun onToAccountSelected(accountId: String) { _state.update { it.copy(toAccountId = accountId) } }
    fun onNoteChange(value: String) { _state.update { it.copy(note = value) } }
    fun onDateChange(value: Long) { _state.update { it.copy(date = value) } }
    fun onRecurringToggle(isRecurring: Boolean) { _state.update { it.copy(isRecurring = isRecurring) } }
    fun onRecurringIntervalChange(interval: String) { _state.update { it.copy(recurringInterval = interval) } }
    fun onRecurringUnitChange(unit: RecurringUnit) { _state.update { it.copy(recurringUnit = unit) } }
    fun onRecurringEndDateChange(endDate: Long?) { _state.update { it.copy(recurringEndDate = endDate) } }
    fun onNotifyForRecurringEntriesChange(enabled: Boolean) { _state.update { it.copy(notifyForRecurringEntries = enabled) } }
    fun addReceiptUri(uri: String) { _state.update { state -> if (uri.isBlank() || uri in state.receiptUris) state else state.copy(receiptUris = state.receiptUris + uri) } }
    fun removeReceiptUri(uri: String) { _state.update { state -> state.copy(receiptUris = state.receiptUris.filterNot { it == uri }) } }
    fun clearAllReceipts() { _state.update { it.copy(receiptUris = emptyList()) } }

    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            val type = _state.value.type
            val categoryType = if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
            val category = com.moneytracker.app.domain.model.Category(
                id = UUID.randomUUID().toString(),
                name = name,
                iconKey = iconKey,
                type = categoryType,
                colorHex = colorHex
            )
            categoryRepository.saveCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            _state.update { state ->
                val updatedSelectedIds = state.selectedCategoryIds - categoryId
                val updatedSplits = state.splits.filterNot { it.targetId == categoryId }
                val normalizedSplits = if (updatedSplits.isEmpty()) listOf(SplitState(amount = state.amount)) else updatedSplits
                state.copy(selectedCategoryIds = updatedSelectedIds, splits = normalizedSplits)
            }
        }
    }

    fun editCategory(categoryId: String, newName: String, newIconKey: String, newColorHex: String) {
        viewModelScope.launch {
            try {
                val existing = categoryRepository.getCategoryById(categoryId) ?: return@launch
                val updated = existing.copy(name = newName, iconKey = newIconKey, colorHex = newColorHex)
                categoryRepository.updateCategory(updated)
                
                _state.update { state ->
                    val newSplits = state.splits.map { split ->
                        if (split.targetId == categoryId) split.copy(targetName = newName) else split
                    }
                    state.copy(splits = newSplits)
                }
            } catch (e: Exception) {
                android.util.Log.e("AddTransactionVM", "Failed to edit category", e)
            }
        }
    }

    fun saveTransaction() {
        val currentState = _state.value
        if (currentState.isSaving) return
        if (!currentState.isValid) {
            _state.update { it.copy(errorMessage = "Please fill all required fields") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            try {
                val now = System.currentTimeMillis()
                val isSeriesChildEntry = currentState.parentRecurringId != null
                val shouldPersistRecurringOnThisEntry = currentState.isRecurring && !isSeriesChildEntry
                val finalPayee = currentState.payee.ifBlank { currentState.splits.firstOrNull()?.targetName ?: "Transaction" }

                // --- If in split mode, process EVERY split item as an independent TransactionEntity ---
                if (currentState.isSplitMode) {
                    val validSplits = currentState.splits.filter { it.targetId != null }
                    
                    validSplits.forEachIndexed { index, split ->
                        val amount = evaluateAmountExpression(split.amount) ?: return@forEachIndexed
                        if (amount <= 0.0) return@forEachIndexed
                        
                        val isFirst = index == 0
                        val transactionId = if (currentState.isEditMode && isFirst) {
                            currentState.editTransactionId!!
                        } else {
                            UUID.randomUUID().toString()
                        }
                        
                        val isThisRecurring = isFirst && shouldPersistRecurringOnThisEntry

                        if (split.targetType == SplitTargetType.CATEGORY) {
                            val transaction = TransactionEntity(
                                id = transactionId,
                                accountId = currentState.selectedAccountId!!,
                                payee = finalPayee,
                                note = currentState.note.ifBlank { null },
                                date = currentState.date,
                                totalAmount = amount,
                                type = currentState.type,
                                toAccountId = null,
                                createdAt = now,
                                modifiedAt = now,
                                syncStatus = SyncStatus.DIRTY,
                                isRecurring = isThisRecurring,
                                recurringInterval = if (isThisRecurring) currentState.recurringInterval.toIntOrNull() else null,
                                recurringUnit = if (isThisRecurring) currentState.recurringUnit else null,
                                recurringEndDate = if (isThisRecurring) currentState.recurringEndDate else null,
                                parentRecurringId = if (isFirst) currentState.parentRecurringId else null,
                                notifyForRecurringEntries = if (isThisRecurring) currentState.notifyForRecurringEntries else true,
                                receiptUri = encodeReceiptUris(currentState.receiptUris)
                            )
                            
                            val dbSplits = listOf(
                                TransactionSplitEntity(
                                    id = UUID.randomUUID().toString(),
                                    transactionId = transactionId,
                                    categoryId = split.targetId!!,
                                    amount = amount,
                                    note = null
                                )
                            )

                            if (currentState.isEditMode && isFirst) {
                                transactionRepository.updateTransactionFull(transaction, dbSplits)
                                val wasParentTurnedOff = currentState.parentRecurringId == null && !currentState.isRecurring
                                val wasChildTurnedOff = currentState.parentRecurringId != null && !currentState.isRecurring
                                if (wasParentTurnedOff) transactionRepository.stopRecurringSeries(transactionId)
                                else if (wasChildTurnedOff) currentState.parentRecurringId?.let { parentId -> transactionRepository.stopRecurringSeries(parentId) }
                            } else {
                                transactionRepository.saveTransaction(transaction, dbSplits)
                            }
                            
                            categoryRecommendationRepository.upsertRecommendation(
                                finalPayee, currentState.type, split.targetId!!, currentState.selectedAccountId
                            )
                            
                            if (currentState.type == TransactionType.EXPENSE) {
                                budgetAlertManager.checkBudgets(currentState.date, mapOf(split.targetId!! to split.targetName))
                            }

                        } else if (split.targetType == SplitTargetType.PERSON) {
                            val transferTransaction = TransactionEntity(
                                id = transactionId,
                                accountId = currentState.selectedAccountId!!, 
                                payee = "Split with ${split.targetName}",
                                note = currentState.note.ifBlank { null },
                                date = currentState.date,
                                totalAmount = amount,
                                type = TransactionType.TRANSFER, 
                                toAccountId = split.targetId!!, 
                                createdAt = now,
                                modifiedAt = now,
                                syncStatus = SyncStatus.DIRTY,
                                isRecurring = isThisRecurring, 
                                recurringInterval = if (isThisRecurring) currentState.recurringInterval.toIntOrNull() else null,
                                recurringUnit = if (isThisRecurring) currentState.recurringUnit else null,
                                recurringEndDate = if (isThisRecurring) currentState.recurringEndDate else null,
                                parentRecurringId = if (isFirst) currentState.parentRecurringId else null,
                                notifyForRecurringEntries = if (isThisRecurring) currentState.notifyForRecurringEntries else true,
                                receiptUri = encodeReceiptUris(currentState.receiptUris)
                            )
                            
                            if (currentState.isEditMode && isFirst) {
                                transactionRepository.updateTransactionFull(transferTransaction, emptyList())
                                val wasParentTurnedOff = currentState.parentRecurringId == null && !currentState.isRecurring
                                val wasChildTurnedOff = currentState.parentRecurringId != null && !currentState.isRecurring
                                if (wasParentTurnedOff) transactionRepository.stopRecurringSeries(transactionId)
                                else if (wasChildTurnedOff) currentState.parentRecurringId?.let { parentId -> transactionRepository.stopRecurringSeries(parentId) }
                            } else {
                                transactionRepository.saveTransaction(transferTransaction, emptyList())
                            }
                        }
                    }
                    
                    if (shouldPersistRecurringOnThisEntry) recurringTransactionManager.checkNow()

                } else {
                    val transactionId = currentState.editTransactionId ?: UUID.randomUUID().toString()
                    val transaction = TransactionEntity(
                        id = transactionId,
                        accountId = currentState.selectedAccountId!!,
                        payee = finalPayee,
                        note = currentState.note.ifBlank { null },
                        date = currentState.date,
                        totalAmount = currentState.totalAmount,
                        type = currentState.type,
                        toAccountId = currentState.toAccountId,
                        createdAt = now,
                        modifiedAt = now,
                        syncStatus = SyncStatus.DIRTY,
                        isRecurring = shouldPersistRecurringOnThisEntry,
                        recurringInterval = if (shouldPersistRecurringOnThisEntry) currentState.recurringInterval.toIntOrNull() else null,
                        recurringUnit = if (shouldPersistRecurringOnThisEntry) currentState.recurringUnit else null,
                        recurringEndDate = if (shouldPersistRecurringOnThisEntry) currentState.recurringEndDate else null,
                        parentRecurringId = currentState.parentRecurringId,
                        notifyForRecurringEntries = if (shouldPersistRecurringOnThisEntry) currentState.notifyForRecurringEntries else true,
                        receiptUri = encodeReceiptUris(currentState.receiptUris)
                    )

                    val dbSplits = if (currentState.type == TransactionType.TRANSFER || currentState.selectedCategoryIds.isEmpty()) {
                        emptyList()
                    } else {
                        val amountPerCategory = currentState.totalAmount / currentState.selectedCategoryIds.size
                        currentState.selectedCategoryIds.map { categoryId ->
                            TransactionSplitEntity(
                                id = UUID.randomUUID().toString(),
                                transactionId = transactionId,
                                categoryId = categoryId,
                                amount = amountPerCategory,
                                note = null
                            )
                        }
                    }

                    if (currentState.isEditMode) {
                        transactionRepository.updateTransactionFull(transaction, dbSplits)
                        val wasParentTurnedOff = currentState.parentRecurringId == null && !currentState.isRecurring
                        val wasChildTurnedOff = currentState.parentRecurringId != null && !currentState.isRecurring
                        if (wasParentTurnedOff) transactionRepository.stopRecurringSeries(transactionId)
                        else if (wasChildTurnedOff) currentState.parentRecurringId?.let { parentId -> transactionRepository.stopRecurringSeries(parentId) }
                    } else {
                        transactionRepository.saveTransaction(transaction, dbSplits)
                        if (shouldPersistRecurringOnThisEntry) recurringTransactionManager.checkNow()
                    }
                    
                    if (currentState.payee.isNotBlank() && dbSplits.isNotEmpty()) {
                        categoryRecommendationRepository.upsertRecommendation(
                            currentState.payee, currentState.type, dbSplits.first().categoryId, currentState.selectedAccountId
                        )
                    }

                    if (currentState.type == TransactionType.EXPENSE && dbSplits.isNotEmpty()) {
                        val categoryMap = dbSplits.associate { it.categoryId to (currentState.categories.find { cat -> cat.id == it.categoryId }?.name ?: "") }
                        budgetAlertManager.checkBudgets(currentState.date, categoryMap)
                    }
                }

                _state.update { it.copy(isSaving = false) }
                _navigateBack.trySend(Unit)
            } catch (e: Exception) {
                android.util.Log.e("AddTxnVM", "Save failed", e)
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save transaction") }
            }
        }
    }

    fun deleteTransaction() {
        val transactionId = _state.value.editTransactionId ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
            _navigateBack.trySend(Unit)
        }
    }
    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
