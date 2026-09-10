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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import com.moneytracker.app.data.local.database.entities.InvestmentEntity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onStockClick: (String) -> Unit = {},
    onEditTransaction: (String, Boolean) -> Unit = { _, _ -> },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Top Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val currency = LocalCurrencySymbol.current
                            Text(
                                if (state.totalAccountBalance < 0) "-$currency${formatAmount(kotlin.math.abs(state.totalAccountBalance))}" else "$currency${formatAmount(state.totalAccountBalance)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (state.totalAccountBalance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val currency = LocalCurrencySymbol.current
                            val net = state.totalIncome - state.totalExpense - state.totalInvestment
                            val isNetPositive = net >= 0
                            Text(
                                "${if (isNetPositive) "+" else "-"}$currency${formatAmount(kotlin.math.abs(net))}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isNetPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards Row — all 4 options in one row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    title = "Invest",
                    amount = state.totalInvestment,
                    type = SummaryType.INVESTMENT,
                    isSelected = state.selectedOverview == OverviewType.INVESTMENT,
                    onClick = { viewModel.selectOverview(OverviewType.INVESTMENT) },
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
                    OverviewType.INVESTMENT -> "Investment Overview"
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
                onStockClick = onStockClick,
                onEditTransaction = onEditTransaction,
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
    onStockClick: (String) -> Unit,
    onEditTransaction: (String, Boolean) -> Unit,
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
        OverviewType.INVESTMENT -> state.investmentSpending
    }

    val centerLabel = when (state.selectedOverview) {
        OverviewType.EXPENSE -> "Expense"
        OverviewType.INCOME -> "Income"
        OverviewType.TRANSFER -> "Transfer"
        OverviewType.INVESTMENT -> "Investment"
    }

    val totalAmount = when (state.selectedOverview) {
        OverviewType.EXPENSE -> state.totalExpense
        OverviewType.INCOME -> state.totalIncome
        OverviewType.TRANSFER -> state.totalTransfer
        OverviewType.INVESTMENT -> state.totalInvestment
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
        if (state.selectedOverview == OverviewType.INVESTMENT && state.investmentHoldings.isNotEmpty()) {
            val isProfit = state.investmentTotalPnl >= 0
            val pnlColor = if (isProfit) Color(0xFF00C853) else MaterialTheme.colorScheme.error
            val pnlSign = if (isProfit) "+" else ""
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Portfolio Valuation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${LocalCurrencySymbol.current}${formatAmount(state.investmentCurrentValuation)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pnlColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                contentDescription = null,
                                tint = pnlColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$pnlSign${LocalCurrencySymbol.current}${formatAmount(state.investmentTotalPnl)} ($pnlSign${String.format(Locale.US, "%.2f", state.investmentTotalPnlPercent)}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = pnlColor
                            )
                        }
                    }
                }
            }
        }

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
                    OverviewType.INVESTMENT -> "INVESTMENT"
                    else -> "EXPENSE"
                },
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR),
                holdings = state.investmentHoldings,
                onCategoryClick = onCategoryClick,
                onStockClick = onStockClick
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
    holdings: List<InvestmentEntity> = emptyList(),
    onCategoryClick: (String, String, String, Int, Int) -> Unit,
    onStockClick: (String) -> Unit = {}
) {
    val holdingsMap = remember(holdings) { holdings.associateBy { it.symbol } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        data.forEach { item ->
            val stockHolding = if (type == "INVESTMENT") holdingsMap[item.categoryId] else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (type == "INVESTMENT") {
                            onStockClick(item.categoryId)
                        } else {
                            onCategoryClick(item.categoryId, item.categoryName, type, month, year)
                        }
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${LocalCurrencySymbol.current}${formatAmount(item.amount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stockHolding?.currentPrice != null) {
                        val isProf = stockHolding.totalPnl >= 0
                        val col = if (isProf) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                        val sign = if (isProf) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.US, "%.1f", stockHolding.pnlPercentage)}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = col
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", item.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
