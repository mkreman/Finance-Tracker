package com.moneytracker.app.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.ui.components.BudgetProgressBar
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.MonthSelector
import com.moneytracker.app.ui.components.formatAmount
import java.util.Calendar

@Composable
fun BudgetScreen(
    onAddBudget: (Int, Int) -> Unit,
    onBudgetClick: (String, String) -> Unit = { _, _ -> },
    onEditBudget: (String, String, String, Double, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currency = LocalCurrencySymbol.current

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Budget", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete this budget?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBudget(showDeleteDialog!!)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Budget",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Month Selector
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MonthSelector(
                    currentMonth = currentMonth,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Budget Summary
            if (state.budgets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency${formatAmount(state.totalBudget)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency${formatAmount(state.totalSpent)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Remaining", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (state.totalBudget - state.totalSpent < 0) "-$currency${formatAmount(kotlin.math.abs(state.totalBudget - state.totalSpent))}" else "$currency${formatAmount(state.totalBudget - state.totalSpent)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (state.totalSpent > state.totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Budget Items
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.budgets.forEach { budget ->
                        BudgetProgressBar(
                            budget = budget,
                            onClick = { onBudgetClick(budget.categoryId, budget.categoryName) },
                            onEdit = { 
                                val m = currentMonth.get(Calendar.MONTH) + 1
                                val y = currentMonth.get(Calendar.YEAR)
                                onEditBudget(budget.id, budget.categoryId, budget.categoryName, budget.limitAmount, m, y) 
                            },
                            onDelete = { showDeleteDialog = budget.id }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No budgets set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add a budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB
        FloatingActionButton(
            onClick = {
                val m = currentMonth.get(Calendar.MONTH) + 1
                val y = currentMonth.get(Calendar.YEAR)
                onAddBudget(m, y)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Budget")
        }
    }
}
