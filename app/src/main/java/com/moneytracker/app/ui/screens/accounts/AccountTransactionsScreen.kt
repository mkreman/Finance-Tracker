package com.moneytracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransactionsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: AccountTransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val summary by viewModel.summary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(viewModel.accountName, color = MaterialTheme.colorScheme.onSurface) },
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

        Spacer(modifier = Modifier.height(8.dp))
        
        // Month Selector
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AccountMonthSelector(
                currentMonth = currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onSelectAll = viewModel::selectAll
            )
        }
        
        // Unified Summary Card handling all calculations cleanly
        AccountSummaryCard(summary = summary)

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions for this period",
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
                    items = transactions,
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
fun AccountMonthSelector(
    currentMonth: Calendar?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthText = if (currentMonth == null) {
        "All Time"
    } else {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(currentMonth.time)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = monthText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (currentMonth != null) {
            TextButton(
                onClick = onSelectAll,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Show All Time", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AccountSummaryCard(
    summary: AccountSummary,
    modifier: Modifier = Modifier
) {
    val currency = LocalCurrencySymbol.current
    
    // Determine signage cleanly for UI formatting
    val signFlow = if (summary.netFlow < 0) "-" else ""
    val displayNetFlow = abs(summary.netFlow)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Net Flow",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$signFlow$currency${formatAmount(displayNetFlow)}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (summary.netFlow >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryColumn(
                    title = "Income",
                    amount = summary.income,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                SummaryColumn(
                    title = "Spent",
                    amount = summary.expense,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                SummaryColumn(
                    title = "Net Transfer",
                    amount = summary.netTransfer,
                    color = if (summary.netTransfer >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    title: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val currency = LocalCurrencySymbol.current
    val isNegative = amount < 0
    val displayAmount = abs(amount)
    val sign = if (isNegative) "-" else ""

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$sign$currency${formatAmount(displayAmount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
    }
}
