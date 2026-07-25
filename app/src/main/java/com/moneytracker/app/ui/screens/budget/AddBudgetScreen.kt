package com.moneytracker.app.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.repository.BudgetRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.domain.model.Category
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.parseHexColor
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddBudgetState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedCategoryName: String = "",
    val limitAmount: String = "",
    val isEditMode: Boolean = false,
    val editBudgetId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editBudgetId: String? = savedStateHandle.get<String>("budgetId")
    private val editCategoryId: String? = savedStateHandle.get<String>("categoryId")
    private val editCategoryName: String? = savedStateHandle.get<String>("categoryName")
    private val editLimitAmount: String? = savedStateHandle.get<String>("limitAmount")

    // Extract month and year. Fallback to the current actual month/year if null
    private val selectedMonth = savedStateHandle.get<String>("month")?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val selectedYear = savedStateHandle.get<String>("year")?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

    private val _state = MutableStateFlow(AddBudgetState())
    val state: StateFlow<AddBudgetState> = _state.asStateFlow()

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
        }
        
        val month = selectedMonth
        val year = selectedYear
        
        val startDate = cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endDate = cal.apply {
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        viewModelScope.launch {
            // Get all expense categories
            val allExpenseCategories = categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
            
            if (editBudgetId != null) {
                // Edit mode - show only the selected category
                _state.update {
                    it.copy(
                        categories = allExpenseCategories,
                        isEditMode = true,
                        editBudgetId = editBudgetId,
                        selectedCategoryId = editCategoryId,
                        selectedCategoryName = editCategoryName ?: "",
                        limitAmount = editLimitAmount ?: ""
                    )
                }
            } else {
                // Add mode - filter out categories that already have budgets FOR THIS SPECIFIC MONTH
                budgetRepository.getBudgetsWithSpending(month, year, startDate, endDate).collect { budgets ->
                    val categoriesWithBudgets = budgets.map { it.categoryId }.toSet()
                    val availableCategories = allExpenseCategories.filter { it.id !in categoriesWithBudgets }
                    _state.update { it.copy(categories = availableCategories) }
                }
            }
        }
    }

    fun onCategorySelected(id: String, name: String) {
        _state.update { it.copy(selectedCategoryId = id, selectedCategoryName = name) }
    }

    fun onLimitChange(amount: String) {
        _state.update { it.copy(limitAmount = amount) }
    }

    fun save() {
        val current = _state.value
        val limit = current.limitAmount.toDoubleOrNull() ?: return
        val catId = current.selectedCategoryId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            
            // Save to the selected target month, not the real-world current calendar month
            budgetRepository.saveBudget(
                categoryId = catId,
                limitAmount = limit,
                month = selectedMonth,
                year = selectedYear
            )
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    var showCategoryPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (state.isEditMode) "Edit Budget" else "Add Budget", color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                val canSave = state.selectedCategoryId != null
                        && (state.limitAmount.toDoubleOrNull() ?: 0.0) > 0
                        && !state.isSaving
                
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
            windowInsets = WindowInsets(0.dp) // <-- This removes the top gap!
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Selector
            Text("Category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (!state.isEditMode) Modifier.clickable { showCategoryPicker = true } else Modifier)
                    .padding(16.dp)
            ) {
                Text(
                    text = state.selectedCategoryName.ifEmpty { "Select Category" },
                    color = if (state.selectedCategoryId != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Limit Amount
            OutlinedTextField(
                value = state.limitAmount,
                onValueChange = viewModel::onLimitChange,
                label = { Text("Monthly Limit (${LocalCurrencySymbol.current})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Category Picker Dialog
    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.onCategorySelected(category.id, category.name)
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
                            Text(category.name, color = MaterialTheme.colorScheme.onSurface)
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
