package com.moneytracker.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

// Helper function to safely extract the Activity from Compose's wrapped Contexts
fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Transactions.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCategoryClick = { categoryId, categoryName, type, month, year ->
                    navController.navigate(
                        Screen.CategoryTransactions.createRoute(categoryId, categoryName, type, month, year)
                    )
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
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
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
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
                onAddBudget = { month, year ->
                    navController.navigate(Screen.AddBudget.createRoute(month, year))
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onBudgetClick = { categoryId, categoryName, month, year ->
                    navController.navigate(
                        Screen.BudgetTransactions.createRoute(categoryId, categoryName, month, year)
                    )
                },
                onEditBudget = { budgetId, categoryId, categoryName, limitAmount, month, year ->
                    navController.navigate(
                        Screen.EditBudget.createRoute(budgetId, categoryId, categoryName, limitAmount, month, year)
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
                },
                navArgument("amount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("note") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("payee") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("fromWidget") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("suggestedCategoryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fromWidget = backStackEntry.arguments?.getBoolean("fromWidget") ?: false
            val context = LocalContext.current

            AddTransactionScreen(
                onNavigateBack = {
                    if (fromWidget) {
                        context.getActivity()?.finish()
                    } else {
                        if (!navController.popBackStack(Screen.Transactions.route, inclusive = false)) {
                            navController.navigate(Screen.Transactions.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                }
            )
        }

        composable(
            route = Screen.AddBudget.route,
            arguments = listOf(
                navArgument("month") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("year") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditBudget.route,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("limitAmount") { type = NavType.StringType },
                navArgument("month") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("year") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddAccount.route) {
            AddAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            AddAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

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

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) {
            AddTransactionScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Transactions.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.CategoryTransactions.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("month") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("year") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            CategoryTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(
            route = Screen.BudgetTransactions.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("month") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("year") { type = NavType.StringType; nullable = true; defaultValue = null }
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
