package com.moneytracker.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.domain.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsState(
    val cashAccounts: List<Account> = emptyList(),
    val walletAccounts: List<Account> = emptyList(),
    val bankAccounts: List<Account> = emptyList(),
    val investmentAccounts: List<Account> = emptyList(),
    val peopleAccounts: List<Account> = emptyList(),
    // customSections holds grouped custom account types by their customTypeName
    val customSections: List<CustomSection> = emptyList(),
    val inactiveAccounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val cashTotal: Double = 0.0,
    val walletTotal: Double = 0.0,
    val bankTotal: Double = 0.0,
    val investmentTotal: Double = 0.0,
    val peopleTotal: Double = 0.0,
    val peoplePositiveTotal: Double = 0.0, // Loaned
    val peopleNegativeTotal: Double = 0.0, // Borrowed
    val customTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val cashExpanded: Boolean = true,
    val walletExpanded: Boolean = true,
    val bankExpanded: Boolean = true,
    val investmentExpanded: Boolean = true,
    val peopleExpanded: Boolean = true,
    val customExpanded: Boolean = true
)

data class CustomSection(
    val name: String,
    val accounts: List<Account>,
    val total: Double,
    val expanded: Boolean
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        loadAccounts()
        loadInactiveAccounts()
        loadTotals()
        observeSectionPreferences()
    }

    private fun observeSectionPreferences() {
        viewModelScope.launch {
            userPreferences.expandedCash.collect { expanded ->
                _state.update { it.copy(cashExpanded = expanded) }
            }
        }
        viewModelScope.launch {
            userPreferences.expandedWallet.collect { expanded ->
                _state.update { it.copy(walletExpanded = expanded) }
            }
        }
        viewModelScope.launch {
            userPreferences.expandedBank.collect { expanded ->
                _state.update { it.copy(bankExpanded = expanded) }
            }
        }
        viewModelScope.launch {
            userPreferences.expandedInvestment.collect { expanded ->
                _state.update { it.copy(investmentExpanded = expanded) }
            }
        }
        viewModelScope.launch {
            userPreferences.expandedPeople.collect { expanded ->
                _state.update { it.copy(peopleExpanded = expanded) }
            }
        }
        viewModelScope.launch {
            userPreferences.expandedCustom.collect { expanded ->
                _state.update { it.copy(customExpanded = expanded) }
            }
        }
    }

    fun setSectionExpanded(type: AccountType, expanded: Boolean) {
        viewModelScope.launch {
            when (type) {
                AccountType.CASH -> userPreferences.setExpandedCash(expanded)
                AccountType.WALLET -> userPreferences.setExpandedWallet(expanded)
                AccountType.BANK -> userPreferences.setExpandedBank(expanded)
                AccountType.INVESTMENT -> userPreferences.setExpandedInvestment(expanded)
                AccountType.PEOPLE -> userPreferences.setExpandedPeople(expanded)
                AccountType.CUSTOM -> userPreferences.setExpandedCustom(expanded)
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                val cashAccounts = accounts.filter { a -> a.type == AccountType.CASH }
                val walletAccounts = accounts.filter { a -> a.type == AccountType.WALLET }
                val bankAccounts = accounts.filter { a -> a.type == AccountType.BANK }
                val investmentAccounts = accounts.filter { a -> a.type == AccountType.INVESTMENT }
                val peopleAccounts = accounts.filter { a -> a.type == AccountType.PEOPLE }
                val customAccounts = accounts.filter { a -> a.type == AccountType.CUSTOM }

                val cashTotal = cashAccounts.sumOf { it.currentBalance }
                val walletTotal = walletAccounts.sumOf { it.currentBalance }
                val bankTotal = bankAccounts.sumOf { it.currentBalance }
                val investmentTotal = investmentAccounts.sumOf { it.currentBalance }
                val peopleTotal = peopleAccounts.sumOf { it.currentBalance }
                val peoplePositiveTotal = peopleAccounts.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
                val peopleNegativeTotal = peopleAccounts.filter { it.currentBalance < 0 }.sumOf { it.currentBalance }
                val customTotal = customAccounts.sumOf { it.currentBalance }

                // Group custom accounts by their customTypeName (use "Custom" when blank)
                val grouped = customAccounts.groupBy { it.customTypeName?.takeIf { it.isNotBlank() } ?: "Custom" }
                val customSections = grouped.map { (name, list) ->
                    // read saved expanded state for this custom group
                    val expanded = try {
                        userPreferences.expandedForCustom(name).first()
                    } catch (e: Exception) {
                        true
                    }
                    CustomSection(
                        name = name,
                        accounts = list,
                        total = list.sumOf { it.currentBalance },
                        expanded = expanded
                    )
                }

                _state.update {
                    it.copy(
                        cashAccounts = cashAccounts,
                        walletAccounts = walletAccounts,
                        bankAccounts = bankAccounts,
                        investmentAccounts = investmentAccounts,
                        peopleAccounts = peopleAccounts,
                        customSections = customSections,
                        totalBalance = accounts.sumOf { a -> a.currentBalance },
                        cashTotal = cashTotal,
                        walletTotal = walletTotal,
                        bankTotal = bankTotal,
                        investmentTotal = investmentTotal,
                        peopleTotal = peopleTotal,
                        peoplePositiveTotal = peoplePositiveTotal,
                        peopleNegativeTotal = peopleNegativeTotal,
                        customTotal = customTotal,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleCustomSection(name: String) {
        viewModelScope.launch {
            // find current expanded state
            val current = _state.value.customSections.find { it.name == name }?.expanded ?: true
            val next = !current
            userPreferences.setExpandedForCustom(name, next)
            // update in-memory state
            _state.update { st ->
                st.copy(customSections = st.customSections.map {
                    if (it.name == name) it.copy(expanded = next) else it
                })
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
