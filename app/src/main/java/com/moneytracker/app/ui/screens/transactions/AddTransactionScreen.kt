package com.moneytracker.app.ui.screens.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import androidx.compose.ui.viewinterop.AndroidView
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Category
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.parseHexColor
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class AccountSelectionTarget { FROM, TO, PERSON } // Added PERSON Target

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onAddAccount: (AccountSelectionTarget) -> Unit = {},
    createdAccountId: String? = null,
    createdAccountTarget: String? = null,
    onCreatedAccountConsumed: () -> Unit = {},
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.addReceiptUri(uri.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { viewModel.addReceiptUri(it.toString()) }
        pendingCameraUri = null
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Capture newly created accounts properly for their explicit target!
    LaunchedEffect(createdAccountId, createdAccountTarget) {
        if (!createdAccountId.isNullOrBlank()) {
            when (createdAccountTarget) {
                AccountSelectionTarget.TO.name -> viewModel.onToAccountSelected(createdAccountId)
                AccountSelectionTarget.PERSON.name -> viewModel.onPersonAccountCreated(createdAccountId)
                else -> viewModel.onAccountSelected(createdAccountId)
            }
            onCreatedAccountConsumed()
        }
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
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                    Text(if (state.isEditMode) "Edit Transaction" else "Add Transaction", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showDeleteConfirmation by remember { mutableStateOf(false) }
                        if (state.isEditMode) {
                            FilledIconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.size(width = 56.dp, height = 35.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                            ) { Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(20.dp)) }

                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("Delete Transaction") },
                                    text = { Text("Are you sure you want to delete this transaction?") },
                                    confirmButton = { TextButton(onClick = { showDeleteConfirmation = false; viewModel.deleteTransaction() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
                                    dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = viewModel::saveTransaction,
                            enabled = state.isValid && !state.isSaving,
                            modifier = Modifier.size(width = 56.dp, height = 35.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = typeColor, contentColor = Color.White, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Check, "Save", modifier = Modifier.size(24.dp))
                        }
                    }
                }

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
                            Text(text = label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalAlignment = Alignment.Start) {
                    Text("Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    var amountTextFieldValue by remember { mutableStateOf(TextFieldValue(text = state.amount, selection = TextRange(state.amount.length))) }
                    LaunchedEffect(state.amount) {
                        if (state.amount != amountTextFieldValue.text) amountTextFieldValue = amountTextFieldValue.copy(text = state.amount, selection = TextRange(state.amount.length))
                    }
                    fun insertOperator(op: Char) {
                        val text = amountTextFieldValue.text
                        val cursor = amountTextFieldValue.selection.start.coerceIn(0, text.length)
                        val updated = buildString { append(text.substring(0, cursor)); append(op); append(text.substring(cursor)) }
                        val nextCursor = (cursor + 1).coerceAtMost(updated.length)
                        amountTextFieldValue = TextFieldValue(updated, TextRange(nextCursor))
                        viewModel.onAmountChange(updated)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp), horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                                Text(LocalCurrencySymbol.current, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light), color = typeColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                BasicTextField(
                                    value = amountTextFieldValue,
                                    onValueChange = { newValue -> amountTextFieldValue = newValue; viewModel.onAmountChange(newValue.text) },
                                    textStyle = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Start),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    cursorBrush = SolidColor(typeColor),
                                    modifier = Modifier.widthIn(min = 70.dp, max = 220.dp).focusRequester(focusRequester),
                                    decorationBox = { innerTextField ->
                                        if (amountTextFieldValue.text.isEmpty()) Text("0", style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline))
                                        innerTextField()
                                    }
                                )
                            }
                            if (state.amount.any { it == '+' || it == '-' || it == '*' || it == '/' }) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val evaluated = state.evaluatedAmount
                                Text(text = if (evaluated != null) "= ${LocalCurrencySymbol.current}${"%.2f".format(Locale.US, evaluated)}" else "Invalid expression", style = MaterialTheme.typography.bodySmall, color = if (evaluated != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { insertOperator('+') }, modifier = Modifier.size(width = 58.dp, height = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) { Text("+", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
                                FilledTonalButton(onClick = { insertOperator('-') }, modifier = Modifier.size(width = 58.dp, height = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) { Text("-", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { insertOperator('*') }, modifier = Modifier.size(width = 58.dp, height = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) { Text("x", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
                                FilledTonalButton(onClick = { insertOperator('/') }, modifier = Modifier.size(width = 58.dp, height = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) { Text("÷", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 20.dp)) {
            
            if (state.type != TransactionType.TRANSFER) {
                CategorySplitSection(
                    state = state,
                    viewModel = viewModel,
                    onCategoryToggled = viewModel::toggleCategory,
                    onAddCategory = viewModel::addCategory,
                    onEditCategory = viewModel::editCategory,
                    onDeleteCategory = viewModel::deleteCategory,
                    onAddAccount = { onAddAccount(AccountSelectionTarget.PERSON) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.type != TransactionType.TRANSFER) {
                AccountDropdown(label = "Account", accounts = state.accounts, selectedId = state.selectedAccountId, onSelected = viewModel::onAccountSelected, onAddAccount = { onAddAccount(AccountSelectionTarget.FROM) })
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountDropdown(label = "From Account", accounts = state.accounts, selectedId = state.selectedAccountId, onSelected = viewModel::onAccountSelected, onAddAccount = { onAddAccount(AccountSelectionTarget.FROM) }, modifier = Modifier.fillMaxWidth())
                    AccountDropdown(label = "To Account", accounts = state.accounts.filter { it.id != state.selectedAccountId }, selectedId = state.toAccountId, onSelected = viewModel::onToAccountSelected, onAddAccount = { onAddAccount(AccountSelectionTarget.TO) }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DateTimeSelector(date = state.date, onDateSelected = viewModel::onDateChange)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.note, onValueChange = viewModel::onNoteChange, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, colors = textFieldColors(), shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { val uri = createReceiptImageUri(context); pendingCameraUri = uri; cameraLauncher.launch(uri) }) { Icon(Icons.Filled.PhotoCamera, "Capture receipt", tint = if (state.receiptUris.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Filled.UploadFile, "Upload receipt", tint = if (state.receiptUris.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                    }
                }
            )

            if (state.receiptUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ReceiptAttachmentsPreviewSection(receiptUris = state.receiptUris, onRemove = viewModel::removeReceiptUri, onClearAll = viewModel::clearAllReceipts)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recurring Section
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Repeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recurring Transaction", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.isRecurring, onCheckedChange = viewModel::onRecurringToggle, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                }

                if (state.isRecurring) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Repeat every", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = state.recurringInterval, onValueChange = viewModel::onRecurringIntervalChange, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(80.dp), singleLine = true, colors = textFieldColors(), shape = RoundedCornerShape(10.dp))
                        com.moneytracker.app.data.local.database.entities.RecurringUnit.values().forEach { unit ->
                            val isSelected = unit == state.recurringUnit
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface).clickable { viewModel.onRecurringUnitChange(unit) }.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(text = unit.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notify for recurring entries", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(checked = state.notifyForRecurringEntries, onCheckedChange = viewModel::onNotifyForRecurringEntriesChange, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { val calendar = Calendar.getInstance(); state.recurringEndDate?.let { calendar.timeInMillis = it }; DatePickerDialog(context, { _, year, month, day -> val endCalendar = Calendar.getInstance().apply { set(year, month, day, 23, 59, 59) }; viewModel.onRecurringEndDateChange(endCalendar.timeInMillis) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }.background(MaterialTheme.colorScheme.surface).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EventRepeat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.recurringEndDate != null) "Ends: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(state.recurringEndDate)}" else "No end date (tap to set)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (state.recurringEndDate != null) IconButton(onClick = { viewModel.onRecurringEndDateChange(null) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun createReceiptImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(directory, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ReceiptAttachmentsPreviewSection(receiptUris: List<String>, onRemove: (String) -> Unit, onClearAll: () -> Unit) {
    val context = LocalContext.current
    val parsedUris = receiptUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
        Text("Receipt / Invoice", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (parsedUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                parsedUris.forEach { uri ->
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { val mimeType = context.contentResolver.getType(uri).orEmpty(); val openIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, if (mimeType.startsWith("image/")) "image/*" else mimeType.ifBlank { "*/*" }); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; runCatching { context.startActivity(openIntent) }.onFailure { Toast.makeText(context, "No app found to open this attachment", Toast.LENGTH_SHORT).show() } }) {
                        AndroidView(factory = { viewContext -> ImageView(viewContext).apply { scaleType = ImageView.ScaleType.CENTER_CROP } }, update = { imageView -> imageView.setImageURI(uri) }, modifier = Modifier.fillMaxSize())
                        TextButton(onClick = { onRemove(uri.toString()) }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Filled.DeleteOutline, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text("Remove") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClearAll) { Icon(Icons.Filled.DeleteOutline, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text("Remove all attachments") }
        }
    }
}

@Composable
private fun CategorySplitSection(
    state: AddTransactionState,
    viewModel: AddTransactionViewModel,
    onCategoryToggled: (String, String) -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onEditCategory: (String, String, String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddAccount: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = viewModel::toggleSplitMode) { Text(if (state.isSplitMode) "Disable Split" else "Split") }
        }

        if (state.isSplitMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.splits.forEachIndexed { index, split ->
                    if (split.targetType == SplitTargetType.PERSON) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountDropdown(
                                label = "Person",
                                accounts = state.peopleAccounts,
                                selectedId = split.targetId,
                                onSelected = { accountId -> val acc = state.peopleAccounts.find { it.id == accountId }; viewModel.onPersonSplitSelected(index, accountId, acc?.name ?: "") },
                                onAddAccount = onAddAccount, // Tied to AccountSelectionTarget.PERSON via trailing lambda
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = split.amount,
                                onValueChange = { viewModel.onSplitAmountChange(index, it) },
                                modifier = Modifier.width(100.dp),
                                colors = textFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            IconButton(onClick = { viewModel.removeSplit(index) }) { Icon(Icons.Filled.Close, null) }
                        }
                    } else {
                        // Category Dropdown for explicit split items
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryDropdown(
                                label = "Category",
                                categories = state.categories,
                                selectedId = split.targetId,
                                onSelected = { catId -> val cat = state.categories.find { it.id == catId }; viewModel.onCategorySelected(index, catId, cat?.name ?: "") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = split.amount,
                                onValueChange = { viewModel.onSplitAmountChange(index, it) },
                                modifier = Modifier.width(100.dp),
                                colors = textFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            IconButton(onClick = { viewModel.removeSplit(index) }) { Icon(Icons.Filled.Close, null) }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // --- Dynamic Remaining Amount Visualization ---
                val remaining = state.remaining
                val remainingColor = when {
                    remaining < -0.01 -> MaterialTheme.colorScheme.error // Over-allocated
                    remaining > 0.01 -> MaterialTheme.colorScheme.onSurfaceVariant // Under-allocated
                    else -> MaterialTheme.colorScheme.primary // Exactly balanced
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split Total: ${LocalCurrencySymbol.current}${"%.2f".format(Locale.getDefault(), state.splitsTotal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Remaining: ${LocalCurrencySymbol.current}${"%.2f".format(Locale.getDefault(), remaining)}", 
                         style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                         color = remainingColor)
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = viewModel::addSplit) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Category Split") }
                    TextButton(onClick = viewModel::addPersonSplit) { Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(4.dp)); Text("Split with Person") }
                }
            }
        } else {
            CategoryGrid(
                categories = state.categories,
                selectedIds = state.selectedCategoryIds,
                onCategoryToggled = onCategoryToggled,
                onAddCategory = onAddCategory,
                onEditCategory = onEditCategory,
                onDeleteCategory = onDeleteCategory
            )
        }
    }
}

// Added this to support Category Split Row Dropdown easily
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    label: String, categories: List<Category>, selectedId: String?, onSelected: (String) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCat = categories.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedCat?.name ?: "", onValueChange = {}, readOnly = true, label = { Text(label) },
            leadingIcon = selectedCat?.let { cat -> { Icon(CategoryIcons.getIcon(cat.iconKey), null, tint = parseHexColor(cat.colorHex), modifier = Modifier.size(20.dp)) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = textFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    leadingIcon = { Icon(CategoryIcons.getIcon(cat.iconKey), null, tint = parseHexColor(cat.colorHex)) },
                    onClick = { onSelected(cat.id); expanded = false }
                )
            }
        }
    }
}

private sealed class CategoryGridSlot {
    data class Item(val category: Category) : CategoryGridSlot()
    object Add : CategoryGridSlot()
    object Empty : CategoryGridSlot()
}

@Composable
private fun CategoryGrid(categories: List<Category>, selectedIds: Set<String>, onCategoryToggled: (String, String) -> Unit, onAddCategory: (String, String, String) -> Unit, onEditCategory: (String, String, String, String) -> Unit, onDeleteCategory: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope() 
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryActionDialog by remember { mutableStateOf<Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    val displayCategories = categories.sortedBy { it.name.lowercase() }
    val slots = remember(displayCategories) { val all = displayCategories.map { CategoryGridSlot.Item(it) }.toMutableList<CategoryGridSlot>(); all.add(CategoryGridSlot.Add); all }
    val columns = remember(slots) { slots.chunked(3).map { column -> if (column.size == 3) column else column + List(3 - column.size) { CategoryGridSlot.Empty } } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { column ->
                Column(modifier = Modifier.width(78.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    column.forEach { slot ->
                        when (slot) {
                            is CategoryGridSlot.Item -> {
                                val category = slot.category
                                val isSelected = category.id in selectedIds
                                val catColor = parseHexColor(category.colorHex)
                                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSelected) catColor.copy(alpha = 0.1f) else Color.Transparent).pointerInput(category.id) { detectTapGestures(onTap = { focusManager.clearFocus(); onCategoryToggled(category.id, category.name) }, onLongPress = { focusManager.clearFocus(); categoryActionDialog = category }) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(catColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(imageVector = CategoryIcons.getIcon(category.iconKey), contentDescription = category.name, tint = catColor, modifier = Modifier.size(22.dp)) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(category.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                            CategoryGridSlot.Add -> AddNewCategoryButton(modifier = Modifier.fillMaxWidth(), onClick = { showAddCategoryDialog = true })
                            CategoryGridSlot.Empty -> Spacer(modifier = Modifier.height(68.dp))
                        }
                    }
                }
            }
        }
    }

    categoryActionDialog?.let { category ->
        AlertDialog(onDismissRequest = { categoryActionDialog = null }, title = { Text(category.name) }, text = { Text("What would you like to do?") }, confirmButton = { TextButton(onClick = { categoryActionDialog = null; scope.launch { kotlinx.coroutines.delay(50); categoryToEdit = category } }) { Text("Edit") } }, dismissButton = { TextButton(onClick = { categoryActionDialog = null; scope.launch { kotlinx.coroutines.delay(50); categoryToDelete = category } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } })
    }
    if (showAddCategoryDialog) AddCategoryDialog(isEditMode = false, onDismiss = { showAddCategoryDialog = false }, onSave = { name, iconKey, colorHex -> onAddCategory(name, iconKey, colorHex); showAddCategoryDialog = false })
    categoryToEdit?.let { category -> AddCategoryDialog(initialName = category.name, initialIconKey = category.iconKey, initialColorHex = category.colorHex, isEditMode = true, onDismiss = { categoryToEdit = null }, onSave = { name, iconKey, colorHex -> onEditCategory(category.id, name, iconKey, colorHex); categoryToEdit = null }) }
    categoryToDelete?.let { category -> AlertDialog(onDismissRequest = { categoryToDelete = null }, title = { Text("Delete Category") }, text = { Text("Are you sure you want to delete the '${category.name}' category?") }, confirmButton = { TextButton(onClick = { onDeleteCategory(category.id); categoryToDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") } }) }
}

@Composable
private fun AddNewCategoryButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Add Category", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Add New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AddCategoryDialog(initialName: String = "", initialIconKey: String = "restaurant", initialColorHex: String? = null, isEditMode: Boolean, onDismiss: () -> Unit, onSave: (name: String, iconKey: String, colorHex: String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedIcon by remember(initialIconKey) { mutableStateOf(initialIconKey) }
    val availableIcons = listOf("restaurant", "fastfood", "local_cafe", "directions_car", "directions_transit", "directions_bike", "flight", "local_gas_station", "shopping_bag", "local_grocery_store", "movie", "sports_esports", "palette", "medical_services", "local_hospital", "local_pharmacy", "fitness_center", "spa", "school", "child_care", "receipt", "home", "build", "two_wheeler", "pets", "people", "work", "laptop", "computer", "music_note", "weekend", "trending_up", "card_giftcard", "wallet", "store", "phone_android", "savings", "payments", "more_horiz")
    val availableColors = listOf("#FF5722", "#2196F3", "#9C27B0", "#E91E63", "#4CAF50", "#3F51B5", "#FF9800", "#795548", "#607D8B", "#00BCD4")
    var selectedColor by remember(initialColorHex) { mutableStateOf(initialColorHex ?: availableColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (isEditMode) "Edit Category" else "Add New Category") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text("Select Icon", style = MaterialTheme.typography.titleSmall)
                val iconRows = availableIcons.chunked((availableIcons.size + 1) / 2)
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    iconRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { iconKey ->
                                val isSelected = iconKey == selectedIcon
                                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant).then(if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier).clickable { selectedIcon = iconKey }, contentAlignment = Alignment.Center) { Icon(CategoryIcons.getIcon(iconKey), iconKey, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                }
                Text("Select Color", style = MaterialTheme.typography.titleSmall)
                val colorRows = availableColors.chunked(5)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { colorHex ->
                                val isSelected = colorHex == selectedColor
                                val parsedColor = parseHexColor(colorHex)
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(parsedColor.copy(alpha = if (isSelected) 1f else 0.5f)).then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier).clickable { selectedColor = colorHex }, contentAlignment = Alignment.Center) { if (isSelected) Icon(Icons.Filled.Check, "Selected", tint = Color.White, modifier = Modifier.size(24.dp)) }
                            }
                            repeat(5 - row.size) { Spacer(modifier = Modifier.size(44.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), selectedIcon, selectedColor) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(label: String, accounts: List<com.moneytracker.app.domain.model.Account>, selectedId: String?, onSelected: (String) -> Unit, onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedId }
    
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(value = selectedAccount?.name ?: "", onValueChange = {}, readOnly = true, label = { Text(label) }, leadingIcon = selectedAccount?.let { account -> { Icon(CategoryIcons.getIcon(account.iconKey), null, tint = parseHexColor(account.colorHex), modifier = Modifier.size(20.dp)) } }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = textFieldColors())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            val groupedAccounts = accounts.groupBy { if (it.type.name == "CUSTOM" && !it.customTypeName.isNullOrBlank()) it.customTypeName!! else it.type.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
            groupedAccounts.forEach { (groupName, groupAccounts) ->
                Text(text = groupName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                groupAccounts.forEach { account ->
                    DropdownMenuItem(text = { Text(account.name, color = MaterialTheme.colorScheme.onSurface) }, leadingIcon = { Icon(CategoryIcons.getIcon(account.iconKey), null, tint = parseHexColor(account.colorHex), modifier = Modifier.size(20.dp)) }, onClick = { onSelected(account.id); expanded = false })
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DropdownMenuItem(text = { Text("Create New Account", color = MaterialTheme.colorScheme.primary) }, leadingIcon = { Icon(Icons.Filled.Add, "Add Account", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }, onClick = { expanded = false; onAddAccount() })
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
        OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val newCal = Calendar.getInstance().apply { timeInMillis = date; set(y, m, d) }; onDateSelected(newCal.timeInMillis) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(dateFmt.format(Date(date))) }
        OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> val newCal = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }; onDateSelected(newCal.timeInMillis) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show() }, modifier = Modifier.weight(0.7f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(timeFmt.format(Date(date))) }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
