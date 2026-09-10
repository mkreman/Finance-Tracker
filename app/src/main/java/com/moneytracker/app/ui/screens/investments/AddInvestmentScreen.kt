package com.moneytracker.app.ui.screens.investments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.parseHexColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddInvestmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val editingTransaction by viewModel.editingTransaction.collectAsState()
    val editingAccountId by viewModel.editingAccountId.collectAsState()

    var transactionType by remember {
        mutableStateOf(viewModel.initialType?.takeIf { it in listOf("INVEST", "WITHDRAW") } ?: "INVEST")
    }

    // Invest state
    var query by remember { mutableStateOf("") }
    var selectedAsset by remember { mutableStateOf<SearchResult?>(null) }
    var investUnits by remember { mutableStateOf("") }
    var investAmount by remember { mutableStateOf("") }

    // Withdraw state (stocks already bought)
    var selectedHolding by remember { mutableStateOf<InvestmentEntity?>(null) }
    var withdrawUnits by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }

    // Common state
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasInitializedEdit by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val currency = LocalCurrencySymbol.current

    // Handle edit mode transaction pre-population
    LaunchedEffect(editingTransaction, editingAccountId) {
        val txn = editingTransaction ?: return@LaunchedEffect
        if (!hasInitializedEdit) {
            hasInitializedEdit = true
            transactionType = txn.transactionType
            selectedDateMillis = txn.date
            if (txn.transactionType == "INVEST") {
                investUnits = if (txn.units == txn.units.toLong().toDouble()) txn.units.toLong().toString() else txn.units.toString()
                investAmount = if (txn.amount == txn.amount.toLong().toDouble()) txn.amount.toLong().toString() else txn.amount.toString()
            } else {
                withdrawUnits = if (txn.units == txn.units.toLong().toDouble()) txn.units.toLong().toString() else txn.units.toString()
                withdrawAmount = if (txn.amount == txn.amount.toLong().toDouble()) txn.amount.toLong().toString() else txn.amount.toString()
            }
        }
    }

    LaunchedEffect(editingAccountId) {
        editingAccountId?.let { selectedAccountId = it }
    }

    LaunchedEffect(editingTransaction, holdings) {
        val txn = editingTransaction ?: return@LaunchedEffect
        val matching = holdings.firstOrNull { it.symbol.equals(txn.symbol, ignoreCase = true) }
        if (txn.transactionType == "INVEST") {
            selectedAsset = SearchResult(
                symbol = txn.symbol,
                name = matching?.name ?: txn.symbol,
                type = matching?.type ?: "Stock",
                exchange = matching?.exchange ?: ""
            )
        } else {
            selectedHolding = matching ?: InvestmentEntity(
                symbol = txn.symbol,
                name = txn.symbol,
                type = "Stock",
                exchange = "",
                totalUnits = txn.units,
                totalInvestedAmount = txn.amount
            )
        }
    }

    // Handle initial stock pre-selection from Stock Details (when not editing)
    LaunchedEffect(holdings, viewModel.initialSymbol) {
        if (viewModel.isEditMode) return@LaunchedEffect
        val sym = viewModel.initialSymbol
        if (sym != null && holdings.isNotEmpty()) {
            val matching = holdings.firstOrNull { it.symbol.equals(sym, ignoreCase = true) }
            if (matching != null) {
                selectedHolding = matching
                if (selectedAsset == null) {
                    selectedAsset = SearchResult(
                        symbol = matching.symbol,
                        name = matching.name,
                        type = matching.type,
                        exchange = matching.exchange
                    )
                }
            }
        }
    }

    // Auto-select first account if not set
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.firstOrNull { it.type.name == "INVESTMENT" }?.id
                ?: accounts.firstOrNull { it.type.name == "BANK" }?.id
                ?: accounts.firstOrNull { it.type.name == "WALLET" }?.id
                ?: accounts.first().id
        }
    }

    // Auto-select first holding for withdrawal if available and none selected (when not editing)
    LaunchedEffect(holdings, transactionType) {
        if (viewModel.isEditMode) return@LaunchedEffect
        if (transactionType == "WITHDRAW" && selectedHolding == null && holdings.isNotEmpty()) {
            selectedHolding = holdings.first()
            val sym = viewModel.initialSymbol
            selectedHolding = holdings.firstOrNull { it.symbol.equals(sym, ignoreCase = true) } ?: holdings.first()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val maxAvailableUnits = (selectedHolding?.totalUnits ?: 0.0) +
        (if (viewModel.isEditMode && editingTransaction?.transactionType == "WITHDRAW") (editingTransaction?.units ?: 0.0) else 0.0)

    // Validation
    val canSaveInvest = transactionType == "INVEST" &&
        selectedAsset != null &&
        investUnits.toDoubleOrNull()?.let { it > 0 } == true &&
        investAmount.toDoubleOrNull()?.let { it > 0 } == true &&
        !uiState.isSaving

    val canSaveWithdraw = transactionType == "WITHDRAW" &&
        selectedHolding != null &&
        withdrawUnits.toDoubleOrNull()?.let { it > 0 && it <= maxAvailableUnits } == true &&
        withdrawAmount.toDoubleOrNull()?.let { it > 0 } == true &&
        !uiState.isSaving

    val canSave = if (transactionType == "INVEST") canSaveInvest else canSaveWithdraw
    val activeTypeColor = if (transactionType == "INVEST") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with zero top gap, Back button, and top-right Save/Delete buttons
        TopAppBar(
            title = {
                Text(
                    text = if (viewModel.isEditMode) {
                        if (transactionType == "INVEST") "Edit Investment" else "Edit Withdrawal"
                    } else {
                        if (transactionType == "INVEST") "Invest" else "Withdraw"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDeleteConfirmation by remember { mutableStateOf(false) }
                    if (viewModel.isEditMode) {
                        FilledIconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(width = 56.dp, height = 35.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(20.dp))
                        }

                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Delete Entry") },
                                text = { Text("Are you sure you want to delete this investment entry?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showDeleteConfirmation = false
                                        viewModel.deleteInvestmentTransaction()
                                    }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            if (viewModel.isEditMode) {
                                val units = if (transactionType == "INVEST") investUnits else withdrawUnits
                                val amount = if (transactionType == "INVEST") investAmount else withdrawAmount
                                viewModel.updateInvestmentTransaction(
                                    units = units,
                                    amount = amount,
                                    date = selectedDateMillis,
                                    accountId = selectedAccountId
                                )
                            } else {
                                if (transactionType == "INVEST") {
                                    selectedAsset?.let { asset ->
                                        viewModel.saveInvest(
                                            asset = asset,
                                            units = investUnits,
                                            amount = investAmount,
                                            date = selectedDateMillis,
                                            accountId = selectedAccountId
                                        )
                                    }
                                } else {
                                    selectedHolding?.let { holding ->
                                        viewModel.saveWithdrawal(
                                            holding = holding,
                                            units = withdrawUnits,
                                            amount = withdrawAmount,
                                            date = selectedDateMillis,
                                            accountId = selectedAccountId
                                        )
                                    }
                                }
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier.size(width = 56.dp, height = 35.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = activeTypeColor,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, "Save", modifier = Modifier.size(24.dp))
                        }
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
            if (!viewModel.isEditMode) {
                // ── Segmented Type Selector (Invest & Withdraw in similar fashion as Expense/Income/Transfer) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("INVEST" to "Invest", "WITHDRAW" to "Withdraw").forEach { (type, label) ->
                        val isSelected = transactionType == type
                        val buttonColor = if (type == "INVEST") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) buttonColor else Color.Transparent)
                                .clickable {
                                    transactionType = type
                                    viewModel.clearSaveError()
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════
            // OPTION 1: INVEST (Search Bar + Stock Selection)
            // ═══════════════════════════════════════════════
            if (transactionType == "INVEST") {
                if (!viewModel.isEditMode) {
                    // Fuzzy Stock / Mutual Fund Search Bar
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            selectedAsset = null
                            viewModel.searchAsset(it)
                        },
                        label = { Text("Search Stock / Mutual Fund (e.g. TCS, Reliance)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (query.isNotBlank()) {
                                IconButton(onClick = {
                                    query = ""
                                    selectedAsset = null
                                    viewModel.searchAsset("")
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                        singleLine = true
                    )

                    // Search Results Dropdown (Fuzzy matched Indian stocks)
                    if (query.isNotBlank() && selectedAsset == null) {
                        if (isSearching && searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else if (searchResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                    items(searchResults) { result ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    result.name,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                                )
                                            },
                                            supportingContent = {
                                                Text(
                                                    "${result.symbol} • ${result.exchange} • ${result.type}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                selectedAsset = result
                                                query = result.name
                                            }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        } else if (!isSearching && query.trim().length >= 2) {
                            Text(
                                text = "No matching stocks found for \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Selected Stock Info Card
                if (selectedAsset != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    selectedAsset!!.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${selectedAsset!!.symbol} • ${selectedAsset!!.exchange} • ${selectedAsset!!.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Account Selection
                if (accounts.isNotEmpty()) {
                    AccountDropdown(
                        label = "Pay From Account",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { selectedAccountId = it }
                    )
                }

                // Units Field
                OutlinedTextField(
                    value = investUnits,
                    onValueChange = { investUnits = it },
                    label = { Text("Units / Shares to Buy") },
                    placeholder = { Text("e.g. 10") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                // Total Amount Field
                OutlinedTextField(
                    value = investAmount,
                    onValueChange = { investAmount = it },
                    label = { Text("Total Invested Amount ($currency)") },
                    placeholder = { Text("e.g. 15000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
            }

            // ══════════════════════════════════════════════════════════════
            // OPTION 2: WITHDRAW (Select from already bought stocks)
            // ══════════════════════════════════════════════════════════════
            if (transactionType == "WITHDRAW") {
                if (!viewModel.isEditMode) {
                    if (holdings.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "No stocks available to withdraw",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "You haven't bought any stocks yet. Switch to 'Invest' to record your first stock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Bought Stocks Selector Dropdown
                        BoughtStockDropdown(
                            holdings = holdings,
                            selectedHolding = selectedHolding,
                            onHoldingSelected = {
                                selectedHolding = it
                                withdrawUnits = ""
                                withdrawAmount = ""
                            }
                        )
                    }
                }

                // Selected Bought Stock Detail Card
                if (selectedHolding != null) {
                    val holding = selectedHolding!!
                    val avgPrice = if (holding.totalUnits > 0) holding.totalInvestedAmount / holding.totalUnits else 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                holding.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${holding.symbol} • ${holding.exchange}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Available: ${formatUnits(maxAvailableUnits)} units",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Avg Cost: $currency${formatAmount(avgPrice)}/unit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Account Selection (Deposit To)
                    if (accounts.isNotEmpty()) {
                        AccountDropdown(
                            label = "Deposit To Account",
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onAccountSelected = { selectedAccountId = it }
                        )
                    }

                    // Units to Withdraw Field
                    val isOverUnits = withdrawUnits.toDoubleOrNull()?.let { it > maxAvailableUnits } == true
                    OutlinedTextField(
                        value = withdrawUnits,
                        onValueChange = { withdrawUnits = it },
                        label = { Text("Units to Withdraw") },
                        placeholder = { Text("Max: ${formatUnits(maxAvailableUnits)}") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isOverUnits,
                        supportingText = {
                            if (isOverUnits) {
                                Text("Cannot exceed available ${formatUnits(maxAvailableUnits)} units", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Total available: ${formatUnits(maxAvailableUnits)} units")
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = {
                                withdrawUnits = if (maxAvailableUnits == maxAvailableUnits.toLong().toDouble()) {
                                    maxAvailableUnits.toLong().toString()
                                } else {
                                    maxAvailableUnits.toString()
                                }
                            }) {
                                Text("MAX", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    // Withdrawal Amount Field
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Withdrawal Amount to Receive ($currency)") },
                        placeholder = { Text("e.g. 5000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }
            }

            // ── Common Fields: Date & Time Pickers ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(y, m, d)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateFormat.format(Date(selectedDateMillis)))
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier
                        .weight(0.8f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(timeFormat.format(Date(selectedDateMillis)))
                }
            }

            // Error Message Banner
            if (uiState.saveError != null) {
                Text(
                    text = uiState.saveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoughtStockDropdown(
    holdings: List<InvestmentEntity>,
    selectedHolding: InvestmentEntity?,
    onHoldingSelected: (InvestmentEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedHolding?.let { "${it.name} (${it.symbol})" } ?: "Select Bought Stock",
            onValueChange = {},
            readOnly = true,
            label = { Text("Stock / Mutual Fund Already Bought") },
            leadingIcon = {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            holdings.forEach { holding ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                holding.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                "${holding.symbol} • ${formatUnits(holding.totalUnits)} units available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        onHoldingSelected(holding)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: "Select Account",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    selectedAccount?.let { CategoryIcons.getIcon(it.iconKey) } ?: Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = selectedAccount?.let { parseHexColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    leadingIcon = {
                        Icon(
                            CategoryIcons.getIcon(account.iconKey),
                            contentDescription = null,
                            tint = parseHexColor(account.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        String.format(Locale.US, "%,.0f", amount)
    } else {
        String.format(Locale.US, "%,.2f", amount)
    }
}

private fun formatUnits(units: Double): String {
    return if (units == units.toLong().toDouble()) {
        String.format(Locale.US, "%,.0f", units)
    } else {
        String.format(Locale.US, "%,.4f", units)
    }
}