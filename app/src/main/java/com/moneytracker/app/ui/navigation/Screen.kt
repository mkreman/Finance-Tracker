package com.moneytracker.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )

    object Transactions : Screen(
        route = "transactions",
        title = "Transactions",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )

    object Accounts : Screen(
        route = "accounts",
        title = "Accounts",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    )

    object Budget : Screen(
        route = "budget",
        title = "Budget",
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChart
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    object AddTransaction : Screen(
        route = "add_transaction?type={type}&amount={amount}&note={note}&payee={payee}&fromWidget={fromWidget}&suggestedCategoryId={suggestedCategoryId}",
        title = "Add Transaction"
    ) {
        fun createRoute(
            type: String? = null,
            amount: String? = null,
            note: String? = null,
            payee: String? = null,
            fromWidget: Boolean = false,
            suggestedCategoryId: String? = null
        ): String {
            val parts = mutableListOf<String>()
            if (type != null) parts.add("type=${Uri.encode(type)}")
            if (amount != null) parts.add("amount=${Uri.encode(amount)}")
            if (note != null) parts.add("note=${Uri.encode(note)}")
            if (payee != null) parts.add("payee=${Uri.encode(payee)}")
            if (fromWidget) parts.add("fromWidget=true")
            if (suggestedCategoryId != null) parts.add("suggestedCategoryId=${Uri.encode(suggestedCategoryId)}")
            return if (parts.isEmpty()) "add_transaction" else "add_transaction?${parts.joinToString("&")}" 
        }
    }

    object AddAccount : Screen(
        route = "add_account",
        title = "Add Account"
    )

    object AddBudget : Screen(
        route = "add_budget?month={month}&year={year}",
        title = "Add Budget"
    ) {
        fun createRoute(month: Int? = null, year: Int? = null): String {
            val parts = mutableListOf<String>()
            if (month != null) parts.add("month=$month")
            if (year != null) parts.add("year=$year")
            return if (parts.isEmpty()) "add_budget" else "add_budget?${parts.joinToString("&")}"
        }
    }

    object EditBudget : Screen(
        route = "edit_budget/{budgetId}/{categoryId}/{categoryName}/{limitAmount}?month={month}&year={year}",
        title = "Edit Budget"
    ) {
        fun createRoute(budgetId: String, categoryId: String, categoryName: String, limitAmount: Double, month: Int? = null, year: Int? = null): String {
            val base = "edit_budget/$budgetId/$categoryId/$categoryName/$limitAmount"
            val parts = mutableListOf<String>()
            if (month != null) parts.add("month=$month")
            if (year != null) parts.add("year=$year")
            return if (parts.isEmpty()) base else "$base?${parts.joinToString("&")}"
        }
    }

    object EditAccount : Screen(
        route = "edit_account/{accountId}",
        title = "Edit Account"
    ) {
        fun createRoute(accountId: String) = "edit_account/$accountId"
    }

    object AccountTransactions : Screen(
        route = "account_transactions/{accountId}/{accountName}",
        title = "Account Transactions"
    ) {
        fun createRoute(accountId: String, accountName: String) = "account_transactions/$accountId/$accountName"
    }

    object EditTransaction : Screen(
        route = "edit_transaction/{transactionId}",
        title = "Edit Transaction"
    ) {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    object CategoryTransactions : Screen(
        route = "category_transactions/{categoryId}/{categoryName}/{type}?month={month}&year={year}",
        title = "Category Transactions"
    ) {
        fun createRoute(categoryId: String, categoryName: String, type: String, month: Int? = null, year: Int? = null): String {
            val base = "category_transactions/$categoryId/$categoryName/$type"
            val parts = mutableListOf<String>()
            if (month != null) parts.add("month=$month")
            if (year != null) parts.add("year=$year")
            return if (parts.isEmpty()) base else "$base?${parts.joinToString("&")}" 
        }
    }

    object BudgetTransactions : Screen(
        route = "budget_transactions/{categoryId}/{categoryName}?month={month}&year={year}",
        title = "Budget Transactions"
    ) {
        fun createRoute(categoryId: String, categoryName: String, month: Int? = null, year: Int? = null): String {
            val base = "budget_transactions/$categoryId/$categoryName"
            val parts = mutableListOf<String>()
            if (month != null) parts.add("month=$month")
            if (year != null) parts.add("year=$year")
            return if (parts.isEmpty()) base else "$base?${parts.joinToString("&")}" 
        }
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Transactions, Accounts, Budget, Settings)
    }
}
