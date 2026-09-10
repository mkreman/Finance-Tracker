package com.moneytracker.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.moneytracker.app.ui.screens.investments.AddInvestmentScreen
import com.moneytracker.app.ui.screens.investments.InvestmentsScreen
import com.moneytracker.app.ui.screens.investments.StockDetailScreen
import com.moneytracker.app.ui.screens.settings.SettingsScreen
import com.moneytracker.app.ui.screens.transactions.AccountSelectionTarget
import com.moneytracker.app.ui.screens.transactions.AddTransactionScreen
import com.moneytracker.app.ui.screens.transactions.AddTransactionViewModel
import com.moneytracker.app.ui.screens.transactions.TransactionsScreen

// Helper function to safely extract the Activity from Compose's wrapped Contexts
fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    pagerState: PagerState,
    onSelectTab: (Screen) -> Unit
) {
    val onEditTransactionClick: (String, Boolean) -> Unit = { transactionId, isInvestment ->
        if (isInvestment) {
            navController.navigate(Screen.AddInvestment.createRoute(transactionId = transactionId))
        } else {
            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.MainTabs.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Screen.MainTabs.route) {
            MainTabsPager(
                navController = navController,
                pagerState = pagerState,
                onEditTransaction = onEditTransactionClick
            )
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
                },
                navArgument("suggestedAccountId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fromWidget = backStackEntry.arguments?.getBoolean("fromWidget") ?: false
            val context = LocalContext.current
            val viewModel: AddTransactionViewModel = androidx.hilt.navigation.compose.hiltViewModel(backStackEntry)
            val createdAccountId by backStackEntry.savedStateHandle
                .getStateFlow<String?>("createdAccountId", null)
                .collectAsState()
            val createdAccountTarget by backStackEntry.savedStateHandle
                .getStateFlow<String?>("createdAccountTarget", null)
                .collectAsState()

            AddTransactionScreen(
                onNavigateBack = {
                    if (fromWidget) {
                        context.getActivity()?.finish()
                    } else {
                        onSelectTab(Screen.Transactions)
                        if (!navController.popBackStack(Screen.MainTabs.route, inclusive = false)) {
                            navController.navigate(Screen.MainTabs.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onAddAccount = { target ->
                    backStackEntry.savedStateHandle["createdAccountTarget"] = target.name
                    // Check if it's a Person target, and pass the explicit type!
                    val accountType = if (target.name == "PERSON") "PEOPLE" else null
                    navController.navigate(Screen.AddAccount.createRoute(accountType))
                },
                createdAccountId = createdAccountId,
                createdAccountTarget = createdAccountTarget,
                onCreatedAccountConsumed = {
                    backStackEntry.savedStateHandle["createdAccountId"] = null
                    backStackEntry.savedStateHandle["createdAccountTarget"] = null
                },
                viewModel = viewModel
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

        composable(
            route = Screen.AddAccount.route,
            arguments = listOf(
                navArgument("accountType") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            AddAccountScreen(
                onNavigateBack = { createdAccountId ->
                    if (!createdAccountId.isNullOrBlank()) {
                        navController.previousBackStackEntry?.savedStateHandle?.set("createdAccountId", createdAccountId)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            AddAccountScreen(
                onNavigateBack = { _ -> navController.popBackStack() }
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
                onEditTransaction = onEditTransactionClick
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewModel: AddTransactionViewModel = androidx.hilt.navigation.compose.hiltViewModel(backStackEntry)
            val createdAccountId by backStackEntry.savedStateHandle
                .getStateFlow<String?>("createdAccountId", null)
                .collectAsState()
            val createdAccountTarget by backStackEntry.savedStateHandle
                .getStateFlow<String?>("createdAccountTarget", null)
                .collectAsState()

            AddTransactionScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.MainTabs.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onAddAccount = { target ->
                    backStackEntry.savedStateHandle["createdAccountTarget"] = target.name
                    val accountType = if (target.name == "PERSON") "PEOPLE" else null
                    navController.navigate(Screen.AddAccount.createRoute(accountType))
                },
                createdAccountId = createdAccountId,
                createdAccountTarget = createdAccountTarget,
                onCreatedAccountConsumed = {
                    backStackEntry.savedStateHandle["createdAccountId"] = null
                    backStackEntry.savedStateHandle["createdAccountTarget"] = null
                },
                viewModel = viewModel
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
                onEditTransaction = onEditTransactionClick
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
                onEditTransaction = onEditTransactionClick
            )
        }

        // Investments Overview Screen Target
        composable("investments") {
            InvestmentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddInvestment = { navController.navigate("add_investment") },
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) }
            )
        }

        // Stock Summary / Details Screen Target
        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) {
            StockDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onBuyMore = { symbol ->
                    navController.navigate(Screen.AddInvestment.createRoute(symbol = symbol, type = "INVEST"))
                },
                onWithdraw = { symbol ->
                    navController.navigate(Screen.AddInvestment.createRoute(symbol = symbol, type = "WITHDRAW"))
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddInvestment.createRoute(transactionId = transactionId))
                }
            )
        }

        // Add / Edit Investment Screen Target
        composable(
            route = "add_investment?symbol={symbol}&type={type}&transactionId={transactionId}",
            arguments = listOf(
                navArgument("symbol") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddInvestmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabsPager(
    navController: NavHostController,
    pagerState: PagerState,
    onEditTransaction: (String, Boolean) -> Unit
) {
    val tabs = Screen.bottomNavItems

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (tabs[page]) {
            Screen.Dashboard -> {
                DashboardScreen(
                    onCategoryClick = { categoryId, categoryName, type, month, year ->
                        navController.navigate(
                            Screen.CategoryTransactions.createRoute(categoryId, categoryName, type, month, year)
                        )
                    },
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    },
                    onEditTransaction = onEditTransaction,
                    onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.createRoute())
                    }
                )
            }
            Screen.Transactions -> {
                TransactionsScreen(
                    onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.createRoute())
                    },
                    onEditTransaction = onEditTransaction
                )
            }
            Screen.Accounts -> {
                AccountsScreen(
                    onAddAccount = {
                        navController.navigate(Screen.AddAccount.createRoute())
                    },
                    onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.createRoute())
                    },
                    onAddInvestment = {
                        navController.navigate("investments")
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
            Screen.Budget -> {
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
            Screen.Settings -> {
                SettingsScreen()
            }
            else -> Unit
        }
    }
}
