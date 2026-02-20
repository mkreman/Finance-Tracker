package com.moneytracker.app.ui.screens.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Category
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus amount field and show keyboard when screen opens
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // One-shot navigation event — avoids crash on re-entry
    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            onNavigateBack()
        }
    }

    val typeColor = when (state.type) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Top Header Area (colored by type) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(typeColor.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp)
            ) {
                // Top bar row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Text(
                        if (state.isEditMode) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (state.isEditMode) {
                        IconButton(onClick = viewModel::deleteTransaction) {
                            Icon(Icons.Filled.Delete, "Delete", tint = ExpenseRed)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Transaction type tabs
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        val isSelected = type == state.type
                        val (label, color) = when (type) {
                            TransactionType.EXPENSE -> "Expense" to ExpenseRed
                            TransactionType.INCOME -> "Income" to IncomeGreen
                            TransactionType.TRANSFER -> "Transfer" to TransferBlue
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color else Color.Transparent)
                                .clickable { viewModel.onTypeChange(type) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Amount display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Amount", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            LocalCurrencySymbol.current,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                            color = typeColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = state.amount,
                            onValueChange = viewModel::onAmountChange,
                            textStyle = TextStyle(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(typeColor),
                            modifier = Modifier
                                .widthIn(min = 60.dp, max = 220.dp)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (state.amount.isEmpty()) {
                                    Text(
                                        "0",
                                        style = TextStyle(
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextTertiary
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }

        // ── Scrollable body ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp)
        ) {
            // Category Grid (not for transfers)
            if (state.type != TransactionType.TRANSFER) {
                CategorySplitSection(
                    state = state,
                    onCategorySelected = viewModel::onCategorySelected,
                    onCategoryToggled = viewModel::toggleCategory,
                    onSplitAmountChange = viewModel::onSplitAmountChange,
                    onToggleSplit = viewModel::toggleSplitMode,
                    onAddSplit = viewModel::addSplit,
                    onRemoveSplit = viewModel::removeSplit,
                    onAddCategory = viewModel::addCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Account selector
            AccountDropdown(
                label = if (state.type == TransactionType.TRANSFER) "From Account" else "Account",
                accounts = state.accounts,
                selectedId = state.selectedAccountId,
                onSelected = viewModel::onAccountSelected
            )

            // Transfer: To Account
            if (state.type == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(12.dp))
                AccountDropdown(
                    label = "To Account",
                    accounts = state.accounts.filter { it.id != state.selectedAccountId },
                    selectedId = state.toAccountId,
                    onSelected = viewModel::onToAccountSelected
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date & Time row
            DateTimeSelector(
                date = state.date,
                onDateSelected = viewModel::onDateChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Filled.Notes, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = viewModel::saveTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = state.isValid && !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeColor,
                    contentColor = Color.White,
                    disabledContainerColor = typeColor.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.isEditMode) "Update" else "Save",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ──────────── Category Section ────────────

@Composable
private fun CategorySplitSection(
    state: AddTransactionState,
    onCategorySelected: (Int, String, String) -> Unit,
    onCategoryToggled: (String, String) -> Unit,
    onSplitAmountChange: (Int, String) -> Unit,
    onToggleSplit: () -> Unit,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    onAddCategory: (String, String) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (state.isSplitMode) "Split Categories" else "Category",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            TextButton(onClick = onToggleSplit, contentPadding = PaddingValues(0.dp)) {
                Text(
                    if (state.isSplitMode) "Single" else "Split",
                    color = AccentOrange,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isSplitMode) {
            // Split mode — list of rows
            state.splits.forEachIndexed { index, split ->
                SplitRow(
                    split = split,
                    categories = state.categories,
                    showAmount = true,
                    showRemove = state.splits.size > 1,
                    onCategorySelected = { catId, catName -> onCategorySelected(index, catId, catName) },
                    onAmountChange = { onSplitAmountChange(index, it) },
                    onRemove = { onRemoveSplit(index) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Remaining
            val remaining = state.remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Remaining:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    "${LocalCurrencySymbol.current}${String.format("%.2f", remaining)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (kotlin.math.abs(remaining) < 0.01) IncomeGreen else ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onAddSplit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Split", color = AccentOrange, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            // Single/multi category — icon grid with multi-select
            CategoryGrid(
                categories = state.categories,
                selectedIds = state.selectedCategoryIds,
                onCategoryToggled = onCategoryToggled,
                onAddCategory = onAddCategory
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedIds: Set<String>,
    onCategoryToggled: (String, String) -> Unit,
    onAddCategory: (String, String) -> Unit = { _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Filter out "Other" categories and add "Add New" at the end
    val displayCategories = categories.filter { !it.name.equals("Other", ignoreCase = true) }

    // Show as a wrapping grid: 4 per row
    // We add a virtual "+Add" item at the end
    val itemCount = displayCategories.size + 1 // +1 for "Add New"
    val fullRows = displayCategories.chunked(4)
    // Check if the last row has space for our "Add" button
    val lastRow = fullRows.lastOrNull()
    val needsExtraRow = lastRow == null || lastRow.size == 4

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fullRows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    val isSelected = category.id in selectedIds
                    val catColor = parseHexColor(category.colorHex)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.18f) else Color.Transparent)
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, catColor, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                focusManager.clearFocus()
                                onCategoryToggled(category.id, category.name)
                            }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = if (isSelected) 0.25f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIcons.getIcon(category.iconKey),
                                contentDescription = category.name,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // If this is the last row and it has space, add the "Add New" button
                if (rowIndex == fullRows.lastIndex && !needsExtraRow) {
                    AddNewCategoryButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showAddCategoryDialog = true }
                    )
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    // Fill empty spaces in last row
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // If we need an extra row for the "Add" button
        if (needsExtraRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddNewCategoryButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showAddCategoryDialog = true }
                )
                repeat(3) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, iconKey ->
                onAddCategory(name, iconKey)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun AddNewCategoryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Category",
                tint = AccentOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add New",
            style = MaterialTheme.typography.labelSmall,
            color = AccentOrange,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("restaurant") }

    val availableIcons = listOf(
        "restaurant", "directions_car", "shopping_bag", "movie",
        "medical_services", "school", "receipt", "home",
        "two_wheeler", "pets", "people", "work",
        "laptop", "trending_up", "card_giftcard", "wallet",
        "store", "phone_android", "savings", "payments"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentOrange,
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = DividerColor,
                        focusedLabelColor = AccentOrange,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Select Icon", style = MaterialTheme.typography.titleSmall, color = TextSecondary)

                // Icon grid — 5 per row
                val iconRows = availableIcons.chunked(5)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    iconRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { iconKey ->
                                val isSelected = iconKey == selectedIcon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) AccentOrange.copy(alpha = 0.2f)
                                            else CardBackground
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.5.dp,
                                                AccentOrange,
                                                RoundedCornerShape(10.dp)
                                            )
                                            else Modifier
                                        )
                                        .clickable { selectedIcon = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIcons.getIcon(iconKey),
                                        contentDescription = iconKey,
                                        tint = if (isSelected) AccentOrange else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            repeat(5 - row.size) {
                                Spacer(modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), selectedIcon) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = if (name.isNotBlank()) AccentOrange else TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// ──────────── Split Row ────────────

@Composable
private fun SplitRow(
    split: SplitState,
    categories: List<Category>,
    showAmount: Boolean,
    showRemove: Boolean,
    onCategorySelected: (String, String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var showCategoryPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .clickable { showCategoryPicker = true }
                .padding(12.dp)
        ) {
            Text(
                text = split.categoryName.ifEmpty { "Select Category" },
                color = if (split.categoryId != null) TextPrimary else TextTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (showAmount) {
            OutlinedTextField(
                value = split.amount,
                onValueChange = onAmountChange,
                label = { Text(LocalCurrencySymbol.current) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(110.dp),
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        if (showRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, "Remove", tint = ExpenseRed, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onCategorySelected(category.id, category.name)
                                    showCategoryPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CategoryIcons.getIcon(category.iconKey),
                                contentDescription = null,
                                tint = parseHexColor(category.colorHex),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category.name, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ──────────── Account Dropdown ────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<com.moneytracker.app.domain.model.Account>,
    selectedId: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = selectedAccount?.let {
                {
                    Icon(
                        imageVector = CategoryIcons.getIcon(it.iconKey),
                        contentDescription = null,
                        tint = parseHexColor(it.colorHex),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name, color = TextPrimary) },
                    onClick = {
                        onSelected(account.id)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = CategoryIcons.getIcon(account.iconKey),
                            contentDescription = null,
                            tint = parseHexColor(account.colorHex)
                        )
                    }
                )
            }
        }
    }
}

// ──────────── Date & Time Selector ────────────

@Composable
private fun DateTimeSelector(
    date: Long,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date button
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply { timeInMillis = date }
                        newCal.set(Calendar.YEAR, y)
                        newCal.set(Calendar.MONTH, m)
                        newCal.set(Calendar.DAY_OF_MONTH, d)
                        onDateSelected(newCal.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(dateFmt.format(Date(date)), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }

        // Time button
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                TimePickerDialog(
                    context,
                    { _, h, m ->
                        val newCal = Calendar.getInstance().apply { timeInMillis = date }
                        newCal.set(Calendar.HOUR_OF_DAY, h)
                        newCal.set(Calendar.MINUTE, m)
                        onDateSelected(newCal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            modifier = Modifier
                .weight(0.7f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Filled.AccessTime, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(timeFmt.format(Date(date)), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ──────────── Shared text field colors ────────────

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentOrange,
    focusedBorderColor = AccentOrange,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentOrange,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
