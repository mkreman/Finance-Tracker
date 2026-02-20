package com.moneytracker.app.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.components.CategoryIcons
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.*
import java.util.Calendar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // General Section
        SettingsSectionHeader("General")

        SettingsItem(
            icon = Icons.Filled.Palette,
            title = "Theme",
            subtitle = "Darker",
            iconTint = AccentPurple
        )

        // Currency picker
        var showCurrencyPicker by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Filled.Language,
            title = "Currency",
            subtitle = UserPreferences.displayForCode(state.currencyCode),
            iconTint = TransferBlue,
            onClick = { showCurrencyPicker = true }
        )

        if (showCurrencyPicker) {
            AlertDialog(
                onDismissRequest = { showCurrencyPicker = false },
                title = { Text("Currency", color = TextPrimary) },
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
                                    .background(if (isSelected) TransferBlue.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setCurrency(currency.code)
                                        showCurrencyPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currency.symbol, color = TextPrimary, modifier = Modifier.width(36.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(currency.name, color = TextPrimary)
                                    Text(currency.code, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = DarkSurface,
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
            iconTint = IncomeGreen,
            onClick = { showDayPicker = true }
        )

        if (showDayPicker) {
            AlertDialog(
                onDismissRequest = { showDayPicker = false },
                title = { Text("First Day of Week", color = TextPrimary) },
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
                                    .background(if (isSelected) IncomeGreen.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setFirstDayOfWeek(value)
                                        showDayPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = DarkSurface,
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
            iconTint = AccentOrange,
            onClick = { showAccountPicker = true }
        )

        if (showAccountPicker) {
            AlertDialog(
                onDismissRequest = { showAccountPicker = false },
                title = { Text("Default Account", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // "Auto" option — first available
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.defaultAccountId == null) AccentOrange.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewModel.setDefaultAccount(null)
                                    showAccountPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = AccentOrange, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("First available", color = TextPrimary)
                        }
                        state.accounts.forEach { account ->
                            val isSelected = account.id == state.defaultAccountId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentOrange.copy(alpha = 0.12f) else Color.Transparent)
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
                                Text(account.name, color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        SettingsSectionHeader("Security")

        var biometricEnabled by remember { mutableStateOf(false) }
        SettingsToggleItem(
            icon = Icons.Filled.Fingerprint,
            title = "Biometric Lock",
            subtitle = "Require fingerprint to open",
            iconTint = AccentOrange,
            isChecked = biometricEnabled,
            onCheckedChange = { biometricEnabled = it }
        )

        SettingsItem(
            icon = Icons.Filled.Lock,
            title = "Passcode Protection",
            subtitle = "Not set",
            iconTint = ExpenseRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Section
        SettingsSectionHeader("Data & Sync")

        var syncEnabled by remember { mutableStateOf(true) }
        SettingsToggleItem(
            icon = Icons.Filled.CloudSync,
            title = "Auto Cloud Sync",
            subtitle = "Sync data across devices",
            iconTint = TransferBlue,
            isChecked = syncEnabled,
            onCheckedChange = { syncEnabled = it }
        )

        SettingsItem(
            icon = Icons.Filled.Backup,
            title = "Backup Now",
            subtitle = "Last backup: Never",
            iconTint = IncomeGreen
        )

        SettingsItem(
            icon = Icons.Filled.FileDownload,
            title = "Export Data",
            subtitle = "Export as TSV",
            iconTint = AccentOrange,
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
            iconTint = TransferBlue,
            onClick = {
                importLauncher.launch(arrayOf("text/tab-separated-values", "text/plain", "*/*"))
            }
        )

        var showResetDialog by remember { mutableStateOf(false) }
        SettingsItem(
            icon = Icons.Filled.DeleteForever,
            title = "Clear All Data",
            subtitle = "This cannot be undone",
            iconTint = ExpenseRed,
            onClick = { showResetDialog = true }
        )

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Clear All Data?", color = TextPrimary) },
                text = {
                    Text(
                        "This will permanently delete all accounts, transactions, and budgets. This action cannot be undone.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSectionHeader("About")

        SettingsItem(
            icon = Icons.Filled.Info,
            title = "Version",
            subtitle = "0.5.3",
            iconTint = TextSecondary
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = AccentOrange,
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
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
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
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentOrange,
                checkedTrackColor = AccentOrange.copy(alpha = 0.3f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
