package com.moneytracker.app.ui.screens.settings

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.parseHexColor
import java.util.Calendar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showVersionHistory by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri ->
        uri?.let { viewModel.exportData(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(context, it) }
    }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.importMessage) {
        state.importMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // General Section
        SettingsSectionHeader("General")

        // Theme picker
        var showThemePicker by remember { mutableStateOf(false) }
        val themeSubtitle = when (state.themeMode) {
            1 -> "Light"
            2 -> "Dark"
            else -> "System"
        }
        SettingsItem(
            icon = Icons.Filled.Palette,
            title = "Theme",
            subtitle = themeSubtitle,
            iconTint = MaterialTheme.colorScheme.primaryContainer,
            onClick = { showThemePicker = true }
        )

        if (showThemePicker) {
            AlertDialog(
                onDismissRequest = { showThemePicker = false },
                title = { Text("Theme", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        listOf(0 to "Follow system", 1 to "Light", 2 to "Dark").forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (state.themeMode == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setThemeMode(value)
                                        showThemePicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Currency picker
        var showCurrencyPicker by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Filled.Language,
            title = "Currency",
            subtitle = UserPreferences.displayForCode(state.currencyCode),
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = { showCurrencyPicker = true }
        )

        if (showCurrencyPicker) {
            AlertDialog(
                onDismissRequest = { showCurrencyPicker = false },
                title = { Text("Currency", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        UserPreferences.currencies.forEach { currency ->
                            val isSelected = currency.code == state.currencyCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setCurrency(currency.code)
                                        showCurrencyPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currency.symbol, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(36.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(currency.name, color = MaterialTheme.colorScheme.onSurface)
                                    Text(currency.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // First Day of Week picker
        var showDayPicker by remember { mutableStateOf(false) }
        val dayLabel = when (state.firstDayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Monday"
        }
        SettingsItem(
            icon = Icons.Filled.CalendarMonth,
            title = "First Day of Week",
            subtitle = dayLabel,
            iconTint = MaterialTheme.colorScheme.tertiary,
            onClick = { showDayPicker = true }
        )

        if (showDayPicker) {
            AlertDialog(
                onDismissRequest = { showDayPicker = false },
                title = { Text("First Day of Week", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                            Calendar.MONDAY to "Monday",
                            Calendar.SUNDAY to "Sunday",
                            Calendar.SATURDAY to "Saturday"
                        ).forEach { (value, label) ->
                            val isSelected = value == state.firstDayOfWeek
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setFirstDayOfWeek(value)
                                        showDayPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Default Account picker
        val defaultAccount = state.accounts.find { it.id == state.defaultAccountId }
        var showAccountPicker by remember { mutableStateOf(false) }

        SettingsItem(
            icon = Icons.Filled.AccountBalanceWallet,
            title = "Default Account",
            subtitle = defaultAccount?.name ?: "First available",
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = { showAccountPicker = true }
        )

        SettingsToggleItem(
            icon = Icons.Filled.NotificationsActive,
            title = "Default recurring notification",
            subtitle = "Applied when creating new recurring entries",
            iconTint = MaterialTheme.colorScheme.secondary,
            isChecked = state.defaultNotifyForRecurringEntries,
            onCheckedChange = viewModel::setDefaultNotifyForRecurringEntries
        )

        if (showAccountPicker) {
            AlertDialog(
                onDismissRequest = { showAccountPicker = false },
                title = { Text("Default Account", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // "Auto" option — first available
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.defaultAccountId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewModel.setDefaultAccount(null)
                                    showAccountPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("First available", color = MaterialTheme.colorScheme.onSurface)
                        }
                        state.accounts.forEach { account ->
                            val isSelected = account.id == state.defaultAccountId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setDefaultAccount(account.id)
                                        showAccountPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.getIcon(account.iconKey),
                                    contentDescription = null,
                                    tint = parseHexColor(account.colorHex),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(account.name, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        SettingsSectionHeader("Security")

        // Biometric toggle wired to preferences (mutually exclusive with passcode)
        SettingsToggleItem(
            icon = Icons.Filled.Fingerprint,
            title = "Biometric Lock",
            subtitle = "Require device biometric to open",
            iconTint = MaterialTheme.colorScheme.primary,
            isChecked = state.biometricEnabled,
            onCheckedChange = { enable ->
                val biometricManager = BiometricManager.from(context)
                val can = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                if (enable) {
                    if (can == BiometricManager.BIOMETRIC_SUCCESS) {
                        // Disable passcode when enabling biometric
                        if (state.passcodeEnabled) {
                            viewModel.setPasscodeEnabled(false)
                        }
                        viewModel.setBiometricEnabled(true)
                        Toast.makeText(context, "Biometric enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Biometric not available on this device", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val activity = context as? FragmentActivity
                    if (activity == null) {
                        Toast.makeText(context, "Unable to verify biometric on this screen", Toast.LENGTH_SHORT).show()
                        return@SettingsToggleItem
                    }

                    val canDisableAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    if (canDisableAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                        Toast.makeText(context, "Biometric authentication is required to disable", Toast.LENGTH_SHORT).show()
                        return@SettingsToggleItem
                    }

                    val biometricPrompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(context),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                viewModel.setBiometricEnabled(false)
                                Toast.makeText(context, "Biometric disabled", Toast.LENGTH_SHORT).show()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    Toast.makeText(context, "Authentication failed: $errString", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Disable Biometric Lock")
                        .setSubtitle("Verify to disable biometric lock")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .setNegativeButtonText("Cancel")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                }
            }
        )

        // Passcode protection (mutually exclusive with biometric)
        var showPasscodeDialog by remember { mutableStateOf(false) }
        var showDisablePasscodeDialog by remember { mutableStateOf(false) }
        SettingsToggleItem(
            icon = Icons.Filled.Lock,
            title = "Passcode Protection",
            subtitle = if (state.passcodeEnabled) "Enabled" else "Not set",
            iconTint = MaterialTheme.colorScheme.error,
            isChecked = state.passcodeEnabled,
            onCheckedChange = { enable ->
                if (enable) {
                    // Disable biometric when enabling passcode
                    if (state.biometricEnabled) {
                        viewModel.setBiometricEnabled(false)
                    }
                    showPasscodeDialog = true
                } else {
                    showDisablePasscodeDialog = true
                }
            }
        )

        if (showDisablePasscodeDialog) {
            var currentPasscodeInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showDisablePasscodeDialog = false },
                title = { Text("Disable Passcode", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    OutlinedTextField(
                        value = currentPasscodeInput,
                        onValueChange = { currentPasscodeInput = it },
                        label = { Text("Enter current passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (viewModel.verifyCurrentPasscode(currentPasscodeInput)) {
                            viewModel.setPasscodeEnabled(false)
                            Toast.makeText(context, "Passcode disabled", Toast.LENGTH_SHORT).show()
                            showDisablePasscodeDialog = false
                        } else {
                            Toast.makeText(context, "Incorrect passcode", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Disable") }
                },
                dismissButton = {
                    TextButton(onClick = { showDisablePasscodeDialog = false }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showPasscodeDialog) {
            var pass1 by remember { mutableStateOf("") }
            var pass2 by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPasscodeDialog = false },
                title = { Text("Set Passcode", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pass1,
                            onValueChange = { pass1 = it },
                            label = { Text("Enter passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pass2,
                            onValueChange = { pass2 = it },
                            label = { Text("Confirm passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (pass1.isNotEmpty() && pass1 == pass2) {
                            viewModel.setPasscode(pass1)
                            viewModel.setPasscodeEnabled(true)
                            Toast.makeText(context, "Passcode set", Toast.LENGTH_SHORT).show()
                            showPasscodeDialog = false
                        } else {
                            Toast.makeText(context, "Passcodes do not match", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Set") }
                },
                dismissButton = {
                    TextButton(onClick = { showPasscodeDialog = false }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Section
        SettingsSectionHeader("Data & Sync")

        var syncEnabled by remember { mutableStateOf(true) }
        SettingsToggleItem(
            icon = Icons.Filled.CloudSync,
            title = "Auto Cloud Sync",
            subtitle = "Sync data across devices",
            iconTint = MaterialTheme.colorScheme.secondary,
            isChecked = syncEnabled,
            onCheckedChange = { syncEnabled = it }
        )

        SettingsItem(
            icon = Icons.Filled.Backup,
            title = "Backup Now",
            subtitle = "Last backup: Never",
            iconTint = MaterialTheme.colorScheme.tertiary
        )

        SettingsItem(
            icon = Icons.Filled.FileDownload,
            title = "Export Data",
            subtitle = "Export as TSV",
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
                    .format(java.util.Date())
                exportLauncher.launch("MoneyTracker_$timestamp.tsv")
            }
        )

        SettingsItem(
            icon = Icons.Filled.FileUpload,
            title = "Import Data",
            subtitle = "Import from TSV",
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = {
                importLauncher.launch(arrayOf("text/tab-separated-values", "text/plain", "*/*"))
            }
        )

        var showResetDialog by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Filled.DeleteForever,
            title = "Clear All Data",
            subtitle = "This cannot be undone",
            iconTint = MaterialTheme.colorScheme.error,
            onClick = { showResetDialog = true }
        )

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Clear All Data?", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        "This will permanently delete all accounts, transactions, and budgets. This action cannot be undone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSectionHeader("About")

        SettingsItem(
            icon = Icons.Filled.Info,
            title = "Version",
            subtitle = "0.5.4",
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showVersionHistory = true }
        )

        // About entry
        SettingsItem(
            icon = Icons.Filled.Person,
            title = "About",
            subtitle = "App & developer information",
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showAbout = true }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
    
    if (showVersionHistory) {
        VersionHistoryDialog(
            onDismiss = { showVersionHistory = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("About", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                Text("Mayank Nagar", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Personal project", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Contact:", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                Text("mkreman12@gmail.com", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Launch email intent
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:mkreman12@gmail.com")
                }
                context.startActivity(intent)
            }) {
                Text("Contact", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun VersionHistoryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Version History",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                VersionItem(
                    version = "0.5.4",
                    features = listOf(
                        "Bring back accounts and categories after reset",
                        "Auto-focus on account name field when adding account",
                        "Keyboard dismisses when selecting account type, icon or color",
                        "Budget import/export functionality fixed",
                        "Widget redesigned with smaller buttons and arrow symbols (↓↑⇄)",
                        "Added app icon to widget that opens the app when clicked"
                    )
                )
                
                VersionItem(
                    version = "0.5.3",
                    features = listOf(
                        "Auto-focus on account name field when adding account",
                        "Added accounts and budgets to export/import file",
                        "Filter categories with existing budgets when adding new budget",
                        "Show transfers in daily summary on transaction section",
                        "Fixed reset all data functionality",
                        "Auto-create missing accounts/categories when importing TSV file"
                    )
                )
                
                VersionItem(
                    version = "0.5.2",
                    features = listOf(
                        "Default categories and accounts preserved after app installation",
                        "Show all selected categories on transaction entries",
                        "Fixed account balance update when deleting accounts with transactions",
                        "Budget spent amount displayed in red color",
                        "Currency and first day of week settings now functional",
                        "Complete version history in Settings"
                    )
                )
                
                VersionItem(
                    version = "0.5.1",
                    features = listOf(
                        "Fixed overlapping text in budget summary page",
                        "Show total remaining amount in budget summary",
                        "Added ability to edit and delete budgets",
                        "Three-dot menu for accounts with edit, delete, and deactivate options",
                        "Deactivate accounts to hidden list with ability to reactivate"
                    )
                )
                
                VersionItem(
                    version = "0.5.0",
                    features = listOf(
                        "Fixed transaction update functionality and account balance update",
                        "Transfer now correctly updates 'to account' balance",
                        "Negative account balance shown in red with minus sign"
                    )
                )
                
                VersionItem(
                    version = "0.4.0",
                    features = listOf(
                        "Transaction set as default page",
                        "Transfer section now lists entries without plot",
                        "Keyboard auto-shows when clicking + for amount",
                        "Option to add new category with custom icon",
                        "Transfer correctly updates both account balances",
                        "Added 'People' account type for lending/borrowing"
                    )
                )
                
                VersionItem(
                    version = "0.3.0",
                    features = listOf(
                        "Fixed app crash on transaction entry",
                        "Redesigned add transaction page",
                        "Keyboard dismisses after category selection",
                        "Multiple category selection support",
                        "Separate daily totals for expense and income"
                    )
                )
                
                VersionItem(
                    version = "0.2.0",
                    features = listOf(
                        "Fixed app crash after creating expense/income entry",
                        "Default account selection for expense/income",
                        "Account overview with income, expense, and balance",
                        "Clickable accounts showing transaction history",
                        "People category for lending/borrowing",
                        "Icon selection for accounts",
                        "Edit starting amount capability",
                        "Transfer section in dashboard",
                        "Income summary when pressing income",
                        "Clickable categories showing filtered transactions",
                        "Working date edit option",
                        "Multiple category selection with split",
                        "Transfer save functionality",
                        "Edit/delete old transactions",
                        "Overall expense/income on daily headers",
                        "Transaction timestamps",
                        "Clickable budget categories with expense details",
                        "Remaining budget display",
                        "TSV export/import functionality"
                    )
                )
                
                VersionItem(
                    version = "0.1.0",
                    features = listOf(
                        "Initial release",
                        "Basic expense and income tracking",
                        "Account management",
                        "Category organization",
                        "Budget setting",
                        "Transaction history",
                        "Dashboard with statistics"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun VersionItem(version: String, features: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Version $version",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        features.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "• ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = feature,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
    }
}
