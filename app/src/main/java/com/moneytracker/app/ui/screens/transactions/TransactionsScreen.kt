package com.moneytracker.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.MonthSelector
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.theme.*

@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
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

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
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
                                TransactionDateHeader(
                                    dateLabel = item.dateLabel,
                                    dayExpense = item.dayExpense,
                                    dayIncome = item.dayIncome,
                                    dayTransfer = item.dayTransfer
                                )
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

        // FAB
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = AccentOrange,
            contentColor = TextPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
        }
    }
}
