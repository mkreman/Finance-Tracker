package com.moneytracker.app.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.repository.TransactionRepository
import com.moneytracker.app.domain.model.TransactionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AccountTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val accountId: String = savedStateHandle.get<String>("accountId") ?: ""
    val accountName: String = savedStateHandle.get<String>("accountName") ?: "Account"

    val transactions: StateFlow<List<TransactionListItem>> =
        transactionRepository.getTransactionsByAccount(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
