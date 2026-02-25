package com.moneytracker.app.ui.screens.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
            android.util.Log.d("AddTxnScreen", "navigateBack event received")
            try {
                keyboardController?.hide()
                onNavigateBack()
                android.util.Log.d("AddTxnScreen", "onNavigateBack() completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("AddTxnScreen", "Navigation failed", e)
            }
        }
    }

    val typeColor = when (state.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
    }

    // Show error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        if (state.isEditMode) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Grouped Parabolic Container for Actions
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(24.dp)) // Smooth pill-shaped background
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 6.dp), // Wider padding to create room
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // Distinct gap between the buttons
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showDeleteConfirmation by remember { mutableStateOf(false) }

                        // Parabolic Delete Button
                        if (state.isEditMode) {
                            FilledIconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.size(width = 56.dp, height = 42.dp), // Wider than tall
                                shape = RoundedCornerShape(16.dp), // Parabolic/Rounded edges
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(22.dp))
                            }

                            // Safety Confirmation Dialog
                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("Delete Transaction", color = MaterialTheme.colorScheme.onSurface) },
                                    text = { Text("Are you sure you want to delete this transaction?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showDeleteConfirmation = false
                                                viewModel.deleteTransaction()
                                            }
                                        ) {
                                            Text("Delete", color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirmation = false }) {
                                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        // Parabolic Save/Check Button
                        FilledIconButton(
                            onClick = viewModel::saveTransaction,
                            enabled = state.isValid && !state.isSaving,
                            modifier = Modifier.size(width = 56.dp, height = 42.dp), // Wider than tall
                            shape = RoundedCornerShape(16.dp), // Parabolic/Rounded edges
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = typeColor,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = if (state.isEditMode) "Update" else "Save",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Transaction type tabs
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        val isSelected = type == state.type
                        val (label, color) = when (type) {
                            TransactionType.EXPENSE -> "Expense" to MaterialTheme.colorScheme.error
                            TransactionType.INCOME -> "Income" to MaterialTheme.colorScheme.tertiary
                            TransactionType.TRANSFER -> "Transfer" to MaterialTheme.colorScheme.secondary
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
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                                            color = MaterialTheme.colorScheme.outline
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
                    onAddCategory = viewModel::addCategory,
                    onDeleteCategory = viewModel::deleteCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Account selectors
            if (state.type != TransactionType.TRANSFER) {
                // Regular Account Selector
                AccountDropdown(
                    label = "Account",
                    accounts = state.accounts,
                    selectedId = state.selectedAccountId,
                    onSelected = viewModel::onAccountSelected
                )
            } else {
                // Transfer Account Selector (From -> To)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountDropdown(
                        label = "From Account",
                        accounts = state.accounts,
                        selectedId = state.selectedAccountId,
                        onSelected = viewModel::onAccountSelected,
                        modifier = Modifier.weight(1f)
                    )

                    AccountDropdown(
                        label = "To Account",
                        accounts = state.accounts.filter { it.id != state.selectedAccountId },
                        selectedId = state.toAccountId,
                        onSelected = viewModel::onToAccountSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    Icon(Icons.Filled.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recurring Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Repeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Recurring Transaction",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isRecurring,
                        onCheckedChange = viewModel::onRecurringToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                if (state.isRecurring) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Repeat every",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.recurringInterval,
                            onValueChange = viewModel::onRecurringIntervalChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        com.moneytracker.app.data.local.database.entities.RecurringUnit.values().forEach { unit ->
                            val isSelected = unit == state.recurringUnit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                    .clickable { viewModel.onRecurringUnitChange(unit) }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Notify for recurring entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = state.notifyForRecurringEntries,
                            onCheckedChange = viewModel::onNotifyForRecurringEntriesChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Optional end date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val calendar = Calendar.getInstance()
                                state.recurringEndDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val endCalendar = Calendar.getInstance().apply {
                                            set(year, month, day, 23, 59, 59)
                                        }
                                        viewModel.onRecurringEndDateChange(endCalendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EventRepeat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (state.recurringEndDate != null) {
                                    "Ends: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(state.recurringEndDate)}"
                                } else {
                                    "No end date (tap to set)"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (state.recurringEndDate != null) {
                            IconButton(
                                onClick = { viewModel.onRecurringEndDateChange(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
    onAddCategory: (String, String) -> Unit = { _, _ -> },
    onDeleteCategory: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToggleSplit, contentPadding = PaddingValues(0.dp)) {
                Text(
                    if (state.isSplitMode) "Single" else "Split",
                    color = MaterialTheme.colorScheme.primary,
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
                Text("Remaining:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${LocalCurrencySymbol.current}${String.format("%.2f", remaining)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (kotlin.math.abs(remaining) < 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onAddSplit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Split", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            // Single/multi category — icon grid with multi-select
            CategoryGrid(
                categories = state.categories,
                selectedIds = state.selectedCategoryIds,
                onCategoryToggled = onCategoryToggled,
                onAddCategory = onAddCategory,
                onDeleteCategory = onDeleteCategory
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedIds: Set<String>,
    onCategoryToggled: (String, String) -> Unit,
    onAddCategory: (String, String) -> Unit = { _, _ -> },
    onDeleteCategory: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var pendingDeleteCategory by remember { mutableStateOf<Category?>(null) }

    // Filter out "Other" categories and add "Add New" at the end
    val displayCategories = categories.filter { !it.name.equals("Other", ignoreCase = true) }

    // Show as a wrapping grid: 4 per row
    // We add a virtual "+Add" item at the end
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
                            .pointerInput(category.id) {
                                detectTapGestures(
                                    onTap = {
                                        focusManager.clearFocus()
                                        onCategoryToggled(category.id, category.name)
                                    },
                                    onLongPress = {
                                        focusManager.clearFocus()
                                        pendingDeleteCategory = category
                                    }
                                )
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
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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

    pendingDeleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Delete Category", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Delete '${category.name}' category?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category.id)
                        pendingDeleteCategory = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Category",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add New",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
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
        title = { Text("Add New Category", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Select Icon", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
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
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text("Save", color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
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
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showCategoryPicker = true }
                .padding(12.dp)
        ) {
            Text(
                text = split.categoryName.ifEmpty { "Select Category" },
                color = if (split.categoryId != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
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
                Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    categories.forEach { category ->
                        var showCategoryMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            onCategorySelected(category.id, category.name)
                                            showCategoryPicker = false
                                        },
                                        onLongPress = { showCategoryMenu = true }
                                    )
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
                            Text(category.name, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            
                            Box {
                                IconButton(onClick = { showCategoryMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = { 
                                            showCategoryMenu = false
                                            // TODO: Add edit category navigation
                                            Toast.makeText(context, "Edit category feature coming soon", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = { 
                                            showCategoryMenu = false
                                            // TODO: Add delete category confirmation
                                            Toast.makeText(context, "Delete category feature coming soon", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface,
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
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
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
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name, color = MaterialTheme.colorScheme.onSurface) },
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(dateFmt.format(Date(date)), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(timeFmt.format(Date(date)), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ──────────── Shared text field colors ────────────

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
