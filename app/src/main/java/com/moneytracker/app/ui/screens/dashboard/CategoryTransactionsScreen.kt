package com.moneytracker.app.ui.screens.dashboard

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.components.formatAmount
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: CategoryTransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val percentage by viewModel.percentage.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currency = LocalCurrencySymbol.current

    val typeColor = when (viewModel.type.uppercase()) {
        "INCOME" -> MaterialTheme.colorScheme.tertiary
        "TRANSFER" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("${viewModel.categoryName} (${viewModel.type.lowercase().replaceFirstChar { it.uppercase() }})", color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            windowInsets = WindowInsets(0.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Month Selector
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CategoryMonthSelector(
                currentMonth = currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onSelectAll = viewModel::selectAll
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Summary Card
                item(key = "summary") {
                    CategorySummaryCard(
                        totalAmount = summary.totalAmount,
                        transactionCount = summary.transactionCount,
                        highestAmount = summary.highestAmount,
                        averageAmount = summary.averageAmount,
                        currency = currency,
                        typeColor = typeColor,
                        typeName = viewModel.type.lowercase().replaceFirstChar { it.uppercase() },
                        percentage = percentage
                    )
                }

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
fun CategoryMonthSelector(
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
private fun CategorySummaryCard(
    totalAmount: Double,
    transactionCount: Int,
    highestAmount: Double,
    averageAmount: Double,
    currency: String,
    typeColor: androidx.compose.ui.graphics.Color,
    typeName: String,
    percentage: Float = 0f
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Column {
            // Total Amount - prominent display
            Text(
                text = "Total $typeName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "$currency${formatAmount(totalAmount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = typeColor
                )

                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(
                    label = "Transactions",
                    value = transactionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryStatItem(
                    label = "Average",
                    value = "$currency${formatAmount(averageAmount)}",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatItem(
                    label = "Highest",
                    value = "$currency${formatAmount(highestAmount)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
