package com.moneytracker.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

        /**
         * Supported currencies with their symbols.
         */
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

        fun symbolForCode(code: String): String =
            currencies.find { it.code == code }?.symbol ?: "₹"

        fun displayForCode(code: String): String {
            val info = currencies.find { it.code == code }
            return if (info != null) "${info.code} (${info.symbol})" else code
        }
    }

    val defaultAccountId: Flow<String?> = dataStore.data.map {
        it[DEFAULT_ACCOUNT_ID]
    }

    val currencyCode: Flow<String> = dataStore.data.map {
        it[CURRENCY_CODE] ?: "INR"
    }

    val firstDayOfWeek: Flow<Int> = dataStore.data.map {
        it[FIRST_DAY_OF_WEEK] ?: Calendar.MONDAY
    }

    suspend fun setDefaultAccountId(accountId: String?) {
        dataStore.edit { prefs ->
            if (accountId != null) {
                prefs[DEFAULT_ACCOUNT_ID] = accountId
            } else {
                prefs.remove(DEFAULT_ACCOUNT_ID)
            }
        }
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { prefs ->
            prefs[CURRENCY_CODE] = code
        }
    }

    suspend fun setFirstDayOfWeek(day: Int) {
        dataStore.edit { prefs ->
            prefs[FIRST_DAY_OF_WEEK] = day
        }
    }
}
