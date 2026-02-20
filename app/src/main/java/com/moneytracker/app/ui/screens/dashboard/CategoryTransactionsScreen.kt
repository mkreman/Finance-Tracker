package com.moneytracker.app.ui.screens.dashboard

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: CategoryTransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val currency = LocalCurrencySymbol.current

    val typeColor = when (viewModel.type.uppercase()) {
        "INCOME" -> IncomeGreen
        "TRANSFER" -> TransferBlue
        else -> ExpenseRed
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("${viewModel.categoryName} (${viewModel.type.lowercase().replaceFirstChar { it.uppercase() }})", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                        typeName = viewModel.type.lowercase().replaceFirstChar { it.uppercase() }
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
private fun CategorySummaryCard(
    totalAmount: Double,
    transactionCount: Int,
    highestAmount: Double,
    averageAmount: Double,
    currency: String,
    typeColor: androidx.compose.ui.graphics.Color,
    typeName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Total Amount - prominent display
            Text(
                text = "Total $typeName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currency${formatAmount(totalAmount)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = typeColor
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = DividerColor, thickness = 0.5.dp)
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
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
