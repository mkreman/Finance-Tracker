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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.domain.model.ChartData
import com.moneytracker.app.domain.model.Transaction
import com.moneytracker.app.ui.components.*
import com.moneytracker.app.ui.navigation.LocalBottomTabReselect
import com.moneytracker.app.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCategoryClick: (String, String, String, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onEditTransaction: (String) -> Unit = {},
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

            // Overview title
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

            if (state.selectedOverview == OverviewType.TRANSFER) {
                
                // Filters for Transfer Overview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountFilterDropdown(
                        label = "From",
                        accounts = state.accounts,
                        selectedId = state.selectedFromAccountId,
                        onSelected = viewModel::setFromAccountFilter,
                        modifier = Modifier.weight(1f)
                    )

                    AccountFilterDropdown(
                        label = "To",
                        accounts = state.accounts,
                        selectedId = state.selectedToAccountId,
                        onSelected = viewModel::setToAccountFilter,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Apply filters to list
                val filteredTransfers = state.transferTransactions.filter { txn ->
                    val matchFrom = state.selectedFromAccountId == null || txn.accountId == state.selectedFromAccountId
                    val matchTo = state.selectedToAccountId == null || txn.toAccountId == state.selectedToAccountId
                    matchFrom && matchTo
                }

                // Transfer entries list
                if (filteredTransfers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.transferTransactions.isEmpty()) "No transfers this month" else "No matching transfers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredTransfers.forEach { transfer ->
                            TransferEntryItem(
                                transfer = transfer,
                                onClick = { onEditTransaction(transfer.id) }
                            )
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
                        month = currentMonth.get(Calendar.MONTH) + 1,
                        year = currentMonth.get(Calendar.YEAR),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFilterDropdown(
    label: String,
    accounts: List<Account>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedId }
    val displayText = selectedAccount?.name ?: "All"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("All", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { 
                    onSelected(null)
                    expanded = false 
                }
            )
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { 
                        onSelected(account.id)
                        expanded = false 
                    }
                )
            }
        }
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

@Composable
private fun TransferEntryItem(transfer: Transaction, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(transfer.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon("swap_horiz"),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${transfer.accountName} → ${transfer.toAccountName ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${LocalCurrencySymbol.current}${formatAmount(transfer.totalAmount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
