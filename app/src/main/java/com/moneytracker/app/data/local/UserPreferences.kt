package com.moneytracker.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val DEFAULT_ACCOUNT_ID = stringPreferencesKey("default_account_id")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PASSCODE = stringPreferencesKey("passcode")
        val PASSCODE_ENABLED = booleanPreferencesKey("passcode_enabled")
        val PASSCODE_LOCKOUT_END_TIME = longPreferencesKey("passcode_lockout_end_time")
        val PASSCODE_LOCKOUT_LEVEL = intPreferencesKey("passcode_lockout_level")
        val THEME_MODE = intPreferencesKey("theme_mode") 
        val EXPANDED_CASH = booleanPreferencesKey("expanded_cash")
        val EXPANDED_WALLET = booleanPreferencesKey("expanded_wallet")
        val EXPANDED_BANK = booleanPreferencesKey("expanded_bank")
        val EXPANDED_INVESTMENT = booleanPreferencesKey("expanded_investment")
        val EXPANDED_PEOPLE = booleanPreferencesKey("expanded_people")
        val EXPANDED_CUSTOM = booleanPreferencesKey("expanded_custom")
        val NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompted")
        val DEFAULT_NOTIFY_FOR_RECURRING_ENTRIES = booleanPreferencesKey("default_notify_for_recurring_entries")
        val ACCOUNT_TYPE_ORDER = stringPreferencesKey("account_type_order")
        
        // NEW PREFERENCES
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")

        val defaultAccountOrder = listOf("CASH", "WALLET", "BANK", "INVESTMENT", "PEOPLE")

        data class CurrencyInfo(val code: String, val symbol: String, val name: String)

        val currencies = listOf(
            CurrencyInfo("INR", "₹", "Indian Rupee"),
            CurrencyInfo("USD", "$", "US Dollar"),
            CurrencyInfo("EUR", "€", "Euro"),
            CurrencyInfo("GBP", "£", "British Pound"),
            CurrencyInfo("JPY", "¥", "Japanese Yen"),
            CurrencyInfo("AED", "د.إ", "UAE Dirham"),
            CurrencyInfo("SAR", "﷼", "Saudi Riyal"),
            CurrencyInfo("CAD", "C$", "Canadian Dollar"),
            CurrencyInfo("AUD", "A$", "Australian Dollar"),
            CurrencyInfo("CNY", "¥", "Chinese Yuan"),
            CurrencyInfo("KRW", "₩", "South Korean Won"),
            CurrencyInfo("SGD", "S$", "Singapore Dollar"),
            CurrencyInfo("THB", "฿", "Thai Baht"),
            CurrencyInfo("MYR", "RM", "Malaysian Ringgit"),
            CurrencyInfo("BDT", "৳", "Bangladeshi Taka"),
            CurrencyInfo("PKR", "₨", "Pakistani Rupee"),
            CurrencyInfo("LKR", "Rs", "Sri Lankan Rupee"),
            CurrencyInfo("NPR", "Rs", "Nepalese Rupee"),
        )

        fun symbolForCode(code: String): String = currencies.find { it.code == code }?.symbol ?: "₹"
        fun displayForCode(code: String): String {
            val info = currencies.find { it.code == code }
            return if (info != null) "${info.code} (${info.symbol})" else code
        }
    }

    val defaultAccountId: Flow<String?> = dataStore.data.map { it[DEFAULT_ACCOUNT_ID] }
    val currencyCode: Flow<String> = dataStore.data.map { it[CURRENCY_CODE] ?: "INR" }
    val firstDayOfWeek: Flow<Int> = dataStore.data.map { it[FIRST_DAY_OF_WEEK] ?: Calendar.MONDAY }
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val passcode: Flow<String?> = dataStore.data.map { it[PASSCODE] }
    val passcodeEnabled: Flow<Boolean> = dataStore.data.map { it[PASSCODE_ENABLED] ?: false }
    val passcodeLockoutEndTime: Flow<Long> = dataStore.data.map { it[PASSCODE_LOCKOUT_END_TIME] ?: 0L }
    val passcodeLockoutLevel: Flow<Int> = dataStore.data.map { it[PASSCODE_LOCKOUT_LEVEL] ?: 0 }
    val themeMode: Flow<Int> = dataStore.data.map { it[THEME_MODE] ?: 0 }
    
    val expandedCash: Flow<Boolean> = dataStore.data.map { it[EXPANDED_CASH] ?: true }
    val expandedWallet: Flow<Boolean> = dataStore.data.map { it[EXPANDED_WALLET] ?: true }
    val expandedBank: Flow<Boolean> = dataStore.data.map { it[EXPANDED_BANK] ?: true }
    val expandedInvestment: Flow<Boolean> = dataStore.data.map { it[EXPANDED_INVESTMENT] ?: true }
    val expandedPeople: Flow<Boolean> = dataStore.data.map { it[EXPANDED_PEOPLE] ?: true }
    val expandedCustom: Flow<Boolean> = dataStore.data.map { it[EXPANDED_CUSTOM] ?: true }
    
    val notificationPrompted: Flow<Boolean> = dataStore.data.map { it[NOTIFICATION_PROMPTED] ?: false }
    val defaultNotifyForRecurringEntries: Flow<Boolean> = dataStore.data.map { it[DEFAULT_NOTIFY_FOR_RECURRING_ENTRIES] ?: true }
    val accountTypeOrder: Flow<List<String>> = dataStore.data.map { prefs ->
        val orderStr = prefs[ACCOUNT_TYPE_ORDER]
        if (orderStr.isNullOrBlank()) defaultAccountOrder else orderStr.split(",")
    }

    // NEW FLOWS
    val dailyReminderEnabled: Flow<Boolean> = dataStore.data.map { it[DAILY_REMINDER_ENABLED] ?: false }
    val budgetAlertsEnabled: Flow<Boolean> = dataStore.data.map { it[BUDGET_ALERTS_ENABLED] ?: true }

    fun expandedForCustom(name: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("expanded_custom_" + name)] ?: true
    }

    suspend fun setDefaultAccountId(accountId: String?) {
        dataStore.edit { prefs -> if (accountId != null) prefs[DEFAULT_ACCOUNT_ID] = accountId else prefs.remove(DEFAULT_ACCOUNT_ID) }
    }
    suspend fun setCurrencyCode(code: String) { dataStore.edit { it[CURRENCY_CODE] = code } }
    suspend fun setFirstDayOfWeek(day: Int) { dataStore.edit { it[FIRST_DAY_OF_WEEK] = day } }
    suspend fun setBiometricEnabled(enabled: Boolean) { dataStore.edit { it[BIOMETRIC_ENABLED] = enabled } }
    suspend fun setPasscode(passcodeValue: String?) {
        dataStore.edit { if (passcodeValue != null) it[PASSCODE] = passcodeValue else it.remove(PASSCODE) }
    }
    suspend fun setPasscodeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PASSCODE_ENABLED] = enabled
            if (!enabled) {
                prefs.remove(PASSCODE)
                prefs[PASSCODE_LOCKOUT_END_TIME] = 0L
                prefs[PASSCODE_LOCKOUT_LEVEL] = 0
            }
        }
    }
    suspend fun setPasscodeLockout(endTimeMillis: Long, level: Int) {
        dataStore.edit { prefs ->
            prefs[PASSCODE_LOCKOUT_END_TIME] = endTimeMillis
            prefs[PASSCODE_LOCKOUT_LEVEL] = level
        }
    }
    suspend fun clearPasscodeLockout() {
        dataStore.edit { prefs ->
            prefs[PASSCODE_LOCKOUT_END_TIME] = 0L
            prefs[PASSCODE_LOCKOUT_LEVEL] = 0
        }
    }
    suspend fun setThemeMode(mode: Int) { dataStore.edit { it[THEME_MODE] = mode } }
    suspend fun setExpandedCash(expanded: Boolean) { dataStore.edit { it[EXPANDED_CASH] = expanded } }
    suspend fun setExpandedWallet(expanded: Boolean) { dataStore.edit { it[EXPANDED_WALLET] = expanded } }
    suspend fun setExpandedBank(expanded: Boolean) { dataStore.edit { it[EXPANDED_BANK] = expanded } }
    suspend fun setExpandedInvestment(expanded: Boolean) { dataStore.edit { it[EXPANDED_INVESTMENT] = expanded } }
    suspend fun setExpandedPeople(expanded: Boolean) { dataStore.edit { it[EXPANDED_PEOPLE] = expanded } }
    suspend fun setExpandedCustom(expanded: Boolean) { dataStore.edit { it[EXPANDED_CUSTOM] = expanded } }
    suspend fun setExpandedForCustom(name: String, expanded: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("expanded_custom_" + name)] = expanded }
    }
    suspend fun setNotificationPrompted(prompted: Boolean) { dataStore.edit { it[NOTIFICATION_PROMPTED] = prompted } }
    suspend fun setDefaultNotifyForRecurringEntries(enabled: Boolean) { dataStore.edit { it[DEFAULT_NOTIFY_FOR_RECURRING_ENTRIES] = enabled } }
    suspend fun setAccountTypeOrder(order: List<String>) { dataStore.edit { it[ACCOUNT_TYPE_ORDER] = order.joinToString(",") } }
    
    // NEW SETTERS
    suspend fun setDailyReminderEnabled(enabled: Boolean) { dataStore.edit { it[DAILY_REMINDER_ENABLED] = enabled } }
    suspend fun setBudgetAlertsEnabled(enabled: Boolean) { dataStore.edit { it[BUDGET_ALERTS_ENABLED] = enabled } }
}