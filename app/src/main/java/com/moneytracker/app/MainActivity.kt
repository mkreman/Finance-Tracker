package com.moneytracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.navigation.BottomNavBar
import com.moneytracker.app.ui.navigation.NavGraph
import com.moneytracker.app.ui.navigation.Screen
import com.moneytracker.app.ui.theme.DarkBackground
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val currencyCode by userPreferences.currencyCode.collectAsState(initial = "INR")
            val currencySymbol = UserPreferences.symbolForCode(currencyCode)

            MoneyTrackerTheme {
                CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Show bottom nav only on main screens
                    val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }

                    // Handle widget intent - navigate to AddTransaction with type
                    var widgetHandled by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        if (!widgetHandled) {
                            val transactionType = intent?.getStringExtra("transaction_type")
                            if (transactionType != null) {
                                navController.navigate(Screen.AddTransaction.createRoute(transactionType))
                                widgetHandled = true
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        containerColor = DarkBackground,
                        bottomBar = {
                            if (showBottomNav) {
                                BottomNavBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
