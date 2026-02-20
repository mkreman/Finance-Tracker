package com.moneytracker.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.domain.model.Transaction
import com.moneytracker.app.ui.components.*
import com.moneytracker.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onCategoryClick: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Cards Row — clickable to change overview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Expense",
                amount = state.totalExpense,
                type = SummaryType.EXPENSE,
                isSelected = state.selectedOverview == OverviewType.EXPENSE,
                onClick = { viewModel.selectOverview(OverviewType.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Income",
                amount = state.totalIncome,
                type = SummaryType.INCOME,
                isSelected = state.selectedOverview == OverviewType.INCOME,
                onClick = { viewModel.selectOverview(OverviewType.INCOME) },
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Transfer",
                amount = state.totalTransfer,
                type = SummaryType.TRANSFER,
                isSelected = state.selectedOverview == OverviewType.TRANSFER,
                onClick = { viewModel.selectOverview(OverviewType.TRANSFER) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Overview title
        Text(
            text = when (state.selectedOverview) {
                OverviewType.EXPENSE -> "Expense Overview"
                OverviewType.INCOME -> "Income Overview"
                OverviewType.TRANSFER -> "Transfer Overview"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.selectedOverview == OverviewType.TRANSFER) {
            // Transfer entries list
            if (state.transferTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transfers this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.transferTransactions.forEach { transfer ->
                        TransferEntryItem(transfer = transfer)
                    }
                }
            }
        } else {
            val chartData = if (state.selectedOverview == OverviewType.INCOME) state.categoryIncome else state.categorySpending
            val chartTotal = if (state.selectedOverview == OverviewType.INCOME) state.totalIncome else state.totalExpense
            val chartLabel = if (state.selectedOverview == OverviewType.INCOME) "Income" else "Expense"

            if (chartData.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        data = chartData,
                        totalAmount = chartTotal,
                        centerLabel = chartLabel
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Clickable Category Legend
                ClickableLegend(
                    data = chartData,
                    type = if (state.selectedOverview == OverviewType.INCOME) "INCOME" else "EXPENSE",
                    onCategoryClick = onCategoryClick
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ${chartLabel.lowercase()} recorded this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun ClickableLegend(
    data: List<ChartData>,
    type: String,
    onCategoryClick: (String, String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        data.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // extract categoryId from ChartData - we need to pass it
                        onCategoryClick(item.categoryId, item.categoryName, type)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = item.color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${LocalCurrencySymbol.current}${formatAmount(item.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format("%.1f", item.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TransferEntryItem(transfer: Transaction) {
    val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(transfer.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TransferBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon("swap_horiz"),
                contentDescription = null,
                tint = TransferBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${transfer.accountName} → ${transfer.toAccountName ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Text(
            text = "${LocalCurrencySymbol.current}${formatAmount(transfer.totalAmount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TransferBlue
        )
    }
}
