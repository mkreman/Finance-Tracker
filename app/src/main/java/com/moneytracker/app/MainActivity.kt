package com.moneytracker.app

import kotlinx.coroutines.Dispatchers
import androidx.glance.appwidget.updateAll
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.moneytracker.app.data.backup.BackupResult
import com.moneytracker.app.data.backup.GoogleDriveBackupService
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.navigation.BottomNavBar
import com.moneytracker.app.ui.navigation.LocalBottomTabReselect
import com.moneytracker.app.ui.navigation.NavGraph
import com.moneytracker.app.ui.navigation.Screen
import com.moneytracker.app.ui.screens.auth.PasscodeScreen
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import com.moneytracker.app.util.BiometricAuthManager
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var googleDriveBackupService: GoogleDriveBackupService
    private lateinit var biometricAuthManager: BiometricAuthManager
    
    private val intentState = MutableStateFlow<Intent?>(null)

    companion object {
        private const val QUICK_BYPASS_WINDOW_MS = 5000L
        private var lastAppBackgroundAtMs: Long = 0L
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        lifecycleScope.launch {
            userPreferences.setNotificationPrompted(true)
        }
        
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (!smsGranted) {
            Toast.makeText(this, "SMS permission is required for auto-detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        intentState.value = intent

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        checkAndRequestPermissions()
        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            val currencyCode by userPreferences.currencyCode.collectAsState(initial = "INR")
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)
            val currencySymbol = UserPreferences.symbolForCode(currencyCode)
            
            val biometricEnabled by userPreferences.biometricEnabled.collectAsState(initial = null as Boolean?)
            val passcodeEnabled by userPreferences.passcodeEnabled.collectAsState(initial = null as Boolean?)
            val storedPasscode by userPreferences.passcode.collectAsState(initial = null as String?)
            val passcodeLockoutEndTime by userPreferences.passcodeLockoutEndTime.collectAsState(initial = 0L)
            val passcodeLockoutLevel by userPreferences.passcodeLockoutLevel.collectAsState(initial = 0)

            val currentIntent by intentState.collectAsState()
            var showCloudRestorePrompt by rememberSaveable { mutableStateOf(false) }
            var cloudRestorePromptChecked by rememberSaveable { mutableStateOf(false) }
            var cloudRestoreInProgress by rememberSaveable { mutableStateOf(false) }

            val gso = remember {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                    .build()
            }

            val runCloudRestore = {
                lifecycleScope.launch {
                    cloudRestoreInProgress = true
                    when (val result = googleDriveBackupService.restoreLatestBackupFromCloud()) {
                        BackupResult.Success -> {
                            showCloudRestorePrompt = false
                            Toast.makeText(this@MainActivity, "Cloud restore completed", Toast.LENGTH_SHORT).show()
                        }
                        BackupResult.NotSignedIn -> {
                            Toast.makeText(this@MainActivity, "Google sign-in required for cloud restore", Toast.LENGTH_SHORT).show()
                        }
                        BackupResult.NoBackupFound -> {
                            showCloudRestorePrompt = false
                            Toast.makeText(this@MainActivity, "No cloud backup found", Toast.LENGTH_SHORT).show()
                        }
                        is BackupResult.Error -> {
                            Toast.makeText(this@MainActivity, "Cloud restore failed: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    cloudRestoreInProgress = false
                }
            }

            val cloudSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                runCatching {
                    GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        .getResult(com.google.android.gms.common.api.ApiException::class.java)
                }.onSuccess {
                    runCloudRestore()
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }

            var isAuthenticated by rememberSaveable { mutableStateOf(false) }
            var isAuthenticating by remember { mutableStateOf(false) }
            var showPasscodeScreen by remember { mutableStateOf(false) }
            
            // Timer to track when the app was pushed to the background
            var backgroundTime by remember { mutableStateOf(0L) }
            
            val triggerAuth = trigger@{
                val isWidgetTransactionLaunch = currentIntent?.getStringExtra("transaction_type") != null
                val withinQuickBypassWindow = lastAppBackgroundAtMs > 0L &&
                    System.currentTimeMillis() - lastAppBackgroundAtMs <= QUICK_BYPASS_WINDOW_MS

                if (isWidgetTransactionLaunch || withinQuickBypassWindow) {
                    isAuthenticated = true
                    return@trigger
                }

                if (!isAuthenticated && !isAuthenticating && biometricEnabled != null && passcodeEnabled != null) {
                    if (biometricEnabled == true || passcodeEnabled == true) {
                        isAuthenticating = true
                        if (biometricEnabled == true && biometricAuthManager.canAuthenticate(this@MainActivity)) {
                            biometricAuthManager.authenticate(
                                onSuccess = { 
                                    isAuthenticated = true 
                                    isAuthenticating = false
                                },
                                onError = { error ->
                                    isAuthenticating = false
                                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                    if (passcodeEnabled == true && storedPasscode != null) {
                                        showPasscodeScreen = true
                                    } else {
                                        finish() 
                                    }
                                },
                                onFailed = {
                                    isAuthenticating = false
                                    Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else if (passcodeEnabled == true && storedPasscode != null) {
                            showPasscodeScreen = true
                            isAuthenticating = false
                        } else {
                            isAuthenticated = true
                            isAuthenticating = false
                        }
                    } else {
                        isAuthenticated = true 
                    }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (!this@MainActivity.isChangingConfigurations && !isAuthenticating) {
                            isAuthenticated = false
                            backgroundTime = System.currentTimeMillis() // Record exact background time
                            lastAppBackgroundAtMs = backgroundTime
                        }
                    } else if (event == Lifecycle.Event.ON_RESUME) {
                        backgroundTime = 0L // Reset timer
                        triggerAuth()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            
            LaunchedEffect(biometricEnabled, passcodeEnabled, currentIntent) {
                triggerAuth()
            }

            LaunchedEffect(isAuthenticated, cloudRestorePromptChecked) {
                if (isAuthenticated && !cloudRestorePromptChecked) {
                    cloudRestorePromptChecked = true
                    val hasLocalData = googleDriveBackupService.hasAnyLocalData()
                    showCloudRestorePrompt = !hasLocalData
                }
            }

            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MoneyTrackerTheme(darkTheme = darkTheme) {
                val passcodeValue = storedPasscode
                val bottomTabReselectFlow = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
                
                CompositionLocalProvider(
                    LocalCurrencySymbol provides currencySymbol,
                    LocalBottomTabReselect provides bottomTabReselectFlow
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }

                    LaunchedEffect(currentIntent) {
                        val transactionType = currentIntent?.getStringExtra("transaction_type")
                        val suggestionAmount = currentIntent?.getStringExtra("suggestion_amount")
                        val suggestionNote = currentIntent?.getStringExtra("suggestion_note")
                        val suggestionPayee = currentIntent?.getStringExtra("suggestion_payee")
                        val suggestionId = currentIntent?.getStringExtra("extra_suggestion_id")
                        val suggestedCatId = currentIntent?.getStringExtra("suggested_cat_id")
                        val openBudgetCategoryId = currentIntent?.getStringExtra("open_budget_category_id")
                        val openBudgetCategoryName = currentIntent?.getStringExtra("open_budget_category_name")

                        if (suggestionId != null) {
                            com.moneytracker.app.notifications.BankAlertSuggestionNotifier.cancel(this@MainActivity, suggestionId)
                        }
                        
                        if (openBudgetCategoryId != null && openBudgetCategoryName != null) {
                            navController.navigate(
                                Screen.BudgetTransactions.createRoute(
                                    categoryId = openBudgetCategoryId,
                                    categoryName = openBudgetCategoryName
                                )
                            )
                            currentIntent?.removeExtra("open_budget_category_id")
                            currentIntent?.removeExtra("open_budget_category_name")
                        } else if (transactionType != null) {
                            navController.navigate(
                                Screen.AddTransaction.createRoute(
                                    type = transactionType,
                                    amount = suggestionAmount,
                                    note = suggestionNote,
                                    payee = suggestionPayee,
                                    fromWidget = true,
                                    suggestedCategoryId = suggestedCatId
                                )
                            )
                            currentIntent?.removeExtra("transaction_type")
                            currentIntent?.removeExtra("suggestion_amount")
                            currentIntent?.removeExtra("suggestion_note")
                            currentIntent?.removeExtra("suggestion_payee")
                            currentIntent?.removeExtra("extra_suggestion_id")
                            currentIntent?.removeExtra("suggested_cat_id")
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
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

                        if (!isAuthenticated) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable(enabled = true, onClick = {}) 
                            ) {
                                if (showPasscodeScreen && passcodeValue != null) {
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
                                        onBiometricClick = if (biometricEnabled == true && biometricAuthManager.canAuthenticate(this@MainActivity)) {
                                            {
                                                isAuthenticating = true
                                                biometricAuthManager.authenticate(
                                                    onSuccess = {
                                                        isAuthenticated = true
                                                        showPasscodeScreen = false
                                                        isAuthenticating = false
                                                    },
                                                    onError = { error ->
                                                        isAuthenticating = false
                                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                                    },
                                                    onFailed = {
                                                        isAuthenticating = false
                                                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        } else null,
                                        showBiometricOption = biometricEnabled == true && biometricAuthManager.canAuthenticate(this@MainActivity)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "App Locked",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (!isAuthenticating && biometricEnabled == true) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(onClick = { triggerAuth() }) {
                                                Text("Unlock with Biometrics")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showCloudRestorePrompt && isAuthenticated) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!cloudRestoreInProgress) {
                                        showCloudRestorePrompt = false
                                    }
                                },
                                title = { Text("Restore From Cloud") },
                                text = {
                                    Text("No local data found. Do you want to sign in and restore your backup from Google Drive?")
                                },
                                confirmButton = {
                                    Button(
                                        enabled = !cloudRestoreInProgress,
                                        onClick = {
                                            val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                            val hasScope = account?.grantedScopes?.contains(Scope(DriveScopes.DRIVE_APPDATA)) == true

                                            if (account != null && hasScope) {
                                                runCloudRestore()
                                            } else {
                                                val signInClient = GoogleSignIn.getClient(this@MainActivity, gso)
                                                cloudSignInLauncher.launch(signInClient.signInIntent)
                                            }
                                        }
                                    ) {
                                        Text(if (cloudRestoreInProgress) "Restoring..." else "Restore")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        enabled = !cloudRestoreInProgress,
                                        onClick = { showCloudRestorePrompt = false }
                                    ) {
                                        Text("Skip")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyPrompted = runBlocking { userPreferences.notificationPrompted.first() }
            if (!alreadyPrompted && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
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