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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun toEditableAmount(value: Double): String {
        return BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .toPlainString()
    }

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    private val editTransactionId: String? = savedStateHandle.get<String>("transactionId")
    private val initialType: String? = savedStateHandle.get<String>("type")
    private val initialAmount: String? = savedStateHandle.get<String>("amount")
    private val initialNote: String? = savedStateHandle.get<String>("note")
    private val initialPayee: String? = savedStateHandle.get<String>("payee")
    private val suggestedCategoryId: String? = savedStateHandle.get<String>("suggestedCategoryId")

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
            val isMultiTag = transaction.splits.size > 1 &&
                    transaction.splits.all { kotlin.math.abs(it.amount - transaction.totalAmount) < 0.01 }
            val isSplit = transaction.splits.size > 1 && !isMultiTag

            _state.update { state ->
                state.copy(
                    isEditMode = true,
                    editTransactionId = transactionId,
                    type = transaction.type,
                    amount = toEditableAmount(transaction.totalAmount),
                    selectedAccountId = transaction.accountId,
                    toAccountId = transaction.toAccountId,
                    note = transaction.note ?: "",
                    payee = transaction.payee, // Preserve payee
                    date = transaction.date,
                    isSplitMode = isSplit,
                    parentRecurringId = transaction.parentRecurringId,
                    isRecurring = recurrenceSource?.isRecurring ?: false,
                    recurringInterval = recurrenceSource?.recurringInterval?.toString() ?: "1",
                    recurringUnit = recurrenceSource?.recurringUnit ?: RecurringUnit.MONTH,
                    recurringEndDate = recurrenceSource?.recurringEndDate,
                    notifyForRecurringEntries = recurrenceSource?.notifyForRecurringEntries ?: transaction.notifyForRecurringEntries,
                    selectedCategoryIds = if (!isSplit) {
                        transaction.splits.mapNotNull { it.categoryId.takeIf { id -> id.isNotEmpty() } }.toSet()
                    } else {
                        emptySet()
                    },
                    splits = if (transaction.splits.isEmpty()) {
                        listOf(SplitState())
                    } else {
                        transaction.splits.map { split ->
                            SplitState(
                                categoryId = split.categoryId,
                                categoryName = split.categoryName,
                                amount = toEditableAmount(split.amount)
                            )
                        }
                    }
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val defaultAccountId = userPreferences.defaultAccountId.first()

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

                _state.update { currentState ->
                    val accountId = when {
                        currentState.isEditMode -> currentState.selectedAccountId
                        currentState.selectedAccountId != null -> currentState.selectedAccountId
                        defaultAccountId != null && sortedAccounts.any { it.id == defaultAccountId } -> defaultAccountId
                        else -> sortedAccounts.firstOrNull()?.id
                    }
                    currentState.copy(
                        accounts = sortedAccounts,
                        selectedAccountId = accountId
                    )
                }
            }
        }
        viewModelScope.launch {
            userPreferences.defaultNotifyForRecurringEntries.collect { enabled ->
                _state.update { currentState ->
                    if (currentState.isEditMode) currentState
                    else currentState.copy(notifyForRecurringEntries = enabled)
                }
            }
        }
        viewModelScope.launch {
            _state.map { it.type }.distinctUntilChanged().collectLatest { type ->
                categoryRepository.getCategoriesByType(
                    if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
                ).collect { categories ->
                    _state.update { state ->
                        var newSelectedIds = state.selectedCategoryIds
                        var newSplits = state.splits
                        
                        // Auto-select the smart suggested category!
                        if (suggestedCategoryId != null && newSelectedIds.isEmpty() && !state.isEditMode) {
                            val cat = categories.find { it.id == suggestedCategoryId }
                            if (cat != null) {
                                newSelectedIds = setOf(cat.id)
                                newSplits = listOf(SplitState(categoryId = cat.id, categoryName = cat.name, amount = state.amount))
                            }
                        }

                        state.copy(
                            categories = categories,
                            selectedCategoryIds = newSelectedIds,
                            splits = newSplits
                        )
                    }
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _state.update { state ->
            val newState = state.copy(amount = value)
            if (!state.isSplitMode) {
                newState.copy(
                    splits = state.splits.map { it.copy(amount = value) }
                )
            } else {
                newState
            }
        }
    }

    fun toggleCategory(categoryId: String, categoryName: String) {
        _state.update { state ->
            val newIds = state.selectedCategoryIds.toMutableSet()
            if (categoryId in newIds) {
                newIds.remove(categoryId)
            } else {
                newIds.add(categoryId)
            }
            val newSplits = if (newIds.isEmpty()) {
                listOf(SplitState())
            } else {
                newIds.map { id ->
                    val name = state.categories.find { it.id == id }?.name ?: categoryName
                    SplitState(
                        categoryId = id,
                        categoryName = name,
                        amount = state.amount
                    )
                }
            }
            state.copy(
                selectedCategoryIds = newIds,
                splits = newSplits
            )
        }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun onDateChange(value: Long) {
        _state.update { it.copy(date = value) }
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

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onToAccountSelected(accountId: String) {
        _state.update { it.copy(toAccountId = accountId) }
    }

    fun onCategorySelected(index: Int, categoryId: String, categoryName: String) {
        _state.update { state ->
            val newSplits = state.splits.toMutableList()
            if (index < newSplits.size) {
                newSplits[index] = newSplits[index].copy(
                    categoryId = categoryId,
                    categoryName = categoryName
                )
            }
            state.copy(splits = newSplits)
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

    fun toggleSplitMode() {
        _state.update { state ->
            if (state.isSplitMode) {
                val categoryIds = state.splits.mapNotNull { it.categoryId }.toSet()
                val newSplits = if (categoryIds.isEmpty()) {
                    listOf(SplitState(amount = state.amount))
                } else {
                    state.splits.filter { it.categoryId != null }.map { it.copy(amount = state.amount) }
                }
                state.copy(
                    isSplitMode = false,
                    selectedCategoryIds = categoryIds,
                    splits = newSplits
                )
            } else {
                state.copy(
                    isSplitMode = true,
                    selectedCategoryIds = emptySet()
                )
            }
        }
    }

    fun addSplit() {
        _state.update { state ->
            state.copy(splits = state.splits + SplitState())
        }
    }

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
                val updatedSplits = state.splits.filterNot { it.categoryId == categoryId }
                val normalizedSplits = if (updatedSplits.isEmpty()) {
                    listOf(SplitState(amount = state.amount))
                } else {
                    updatedSplits
                }
                state.copy(
                    selectedCategoryIds = updatedSelectedIds,
                    splits = normalizedSplits
                )
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
                        if (split.categoryId == categoryId) {
                            split.copy(categoryName = newName)
                        } else split
                    }
                    state.copy(splits = newSplits)
                }
            } catch (e: Exception) {
                android.util.Log.e("AddTransactionVM", "Failed to edit category", e)
            }
        }
    }

    fun removeSplit(index: Int) {
        _state.update { state ->
            if (state.splits.size > 1) {
                state.copy(splits = state.splits.toMutableList().apply { removeAt(index) })
            } else state
        }
    }

    fun onRecurringToggle(isRecurring: Boolean) {
        _state.update { current ->
            current.copy(isRecurring = isRecurring)
        }
    }

    fun onRecurringIntervalChange(interval: String) {
        _state.update { it.copy(recurringInterval = interval) }
    }

    fun onRecurringUnitChange(unit: RecurringUnit) {
        _state.update { it.copy(recurringUnit = unit) }
    }

    fun onRecurringEndDateChange(endDate: Long?) {
        _state.update { it.copy(recurringEndDate = endDate) }
    }

    fun onNotifyForRecurringEntriesChange(enabled: Boolean) {
        _state.update { it.copy(notifyForRecurringEntries = enabled) }
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
                val transactionId = currentState.editTransactionId ?: UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val existingTransaction = if (currentState.isEditMode) {
                    transactionRepository.getTransactionById(transactionId)
                } else {
                    null
                }
                val isSeriesChildEntry = currentState.parentRecurringId != null
                val shouldPersistRecurringOnThisEntry = currentState.isRecurring && !isSeriesChildEntry

                // If payee is blank (manual entry without a typed payee), use the primary category name
                val finalPayee = currentState.payee.ifBlank { currentState.splits.firstOrNull()?.categoryName ?: "Transaction" }

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
                    parentRecurringId = currentState.parentRecurringId ?: existingTransaction?.parentRecurringId,
                    notifyForRecurringEntries = if (shouldPersistRecurringOnThisEntry) currentState.notifyForRecurringEntries else true
                )

                val splits = if (currentState.type == TransactionType.TRANSFER) {
                    emptyList()
                } else {
                    val validSplits = currentState.splits.mapNotNull { split ->
                        val categoryId = split.categoryId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val splitAmount = split.amount.toDoubleOrNull() ?: 0.0
                        if (splitAmount <= 0.0) return@mapNotNull null

                        TransactionSplitEntity(
                            id = UUID.randomUUID().toString(),
                            transactionId = transactionId,
                            categoryId = categoryId,
                            amount = splitAmount,
                            note = null
                        )
                    }

                    if (validSplits.isEmpty() && existingTransaction != null && existingTransaction.splits.isNotEmpty()) {
                        existingTransaction.splits.map { existing ->
                            TransactionSplitEntity(
                                id = existing.id,
                                transactionId = transactionId,
                                categoryId = existing.categoryId,
                                amount = existing.amount,
                                note = existing.note
                            )
                        }
                    } else if (validSplits.isEmpty()) {
                        throw IllegalStateException("Please select at least one valid category")
                    } else {
                        validSplits
                    }
                }

                if (currentState.isSplitMode && splits.isNotEmpty()) {
                    val splitTotal = splits.sumOf { it.amount }
                    if (kotlin.math.abs(splitTotal - currentState.totalAmount) >= 0.01) {
                        throw IllegalStateException("Split total must match the transaction amount")
                    }
                }

                if (currentState.isEditMode) {
                    transactionRepository.updateTransactionFull(transaction, splits)
                    
                    val wasParentTurnedOff = currentState.parentRecurringId == null && 
                                             existingTransaction?.isRecurring == true && 
                                             !currentState.isRecurring
                                             
                    val wasChildTurnedOff = currentState.parentRecurringId != null && 
                                            !currentState.isRecurring

                    if (wasParentTurnedOff) {
                        transactionRepository.stopRecurringSeries(transactionId)
                    } else if (wasChildTurnedOff) {
                        currentState.parentRecurringId?.let { parentId ->
                            transactionRepository.stopRecurringSeries(parentId)
                        }
                    }
                } else {
                    transactionRepository.saveTransaction(transaction, splits)
                    if (shouldPersistRecurringOnThisEntry) {
                        recurringTransactionManager.checkNow()
                    }
                }

                // Train the recommendation algorithm!
                // If a real payee exists, and a valid category is selected, upsert it using Payee + Type.
                if (currentState.payee.isNotBlank() && splits.isNotEmpty()) {
                    categoryRecommendationRepository.upsertRecommendation(
                        currentState.payee, 
                        currentState.type, 
                        splits.first().categoryId
                    )
                }

                _state.update { it.copy(isSaving = false) }
                _navigateBack.trySend(Unit)
            } catch (e: Exception) {
                android.util.Log.e("AddTxnVM", "Save failed", e)
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save transaction"
                    )
                }
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

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
