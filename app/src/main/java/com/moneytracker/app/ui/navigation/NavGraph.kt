package com.moneytracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moneytracker.app.ui.screens.accounts.AccountTransactionsScreen
import com.moneytracker.app.ui.screens.accounts.AccountsScreen
import com.moneytracker.app.ui.screens.accounts.AddAccountScreen
import com.moneytracker.app.ui.screens.budget.AddBudgetScreen
import com.moneytracker.app.ui.screens.budget.BudgetScreen
import com.moneytracker.app.ui.screens.budget.BudgetTransactionsScreen
import com.moneytracker.app.ui.screens.dashboard.CategoryTransactionsScreen
import com.moneytracker.app.ui.screens.dashboard.DashboardScreen
import com.moneytracker.app.ui.screens.settings.SettingsScreen
import com.moneytracker.app.ui.screens.transactions.AddTransactionScreen
import com.moneytracker.app.ui.screens.transactions.TransactionsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Transactions.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCategoryClick = { categoryId, categoryName, type ->
                    navController.navigate(
                        Screen.CategoryTransactions.createRoute(categoryId, categoryName, type)
                    )
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                onAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                },
                onAccountClick = { accountId, accountName ->
                    navController.navigate(
                        Screen.AccountTransactions.createRoute(accountId, accountName)
                    )
                },
                onEditAccount = { accountId ->
                    navController.navigate(Screen.EditAccount.createRoute(accountId))
                }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                onAddBudget = {
                    navController.navigate(Screen.AddBudget.route)
                },
                onBudgetClick = { categoryId, categoryName ->
                    navController.navigate(
                        Screen.BudgetTransactions.createRoute(categoryId, categoryName)
                    )
                },
                onEditBudget = { budgetId, categoryId, categoryName, limitAmount ->
                    navController.navigate(
                        Screen.EditBudget.createRoute(budgetId, categoryId, categoryName, limitAmount)
                    )
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddAccount.route) {
            AddAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddBudget.route) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Edit Budget
        composable(
            route = Screen.EditBudget.route,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("limitAmount") { type = NavType.StringType }
            )
        ) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Edit Account
        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            AddAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Account Transactions
        composable(
            route = Screen.AccountTransactions.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType },
                navArgument("accountName") { type = NavType.StringType }
            )
        ) {
            AccountTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        // Edit Transaction
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Category Transactions
        composable(
            route = Screen.CategoryTransactions.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) {
            CategoryTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        // Budget Transactions
        composable(
            route = Screen.BudgetTransactions.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) {
            BudgetTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }
    }
}
