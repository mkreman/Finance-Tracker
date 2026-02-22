package com.moneytracker.app

import kotlinx.coroutines.Dispatchers
import androidx.glance.appwidget.updateAll
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.navigation.BottomNavBar
import com.moneytracker.app.ui.navigation.NavGraph
import com.moneytracker.app.ui.navigation.Screen
import com.moneytracker.app.ui.screens.auth.PasscodeScreen
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import com.moneytracker.app.util.BiometricAuthManager
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var userPreferences: UserPreferences
    private lateinit var biometricAuthManager: BiometricAuthManager

    // Launcher for multiple permissions (SMS and Notifications)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        lifecycleScope.launch {
            // Mark notifications as prompted regardless of choice to avoid spamming
            userPreferences.setNotificationPrompted(true)
        }
        
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (!smsGranted) {
            Toast.makeText(this, "SMS permission is required for auto-detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Request Permissions
        checkAndRequestPermissions()

        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            val currencyCode by userPreferences.currencyCode.collectAsState(initial = "INR")
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)
            val currencySymbol = UserPreferences.symbolForCode(currencyCode)
            
            val biometricEnabled by userPreferences.biometricEnabled.collectAsState(initial = false)
            val passcodeEnabled by userPreferences.passcodeEnabled.collectAsState(initial = false)
            val storedPasscode by userPreferences.passcode.collectAsState(initial = null)
            val passcodeLockoutEndTime by userPreferences.passcodeLockoutEndTime.collectAsState(initial = 0L)
            val passcodeLockoutLevel by userPreferences.passcodeLockoutLevel.collectAsState(initial = 0)
            var isAuthenticated by remember { mutableStateOf(false) }
            var showPasscodeScreen by remember { mutableStateOf(false) }
            
            val isWidgetLaunch = intent?.getStringExtra(MoneyTrackerWidget.EXTRA_TRANSACTION_TYPE) != null
            
            LaunchedEffect(biometricEnabled, passcodeEnabled) {
                if (isWidgetLaunch) {
                    isAuthenticated = true
                    return@LaunchedEffect
                }
                if (biometricEnabled || passcodeEnabled) {
                    if (biometricEnabled && biometricAuthManager.canAuthenticate(this@MainActivity)) {
                        biometricAuthManager.authenticate(
                            onSuccess = { isAuthenticated = true },
                            onError = { error ->
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
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
                        showPasscodeScreen = true
                    } else {
                        isAuthenticated = true
                    }
                } else {
                    isAuthenticated = true
                }
            }

            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MoneyTrackerTheme(darkTheme = darkTheme) {
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

                        val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }

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
                                    intent?.removeExtra("transaction_type")
                                    intent?.removeExtra("suggestion_amount")
                                    intent?.removeExtra("suggestion_note")
                                    intent?.removeExtra("suggestion_payee")
                                    widgetHandled = true
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = {
                                if (showBottomNav) {
                                    BottomNavBar(navController = navController)
                                }
                            }
                        ) { innerPadding ->
                            Box(
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

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Add SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }

        // Add Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyPrompted = runBlocking { userPreferences.notificationPrompted.first() }
            if (!alreadyPrompted && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MoneyTrackerWidget().updateAll(this@MainActivity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}