package com.moneytracker.app.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.Budget
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.MonthSelector
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.components.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTransactionsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: BudgetTransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currency = LocalCurrencySymbol.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("${viewModel.categoryName} Budget", color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            windowInsets = WindowInsets(0.dp)
        )

        // Month Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MonthSelector(
                currentMonth = currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth
            )
        }

        // Budget Summary Card
        state.budget?.let { budget ->
            BudgetSummaryCard(budget = budget, currency = currency)
        }

        if (state.transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No expenses this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = state.transactions,
                    key = { item ->
                        when (item) {
                            is TransactionListItem.Header -> "header_${item.dateMillis}"
                            is TransactionListItem.Entry -> "txn_${item.transaction.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is TransactionListItem.Header -> {
                            TransactionDateHeader(dateLabel = item.dateLabel, dayExpense = item.dayExpense, dayIncome = item.dayIncome, dayTransfer = item.dayTransfer)
                        }
                        is TransactionListItem.Entry -> {
                            TransactionItem(
                                transaction = item.transaction,
                                onClick = { onEditTransaction(item.transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(budget: Budget, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Budget Limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$currency${formatAmount(budget.limitAmount)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$currency${formatAmount(budget.spentAmount)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (budget.isOverBudget) "Exceeded By" else "Remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (budget.isOverBudget)
                    "$currency${formatAmount(budget.spentAmount - budget.limitAmount)}"
                else
                    "$currency${formatAmount(budget.remaining)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
