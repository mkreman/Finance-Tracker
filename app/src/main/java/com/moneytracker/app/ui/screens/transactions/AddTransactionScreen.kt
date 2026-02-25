package com.moneytracker.app.ui.screens.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            try {
                keyboardController?.hide()
                onNavigateBack()
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
                    
                    // Action Buttons Row (No collective outer background)
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showDeleteConfirmation by remember { mutableStateOf(false) }

                        if (state.isEditMode) {
                            FilledIconButton(
                                onClick = { showDeleteConfirmation = true },
                                // Restored your original parabolic dimensions
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
                                    title = { Text("Delete Transaction") },
                                    text = { Text("Are you sure you want to delete this transaction?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showDeleteConfirmation = false
                                            viewModel.deleteTransaction()
                                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = viewModel::saveTransaction,
                            enabled = state.isValid && !state.isSaving,
                            // Restored your original parabolic dimensions
                            modifier = Modifier.size(width = 56.dp, height = 35.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = typeColor,
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

        // Body content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp)
        ) {
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

            if (state.type != TransactionType.TRANSFER) {
                AccountDropdown(
                    label = "Account",
                    accounts = state.accounts,
                    selectedId = state.selectedAccountId,
                    onSelected = viewModel::onAccountSelected
                )
            } else {
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

            DateTimeSelector(
                date = state.date,
                onDateSelected = viewModel::onDateChange
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ──────────── Helper Components ────────────

@Composable
private fun CategorySplitSection(
    state: AddTransactionState,
    onCategorySelected: (Int, String, String) -> Unit,
    onCategoryToggled: (String, String) -> Unit,
    onSplitAmountChange: (Int, String) -> Unit,
    onToggleSplit: () -> Unit,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
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
            TextButton(onClick = onToggleSplit) {
                Text(if (state.isSplitMode) "Single" else "Split")
            }
        }

        if (state.isSplitMode) {
            state.splits.forEachIndexed { index, split ->
                SplitRow(
                    split = split,
                    categories = state.categories,
                    showAmount = true,
                    showRemove = state.splits.size > 1,
                    onCategorySelected = { id, name -> onCategorySelected(index, id, name) },
                    onAmountChange = { onSplitAmountChange(index, it) },
                    onRemove = { onRemoveSplit(index) }
                )
            }
        } else {
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
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val displayCategories = categories.filter { !it.name.equals("Other", ignoreCase = true) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        displayCategories.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { category ->
                    val isSelected = category.id in selectedIds
                    val catColor = parseHexColor(category.colorHex)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { 
                                focusManager.clearFocus()
                                onCategoryToggled(category.id, category.name) 
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(CategoryIcons.getIcon(category.iconKey), null, tint = catColor, modifier = Modifier.size(22.dp))
                        }
                        Text(category.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                repeat(4 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
            Text(split.categoryName.ifEmpty { "Select Category" })
        }
        if (showAmount) {
            OutlinedTextField(value = split.amount, onValueChange = onAmountChange, modifier = Modifier.width(100.dp))
        }
        if (showRemove) {
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, null) }
        }
    }
}

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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedAccount?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(text = { Text(account.name) }, onClick = { onSelected(account.id); expanded = false })
            }
        }
    }
}

@Composable
private fun DateTimeSelector(date: Long, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val cal = Calendar.getInstance().apply { timeInMillis = date }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(context, { _, y, m, d ->
                    val newCal = Calendar.getInstance().apply { 
                        timeInMillis = date
                        set(y, m, d)
                    }
                    onDateSelected(newCal.timeInMillis)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(dateFmt.format(Date(date)))
        }

        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m ->
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                    }
                    onDateSelected(newCal.timeInMillis)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            },
            modifier = Modifier.weight(0.7f).height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(timeFmt.format(Date(date)))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)
