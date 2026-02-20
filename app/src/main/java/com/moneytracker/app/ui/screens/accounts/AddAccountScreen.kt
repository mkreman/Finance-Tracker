package com.moneytracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import com.moneytracker.app.ui.theme.*
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
import java.util.UUID
import javax.inject.Inject

data class AddAccountState(
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val initialBalance: String = "0",
    val colorHex: String = "#2196F3",
    val iconKey: String = "bank",
    val isEditMode: Boolean = false,
    val editAccountId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
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
        if (accountId != null) {
            loadAccount(accountId)
        }
    }

    private fun loadAccount(accountId: String) {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().firstOrNull() ?: return@launch
            val account = accounts.find { it.id == accountId } ?: return@launch
            _state.update {
                it.copy(
                    name = account.name,
                    type = account.type,
                    initialBalance = account.initialBalance.toLong().toString(),
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
        if (current.name.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val balance = current.initialBalance.toDoubleOrNull() ?: 0.0

            if (current.isEditMode && current.editAccountId != null) {
                accountRepository.updateAccount(
                    Account(
                        id = current.editAccountId,
                        name = current.name,
                        type = current.type,
                        initialBalance = balance,
                        currentBalance = balance,
                        colorHex = current.colorHex,
                        iconKey = current.iconKey
                    )
                )
            } else {
                accountRepository.saveAccount(
                    Account(
                        id = UUID.randomUUID().toString(),
                        name = current.name,
                        type = current.type,
                        initialBalance = balance,
                        currentBalance = balance,
                        colorHex = current.colorHex,
                        iconKey = current.iconKey
                    )
                )
            }
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val colors = listOf("#FF5722", "#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#607D8B", "#795548")
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = AccentOrange,
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = DividerColor,
        focusedLabelColor = AccentOrange,
        unfocusedLabelColor = TextSecondary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    if (state.isEditMode) "Edit Account" else "Add Account",
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
            Text("Account Type", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountType.values().forEach { type ->
                    val isSelected = type == state.type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentOrange.copy(alpha = 0.2f) else CardBackground)
                            .clickable { viewModel.onTypeChange(type) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name,
                            color = if (isSelected) AccentOrange else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
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
            Text("Icon", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else CardBackground)
                            .then(
                                if (isSelected) Modifier.border(2.dp, selectedColor, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable { viewModel.onIconChange(iconKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIcons.getIcon(iconKey),
                            contentDescription = iconKey,
                            tint = if (isSelected) selectedColor else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Color Picker
            Text("Color", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                            .clickable { viewModel.onColorChange(hex) },
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.name.isNotBlank() && !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White,
                    disabledContainerColor = AccentOrange.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (state.isEditMode) "Update Account" else "Save Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
