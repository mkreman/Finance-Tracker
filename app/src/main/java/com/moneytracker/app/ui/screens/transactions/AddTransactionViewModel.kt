package com.moneytracker.app.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.data.local.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferences: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    // One-shot navigation event to avoid crash on re-entry
    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    private val editTransactionId: String? = savedStateHandle.get<String>("transactionId")
    private val initialType: String? = savedStateHandle.get<String>("type")

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
    }

    private fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId) ?: return@launch
            // Determine if this is multi-tag (same amount per category = totalAmount) vs true split
            val isMultiTag = transaction.splits.size > 1 &&
                    transaction.splits.all { kotlin.math.abs(it.amount - transaction.totalAmount) < 0.01 }
            val isSplit = transaction.splits.size > 1 && !isMultiTag

            _state.update { state ->
                state.copy(
                    isEditMode = true,
                    editTransactionId = transactionId,
                    type = transaction.type,
                    amount = transaction.totalAmount.toLong().toString(),
                    selectedAccountId = transaction.accountId,
                    toAccountId = transaction.toAccountId,
                    note = transaction.note ?: "",
                    date = transaction.date,
                    isSplitMode = isSplit,
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
                                amount = split.amount.toLong().toString()
                            )
                        }
                    }
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Get default account preference
            val defaultAccountId = userPreferences.defaultAccountId.first()

            accountRepository.getAllAccounts().collect { accounts ->
                _state.update { currentState ->
                    val accountId = when {
                        currentState.isEditMode -> currentState.selectedAccountId
                        currentState.selectedAccountId != null -> currentState.selectedAccountId
                        defaultAccountId != null && accounts.any { it.id == defaultAccountId } -> defaultAccountId
                        else -> accounts.firstOrNull()?.id
                    }
                    currentState.copy(
                        accounts = accounts,
                        selectedAccountId = accountId
                    )
                }
            }
        }
        viewModelScope.launch {
            _state.map { it.type }.distinctUntilChanged().collectLatest { type ->
                categoryRepository.getCategoriesByType(
                    if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
                ).collect { categories ->
                    _state.update { it.copy(categories = categories) }
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _state.update { state ->
            val newState = state.copy(amount = value)
            if (!state.isSplitMode) {
                // In single/multi-category mode, each split gets the full amount
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
            // Rebuild splits — one per selected category, each with full amount
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
        _state.update { it.copy(type = type) }
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
                // Going back to single/multi mode — keep categories, set each split amount to full amount
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

    fun addCategory(name: String, iconKey: String) {
        viewModelScope.launch {
            val type = _state.value.type
            val categoryType = if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type
            // Generate a random color for the new category
            val colors = listOf("#FF5722", "#2196F3", "#9C27B0", "#E91E63", "#4CAF50", "#3F51B5", "#FF9800", "#795548", "#607D8B", "#00BCD4")
            val colorHex = colors.random()
            val category = com.moneytracker.app.domain.model.Category(
                id = UUID.randomUUID().toString(),
                name = name,
                iconKey = iconKey,
                type = categoryType,
                colorHex = colorHex
            )
            categoryRepository.saveCategory(category)
            // The category list will auto-update via Flow
        }
    }

    fun removeSplit(index: Int) {
        _state.update { state ->
            if (state.splits.size > 1) {
                state.copy(splits = state.splits.toMutableList().apply { removeAt(index) })
            } else state
        }
    }

    fun saveTransaction() {
        val currentState = _state.value
        if (!currentState.isValid) {
            _state.update { it.copy(errorMessage = "Please fill all required fields") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            try {
                val transactionId = currentState.editTransactionId ?: UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val transaction = TransactionEntity(
                    id = transactionId,
                    accountId = currentState.selectedAccountId!!,
                    payee = currentState.splits.firstOrNull()?.categoryName ?: "Transaction",
                    note = currentState.note.ifBlank { null },
                    date = currentState.date,
                    totalAmount = currentState.totalAmount,
                    type = currentState.type,
                    toAccountId = currentState.toAccountId,
                    createdAt = now,
                    modifiedAt = now,
                    syncStatus = SyncStatus.DIRTY
                )

                val splits = if (currentState.type == TransactionType.TRANSFER) {
                    emptyList()
                } else {
                    currentState.splits.map { split ->
                        TransactionSplitEntity(
                            id = UUID.randomUUID().toString(),
                            transactionId = transactionId,
                            categoryId = split.categoryId!!,
                            amount = split.amount.toDoubleOrNull() ?: 0.0,
                            note = null
                        )
                    }
                }

                if (currentState.isEditMode) {
                    transactionRepository.updateTransactionFull(transaction, splits)
                } else {
                    transactionRepository.saveTransaction(transaction, splits)
                }
                _state.update { it.copy(isSaving = false) }
                _navigateBack.trySend(Unit)
            } catch (e: Exception) {
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
