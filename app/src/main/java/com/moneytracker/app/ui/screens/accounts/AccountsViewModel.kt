package com.moneytracker.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.domain.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsState(
    val cashAccounts: List<Account> = emptyList(),
    val bankAccounts: List<Account> = emptyList(),
    val investmentAccounts: List<Account> = emptyList(),
    val peopleAccounts: List<Account> = emptyList(),
    val inactiveAccounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        loadAccounts()
        loadInactiveAccounts()
        loadTotals()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _state.update {
                    it.copy(
                        cashAccounts = accounts.filter { a -> a.type == AccountType.CASH || a.type == AccountType.WALLET },
                        bankAccounts = accounts.filter { a -> a.type == AccountType.BANK },
                        investmentAccounts = accounts.filter { a -> a.type == AccountType.INVESTMENT },
                        peopleAccounts = accounts.filter { a -> a.type == AccountType.PEOPLE },
                        totalBalance = accounts.sumOf { a -> a.currentBalance },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadInactiveAccounts() {
        viewModelScope.launch {
            accountRepository.getInactiveAccounts().collect { accounts ->
                _state.update { it.copy(inactiveAccounts = accounts) }
            }
        }
    }

    private fun loadTotals() {
        viewModelScope.launch {
            accountRepository.getTotalIncomeAllTime().collect { income ->
                _state.update { it.copy(totalIncome = income) }
            }
        }
        viewModelScope.launch {
            accountRepository.getTotalExpenseAllTime().collect { expense ->
                _state.update { it.copy(totalExpense = expense) }
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            accountRepository.hardDeleteAccount(id)
        }
    }

    fun deactivateAccount(id: String) {
        viewModelScope.launch {
            accountRepository.deactivateAccount(id)
        }
    }

    fun activateAccount(id: String) {
        viewModelScope.launch {
            accountRepository.activateAccount(id)
        }
    }
}
