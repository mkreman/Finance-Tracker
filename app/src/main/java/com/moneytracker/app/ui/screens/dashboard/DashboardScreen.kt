package com.moneytracker.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.repository.TransactionRepository.Companion.UNKNOWN_TRANSFER_TO_ACCOUNT_ID
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.ui.components.*
import com.moneytracker.app.ui.navigation.LocalBottomTabReselect
import com.moneytracker.app.ui.navigation.Screen
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCategoryClick: (String, String, String, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onAddTransaction: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    val scrollState = rememberScrollState()
    val reselectFlow = LocalBottomTabReselect.current

    LaunchedEffect(Unit) {
        reselectFlow.collect { route ->
            if (route == Screen.Dashboard.route) {
                scrollState.animateScrollTo(0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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

            Text(
                text = when (state.selectedOverview) {
                    OverviewType.EXPENSE -> "Expense Overview"
                    OverviewType.INCOME -> "Income Overview"
                    OverviewType.TRANSFER -> "Transfer Overview"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            DashboardDonutSection(
                state = state,
                currentMonth = currentMonth,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB - Added for consistency across tabs
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
private fun DashboardDonutSection(
    state: DashboardState,
    currentMonth: Calendar?,
    onCategoryClick: (String, String, String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val transferPalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.inversePrimary,
        MaterialTheme.colorScheme.outline
    )

    val donutData = when (state.selectedOverview) {
        OverviewType.EXPENSE -> state.categorySpending
        OverviewType.INCOME -> state.categoryIncome
        OverviewType.TRANSFER -> buildTransferDonutData(state, transferPalette)
    }

    val centerLabel = when (state.selectedOverview) {
        OverviewType.EXPENSE -> "Expense"
        OverviewType.INCOME -> "Income"
        OverviewType.TRANSFER -> "Transfer"
    }

    val totalAmount = when (state.selectedOverview) {
        OverviewType.EXPENSE -> state.totalExpense
        OverviewType.INCOME -> state.totalIncome
        OverviewType.TRANSFER -> state.totalTransfer
    }

    if (donutData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No ${centerLabel.lowercase()} recorded this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                data = donutData,
                totalAmount = totalAmount,
                centerLabel = centerLabel
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        currentMonth?.let { calendar ->
            ClickableLegend(
                data = donutData,
                type = when (state.selectedOverview) {
                    OverviewType.INCOME -> "INCOME"
                    OverviewType.TRANSFER -> "TRANSFER"
                    else -> "EXPENSE"
                },
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR),
                onCategoryClick = onCategoryClick
            )
        }
    }
}

private fun buildTransferDonutData(
    state: DashboardState,
    palette: List<Color>
): List<ChartData> {
    val grouped = state.transferTransactions
        .groupBy {
            val accountId = it.toAccountId ?: UNKNOWN_TRANSFER_TO_ACCOUNT_ID
            val accountName = it.toAccountName?.takeIf(String::isNotBlank) ?: "Unknown"
            accountId to accountName
        }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        .toList()
        .sortedByDescending { it.second }

    if (grouped.isEmpty()) return emptyList()

    val total = grouped.sumOf { it.second }
    return grouped.mapIndexed { index, (account, amount) ->
        val (accountId, accountName) = account
        ChartData(
            categoryId = accountId,
            categoryName = accountName,
            amount = amount,
            color = palette[index % palette.size],
            iconKey = "swap_horiz",
            percentage = if (total == 0.0) 0f else ((amount / total) * 100.0).toFloat()
        )
    }
}

@Composable
private fun ClickableLegend(
    data: List<ChartData>,
    type: String,
    month: Int,
    year: Int,
    onCategoryClick: (String, String, String, Int, Int) -> Unit
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
                        onCategoryClick(item.categoryId, item.categoryName, type, month, year)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${LocalCurrencySymbol.current}${formatAmount(item.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format("%.1f", item.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
