package com.moneytracker.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.fragment.app.FragmentActivity
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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.navigation.BottomNavBar
import com.moneytracker.app.ui.navigation.NavGraph
import com.moneytracker.app.ui.navigation.Screen
import com.moneytracker.app.ui.screens.auth.PasscodeScreen
import com.moneytracker.app.ui.theme.DarkBackground
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import com.moneytracker.app.util.BiometricAuthManager
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var userPreferences: UserPreferences
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notification permission on first app launch (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyPrompted = runBlocking { userPreferences.notificationPrompted.first() }
            if (!alreadyPrompted && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
                    lifecycleScope.launch {
                        // Mark that we prompted so we don't keep asking
                        userPreferences.setNotificationPrompted(true)
                    }
                }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            val currencyCode by userPreferences.currencyCode.collectAsState(initial = "INR")
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)
            val currencySymbol = UserPreferences.symbolForCode(currencyCode)
            
            // Authentication state
            val biometricEnabled by userPreferences.biometricEnabled.collectAsState(initial = false)
            val passcodeEnabled by userPreferences.passcodeEnabled.collectAsState(initial = false)
            val storedPasscode by userPreferences.passcode.collectAsState(initial = null)
            val passcodeLockoutEndTime by userPreferences.passcodeLockoutEndTime.collectAsState(initial = 0L)
            val passcodeLockoutLevel by userPreferences.passcodeLockoutLevel.collectAsState(initial = 0)
            var isAuthenticated by remember { mutableStateOf(false) }
            var showPasscodeScreen by remember { mutableStateOf(false) }
            
            // Check if launched from widget
            val isWidgetLaunch = intent?.getStringExtra(MoneyTrackerWidget.EXTRA_TRANSACTION_TYPE) != null
            
            // Trigger authentication on app launch (skip if widget launch)
            LaunchedEffect(biometricEnabled, passcodeEnabled) {
                if (isWidgetLaunch) {
                    // Widget launches bypass authentication
                    isAuthenticated = true
                    return@LaunchedEffect
                }
                if (biometricEnabled || passcodeEnabled) {
                    if (biometricEnabled && biometricAuthManager.canAuthenticate(this@MainActivity)) {
                        // Show biometric prompt
                        biometricAuthManager.authenticate(
                            onSuccess = { isAuthenticated = true },
                            onError = { error ->
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                // Fallback to passcode if available
                                if (passcodeEnabled && storedPasscode != null) {
                                    showPasscodeScreen = true
                                } else {
                                    finish()
                                }
                            },
                            onFailed = {
                                Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else if (passcodeEnabled && storedPasscode != null) {
                        // Show passcode screen
                        showPasscodeScreen = true
                    } else {
                        // No valid auth method, allow access
                        isAuthenticated = true
                    }
                } else {
                    // Auth not enabled
                    isAuthenticated = true
                }
            }

            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MoneyTrackerTheme(darkTheme = darkTheme) {
                // Show lock screen until authenticated
                val passcodeValue = storedPasscode
                if (!isAuthenticated && showPasscodeScreen && passcodeValue != null) {
                    PasscodeScreen(
                        storedPasscode = passcodeValue,
                        initialLockoutEndTime = passcodeLockoutEndTime,
                        initialLockoutLevel = passcodeLockoutLevel,
                        onLockoutStateChange = { endTime, level ->
                            lifecycleScope.launch {
                                userPreferences.setPasscodeLockout(endTime, level)
                            }
                        },
                        onLockoutReset = {
                            lifecycleScope.launch {
                                userPreferences.clearPasscodeLockout()
                            }
                        },
                        onSuccess = {
                            isAuthenticated = true
                            showPasscodeScreen = false
                        },
                        onBiometricClick = if (biometricEnabled && biometricAuthManager.canAuthenticate(this@MainActivity)) {
                            {
                                biometricAuthManager.authenticate(
                                    onSuccess = {
                                        isAuthenticated = true
                                        showPasscodeScreen = false
                                    },
                                    onError = { error ->
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                    },
                                    onFailed = {
                                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else null,
                        showBiometricOption = biometricEnabled && biometricAuthManager.canAuthenticate(this@MainActivity)
                    )
                } else if (isAuthenticated) {
                    CompositionLocalProvider(
                        LocalCurrencySymbol provides currencySymbol
                    ) {
                        val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Show bottom nav only on main screens
                    val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }

                    // Handle widget/notification intent - navigate to AddTransaction with type
                    var widgetHandled by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        if (!widgetHandled) {
                            val transactionType = intent?.getStringExtra("transaction_type")
                            val suggestionAmount = intent?.getStringExtra("suggestion_amount")
                            val suggestionNote = intent?.getStringExtra("suggestion_note")
                            val suggestionPayee = intent?.getStringExtra("suggestion_payee")
                            if (transactionType != null) {
                                navController.navigate(
                                    Screen.AddTransaction.createRoute(
                                        type = transactionType,
                                        amount = suggestionAmount,
                                        note = suggestionNote,
                                        payee = suggestionPayee
                                    )
                                )
                                // Clear the extras so they don't re-trigger on recomposition
                                intent?.removeExtra("transaction_type")
                                intent?.removeExtra("suggestion_amount")
                                intent?.removeExtra("suggestion_note")
                                intent?.removeExtra("suggestion_payee")
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
