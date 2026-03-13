package com.moneytracker.app.ui.screens.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.Budget
import com.moneytracker.app.ui.components.BudgetProgressBar
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.MonthSelector
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.navigation.LocalBottomTabReselect
import com.moneytracker.app.ui.navigation.Screen
import java.util.Calendar
import android.widget.Toast

@Composable
fun BudgetScreen(
    onAddBudget: (Int, Int) -> Unit,
    onAddTransaction: () -> Unit,
    onBudgetClick: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onEditBudget: (String, String, String, Double, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val copyMessage by viewModel.copyMessage.collectAsState()
    val currency = LocalCurrencySymbol.current
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val reselectFlow = LocalBottomTabReselect.current

    LaunchedEffect(Unit) {
        reselectFlow.collect { route ->
            if (route == Screen.Budget.route) {
                scrollState.animateScrollTo(0)
            }
        }
    }

    LaunchedEffect(copyMessage) {
        copyMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearCopyMessage()
        }
    }

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }

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

    if (showReorderDialog) {
        BudgetReorderDialog(
            budgets = state.budgets,
            onMove = viewModel::moveBudget,
            onDismiss = { showReorderDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = { showReorderDialog = true },
                        modifier = Modifier.size(width = 56.dp, height = 35.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = "Rearrange Budgets", modifier = Modifier.size(20.dp))
                    }

                    FilledIconButton(
                        onClick = {
                            val m = currentMonth.get(Calendar.MONTH) + 1
                            val y = currentMonth.get(Calendar.YEAR)
                            onAddBudget(m, y)
                        },
                        modifier = Modifier.size(width = 56.dp, height = 35.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Budget", modifier = Modifier.size(20.dp))
                    }
                }
            }

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
                        val month = currentMonth.get(Calendar.MONTH) + 1
                        val year = currentMonth.get(Calendar.YEAR)
                        BudgetProgressBar(
                            budget = budget,
                            onClick = { onBudgetClick(budget.categoryId, budget.categoryName, month, year) },
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

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = viewModel::copyFromPreviousMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Copy Missing Budgets From Last Month")
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB is now for Add Transaction
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
private fun BudgetReorderDialog(
    budgets: List<Budget>,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rearrange Budgets", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                budgets.forEachIndexed { index, budget ->
                    var dragY by remember(budgets, index) { mutableStateOf(0f) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            budget.categoryName,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        FourDotDragHandle(
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(30.dp)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        dragY += delta
                                    },
                                    onDragStopped = {
                                        if (dragY > 18f && index < budgets.size - 1) {
                                            onMove(index, 1)
                                        } else if (dragY < -18f && index > 0) {
                                            onMove(index, -1)
                                        }
                                        dragY = 0f
                                    }
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FourDotDragHandle(
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.10f
        val x1 = size.width * 0.35f
        val x2 = size.width * 0.65f
        val y1 = size.height * 0.35f
        val y2 = size.height * 0.65f

        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(x1, y1))
        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(x2, y1))
        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(x1, y2))
        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(x2, y2))
    }
}
