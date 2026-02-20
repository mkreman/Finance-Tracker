package com.moneytracker.app.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.domain.model.Account
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.formatAmount
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.*

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
            title = { Text("Delete Account", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete \"${showDeleteDialog!!.name}\"? All transactions associated with this account will also be deleted. This cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(showDeleteDialog!!.id)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Balance Card with Income/Expense/Total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackgroundElevated)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Overall",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Income so far", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "$currency${formatAmount(state.totalIncome)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Expense so far", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "$currency${formatAmount(state.totalExpense)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Balance", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            if (state.totalBalance < 0) "-$currency${formatAmount(kotlin.math.abs(state.totalBalance))}" else "$currency${formatAmount(state.totalBalance)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.totalBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wallet Section
            if (state.cashAccounts.isNotEmpty()) {
                AccountSection(
                    title = "Wallet",
                    accounts = state.cashAccounts,
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
                    onAccountClick = onAccountClick,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = { showDeleteDialog = it },
                    onDeactivateAccount = { viewModel.deactivateAccount(it.id) }
                )
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
                        color = TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextTertiary
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
            containerColor = AccentOrange,
            contentColor = TextPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Account")
        }
    }
}

@Composable
private fun AccountSection(
    title: String,
    accounts: List<Account>,
    onAccountClick: (String, String) -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onDeactivateAccount: (Account) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

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
            .background(CardBackground)
            .clickable(onClick = onClick)
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
                color = TextPrimary
            )
            Text(
                text = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Text(
            text = if (account.currentBalance < 0) "-$currency${formatAmount(kotlin.math.abs(account.currentBalance))}" else "$currency${formatAmount(account.currentBalance)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (account.currentBalance >= 0) IncomeGreen else ExpenseRed
        )

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = TextPrimary) },
                    onClick = { showMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Deactivate", color = TextSecondary) },
                    onClick = { showMenu = false; onDeactivate() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ExpenseRed) },
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
            .background(CardBackground.copy(alpha = 0.5f))
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
                color = TextTertiary
            )
            Text(
                text = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary.copy(alpha = 0.6f)
            )
        }

        Text(
            text = if (account.currentBalance < 0) "-$currency${formatAmount(kotlin.math.abs(account.currentBalance))}" else "$currency${formatAmount(account.currentBalance)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextTertiary
        )

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Options", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Activate", color = IncomeGreen) },
                    onClick = { showMenu = false; onActivate() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ExpenseRed) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}
