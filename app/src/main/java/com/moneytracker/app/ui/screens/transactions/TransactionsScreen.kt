package com.moneytracker.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.MonthSelector
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.navigation.LocalBottomTabReselect
import com.moneytracker.app.ui.navigation.Screen

@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val reselectFlow = LocalBottomTabReselect.current
    
    // Tools to manage keyboard and focus
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Listen for reselect events to scroll to top
    LaunchedEffect(Unit) {
        reselectFlow.collect { route ->
            if (route == Screen.Transactions.route) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Automatically request focus when the search bar opens
    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Clear focus (hides keyboard) and close search when tapping outside
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    if (isSearchOpen) {
                        isSearchOpen = false
                        viewModel.onSearchQueryChange("")
                    }
                })
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with Search Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Updated to match the oval shape of the save/add buttons
                FilledIconButton(
                    onClick = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) {
                            viewModel.onSearchQueryChange("")
                        }
                    },
                    modifier = Modifier.size(width = 56.dp, height = 35.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (isSearchOpen) "Close search" else "Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Search Bar (Shown when search icon is tapped)
            if (isSearchOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester), // Attach the focus requester here
                    placeholder = { Text("Search notes or payee...") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
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
                    onNextMonth = viewModel::nextMonth,
                    onSelectAllTime = viewModel::selectAllTime
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No transactions matching \"$searchQuery\"" else "No transactions for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
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
                                    onClick = { 
                                        focusManager.clearFocus() // Hide keyboard if navigating away
                                        onEditTransaction(item.transaction.id) 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                focusManager.clearFocus() // Hide keyboard if navigating to Add Screen
                onAddTransaction()
            },
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
