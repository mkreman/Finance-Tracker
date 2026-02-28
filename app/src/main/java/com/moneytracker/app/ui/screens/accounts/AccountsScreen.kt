package com.moneytracker.app.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.data.local.database.entities.AccountType
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.components.parseHexColor

@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onAccountClick: (String, String) -> Unit,
    onEditAccount: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currency = LocalCurrencySymbol.current

    var showDeleteDialog by remember { mutableStateOf<Account?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Account", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete \"${showDeleteDialog!!.name}\"? All transactions associated with this account will also be deleted. This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(showDeleteDialog!!.id)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Balance Card with Income/Expense/Total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Overall",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Income so far", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency${formatAmount(state.totalIncome)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Expense so far", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency${formatAmount(state.totalExpense)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (state.totalBalance < 0) "-$currency${formatAmount(kotlin.math.abs(state.totalBalance))}" else "$currency${formatAmount(state.totalBalance)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.totalBalance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cash Section
            if (state.cashAccounts.isNotEmpty()) {
                AccountSection(
                    title = "Cash",
                    accounts = state.cashAccounts,
                    sum = state.cashTotal,
                    expanded = state.cashExpanded,
                    onToggle = { viewModel.setSectionExpanded(AccountType.CASH, !state.cashExpanded) },
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
            }

            // Wallet Section
            if (state.walletAccounts.isNotEmpty()) {
                AccountSection(
                    title = "Wallet",
                    accounts = state.walletAccounts,
                    sum = state.walletTotal,
                    expanded = state.walletExpanded,
                    onToggle = { viewModel.setSectionExpanded(AccountType.WALLET, !state.walletExpanded) },
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
            }

            // Bank Section
            if (state.bankAccounts.isNotEmpty()) {
                AccountSection(
                    title = "Bank Accounts",
                    accounts = state.bankAccounts,
                    sum = state.bankTotal,
                    expanded = state.bankExpanded,
                    onToggle = { viewModel.setSectionExpanded(AccountType.BANK, !state.bankExpanded) },
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
            }

            // Investment Section
            if (state.investmentAccounts.isNotEmpty()) {
                AccountSection(
                    title = "Investments",
                    accounts = state.investmentAccounts,
                    sum = state.investmentTotal,
                    expanded = state.investmentExpanded,
                    onToggle = { viewModel.setSectionExpanded(AccountType.INVESTMENT, !state.investmentExpanded) },
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
            }

            // People Section (Loan/Borrow)
            if (state.peopleAccounts.isNotEmpty()) {
                AccountSection(
                    title = "People",
                    accounts = state.peopleAccounts,
                    sumPositive = state.peoplePositiveTotal,
                    sumNegative = state.peopleNegativeTotal,
                    expanded = state.peopleExpanded,
                    onToggle = { viewModel.setSectionExpanded(AccountType.PEOPLE, !state.peopleExpanded) },
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
            }

            // Custom Accounts Sections (grouped by custom type name)
            if (state.customSections.isNotEmpty()) {
                state.customSections.forEach { section ->
                    AccountSection(
                        title = section.name,
                        accounts = section.accounts,
                        sum = section.total,
                        expanded = section.expanded,
                        onToggle = { viewModel.toggleCustomSection(section.name) },
                        onAccountClick = onAccountClick,
                        onEditAccount = onEditAccount,
                        onDeleteAccount = { showDeleteDialog = it },
                        onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                    )
                }
            }

            // Inactive Accounts Section
            if (state.inactiveAccounts.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inactive Accounts (${state.inactiveAccounts.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.inactiveAccounts.forEach { account ->
                            InactiveAccountCard(
                                account = account,
                                onActivate = { viewModel.activateAccount(account.id) },
                                onDelete = { showDeleteDialog = account }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB
        FloatingActionButton(
            onClick = onAddAccount,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Account")
        }
    }
}

@Composable
private fun AccountSection(
    title: String,
    accounts: List<Account>,
    sum: Double? = null,
    sumPositive: Double? = null,
    sumNegative: Double? = null,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
    onAccountClick: (String, String) -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onDeactivateAccount: (Account) -> Unit
) {
    val currency = LocalCurrencySymbol.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Show detailed "Loaned" and "Borrowed" text if Positive/Negative amounts are passed
        if (sumPositive != null && sumNegative != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = "Loaned: $currency${formatAmount(sumPositive)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Borrowed: $currency${formatAmount(kotlin.math.abs(sumNegative))}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (sum != null) {
            val sumColor = if (sum < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            Text(
                text = if (sum < 0) "-$currency${formatAmount(kotlin.math.abs(sum))}" else "$currency${formatAmount(sum)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = sumColor,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                AccountCard(
                    account = account,
                    onClick = { onAccountClick(account.id, account.name) },
                    onEdit = { onEditAccount(account.id) },
                    onDelete = { onDeleteAccount(account) },
                    onDeactivate = { onDeactivateAccount(account) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeactivate: () -> Unit
) {
    val accentColor = parseHexColor(account.colorHex)
    var showMenu by remember { mutableStateOf(false) }
    val currency = LocalCurrencySymbol.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon(account.iconKey),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (account.type == AccountType.CUSTOM && account.customTypeName != null) {
                    account.customTypeName
                } else {
                    account.type.name.lowercase().replaceFirstChar { it.uppercase() }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (account.currentBalance < 0) "-$currency${formatAmount(kotlin.math.abs(account.currentBalance))}" else "$currency${formatAmount(account.currentBalance)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (account.currentBalance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Deactivate", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { showMenu = false; onDeactivate() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun InactiveAccountCard(
    account: Account,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = parseHexColor(account.colorHex)
    var showMenu by remember { mutableStateOf(false) }
    val currency = LocalCurrencySymbol.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon(account.iconKey),
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = if (account.type == AccountType.CUSTOM && account.customTypeName != null && account.customTypeName.isNotBlank()) {
                    account.customTypeName
                } else {
                    account.type.name.lowercase().replaceFirstChar { it.uppercase() }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }

        Text(
            text = if (account.currentBalance < 0) "-$currency${formatAmount(kotlin.math.abs(account.currentBalance))}" else "$currency${formatAmount(account.currentBalance)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.outline
        )

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Options", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Activate", color = MaterialTheme.colorScheme.tertiary) },
                    onClick = { showMenu = false; onActivate() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}
