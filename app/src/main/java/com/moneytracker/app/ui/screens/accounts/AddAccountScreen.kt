package com.moneytracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.data.local.repository.AccountRepository
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.parseHexColor
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class AddAccountState(
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val customTypeName: String = "",
    val initialBalance: String = "0",
    val colorHex: String = "#2196F3",
    val iconKey: String = "bank",
    val isEditMode: Boolean = false,
    val editAccountId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedAccountId: String? = null
)

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddAccountState())
    val state: StateFlow<AddAccountState> = _state.asStateFlow()

    init {
        val accountId = savedStateHandle.get<String>("accountId")
        val preSelectedType = savedStateHandle.get<String>("accountType")
        
        if (accountId != null) {
            loadAccount(accountId)
        } else if (preSelectedType != null) {
            val type = runCatching { AccountType.valueOf(preSelectedType) }.getOrNull()
            if (type != null) {
                val icon = if (type == AccountType.PEOPLE) "person" else "bank"
                _state.update { it.copy(type = type, iconKey = icon) }
            }
        }
    }

    private fun toEditableAmount(value: Double): String {
        return BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun loadAccount(accountId: String) {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().firstOrNull() ?: return@launch
            val account = accounts.find { it.id == accountId } ?: return@launch
            _state.update {
                it.copy(
                    name = account.name,
                    type = account.type,
                    customTypeName = account.customTypeName ?: "",
                    initialBalance = toEditableAmount(account.initialBalance),
                    colorHex = account.colorHex,
                    iconKey = account.iconKey,
                    isEditMode = true,
                    editAccountId = accountId
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun onTypeChange(type: AccountType) {
        _state.update { it.copy(type = type) }
    }

    fun onCustomTypeNameChange(name: String) {
        _state.update { it.copy(customTypeName = name) }
    }

    fun onBalanceChange(balance: String) {
        _state.update { it.copy(initialBalance = balance) }
    }

    fun onColorChange(color: String) {
        _state.update { it.copy(colorHex = color) }
    }

    fun onIconChange(iconKey: String) {
        _state.update { it.copy(iconKey = iconKey) }
    }

    fun save() {
        val current = _state.value
        val normalizedName = current.name.trim()
        if (normalizedName.isBlank()) return
        if (current.type == AccountType.CUSTOM && current.customTypeName.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val balance = current.initialBalance.toDoubleOrNull() ?: 0.0
            val existingSameName = accountRepository.getAllAccountsIncludingInactiveOnce()
                .firstOrNull {
                    it.id != current.editAccountId &&
                        it.name.trim().equals(normalizedName, ignoreCase = true)
                }

            val savedAccountId = if (current.isEditMode && current.editAccountId != null) {
                accountRepository.updateAccount(
                    Account(
                        id = current.editAccountId,
                        name = normalizedName,
                        type = current.type,
                        customTypeName = if (current.type == AccountType.CUSTOM) current.customTypeName else null,
                        initialBalance = balance,
                        currentBalance = balance,
                        colorHex = current.colorHex,
                        iconKey = current.iconKey
                    )
                )
                current.editAccountId
            } else if (existingSameName != null) {
                val balanceDiff = balance - existingSameName.initialBalance
                accountRepository.saveAccount(
                    existingSameName.copy(
                        name = normalizedName,
                        type = current.type,
                        customTypeName = if (current.type == AccountType.CUSTOM) current.customTypeName else null,
                        initialBalance = balance,
                        currentBalance = existingSameName.currentBalance + balanceDiff,
                        colorHex = current.colorHex,
                        iconKey = current.iconKey,
                        isActive = true,
                        isDeleted = false
                    )
                )
                existingSameName.id
            } else {
                val newId = UUID.randomUUID().toString()
                accountRepository.saveAccount(
                    Account(
                        id = newId,
                        name = normalizedName,
                        type = current.type,
                        customTypeName = if (current.type == AccountType.CUSTOM) current.customTypeName else null,
                        initialBalance = balance,
                        currentBalance = balance,
                        colorHex = current.colorHex,
                        iconKey = current.iconKey
                    )
                )
                newId
            }
            _state.update { it.copy(isSaving = false, isSaved = true, savedAccountId = savedAccountId) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: (String?) -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack(state.savedAccountId)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val colors = listOf("#FF5722", "#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#607D8B", "#795548")
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    if (state.isEditMode) "Edit Account" else "Add Account",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = { onNavigateBack(null) }) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            // MOVED SAVE BUTTON HERE
            actions = {
                val canSave = state.name.isNotBlank() && !state.isSaving
                FilledIconButton(
                    onClick = viewModel::save,
                    enabled = canSave,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(width = 56.dp, height = 35.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Check, "Save", modifier = Modifier.size(24.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            windowInsets = WindowInsets(0.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Account Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // Account Type
            Text("Account Type", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            
            // Single-line horizontal list of account types (exclude CASH)
            val accountTypes = AccountType.values().filterNot { it == AccountType.CASH }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accountTypes.forEach { type ->
                    val isSelected = type == state.type
                    Column(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.onTypeChange(type)
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = CategoryIcons.getIcon(
                                when (type) {
                                    AccountType.WALLET -> "wallet"
                                    AccountType.BANK -> "bank"
                                    AccountType.INVESTMENT -> "trending_up"
                                    AccountType.PEOPLE -> "person"
                                    AccountType.CUSTOM -> "add"
                                    else -> "bank"
                                }
                            ),
                            contentDescription = type.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onTypeChange(AccountType.CUSTOM)
                },
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text(
                    text = "+ Add custom account type",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            // Custom Type Name Field
            if (state.type == AccountType.CUSTOM) {
                OutlinedTextField(
                    value = state.customTypeName,
                    onValueChange = viewModel::onCustomTypeNameChange,
                    label = { Text("Custom Type Name") },
                    placeholder = { Text("e.g., Savings, Credit Card") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = state.initialBalance,
                onValueChange = viewModel::onBalanceChange,
                label = { Text("Initial Balance (${LocalCurrencySymbol.current})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // Icon Picker
            Text("Icon", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CategoryIcons.accountIcons) { iconKey ->
                    val isSelected = iconKey == state.iconKey
                    val selectedColor = parseHexColor(state.colorHex)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isSelected) Modifier.border(2.dp, selectedColor, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.onIconChange(iconKey)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIcons.getIcon(iconKey),
                            contentDescription = iconKey,
                            tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Color Picker
            Text("Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { hex ->
                    val color = parseHexColor(hex)
                    val isSelected = hex == state.colorHex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color)
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.onColorChange(hex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
