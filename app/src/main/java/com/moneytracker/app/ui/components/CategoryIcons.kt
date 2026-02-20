package com.moneytracker.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps icon key strings to Material Icons.
 */
object CategoryIcons {
    private val iconMap = mapOf(
        // Expense
        "restaurant" to Icons.Filled.Restaurant,
        "directions_car" to Icons.Filled.DirectionsCar,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "movie" to Icons.Filled.Movie,
        "medical_services" to Icons.Filled.MedicalServices,
        "school" to Icons.Filled.School,
        "receipt" to Icons.Filled.Receipt,
        "home" to Icons.Filled.Home,
        "two_wheeler" to Icons.Filled.TwoWheeler,
        "pets" to Icons.Filled.Pets,
        "people" to Icons.Filled.People,
        "more_horiz" to Icons.Filled.MoreHoriz,
        // Income
        "work" to Icons.Filled.Work,
        "laptop" to Icons.Filled.Laptop,
        "trending_up" to Icons.Filled.TrendingUp,
        "card_giftcard" to Icons.Filled.CardGiftcard,
        // Account
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "bank" to Icons.Filled.AccountBalance,
        "investment" to Icons.Filled.ShowChart,
        "credit_card" to Icons.Filled.CreditCard,
        "savings" to Icons.Filled.Savings,
        "payments" to Icons.Filled.Payments,
        "currency_rupee" to Icons.Filled.CurrencyRupee,
        "attach_money" to Icons.Filled.AttachMoney,
        "store" to Icons.Filled.Store,
        "phone_android" to Icons.Filled.PhoneAndroid,
        // Transfer
        "swap_horiz" to Icons.Filled.SwapHoriz,
        // Navigation
        "dashboard" to Icons.Filled.Dashboard,
        "list" to Icons.Filled.List,
        "account_balance" to Icons.Filled.AccountBalance,
        "pie_chart" to Icons.Filled.PieChart,
        "settings" to Icons.Filled.Settings,
        "add" to Icons.Filled.Add,
        "arrow_back" to Icons.Filled.ArrowBack,
        "delete" to Icons.Filled.Delete,
        "edit" to Icons.Filled.Edit,
        "check" to Icons.Filled.Check,
        "close" to Icons.Filled.Close,
        "calendar" to Icons.Filled.CalendarMonth,
        "arrow_left" to Icons.Filled.ChevronLeft,
        "arrow_right" to Icons.Filled.ChevronRight,
        "filter" to Icons.Filled.FilterList,
        "search" to Icons.Filled.Search,
    )

    @Composable
    fun getIcon(key: String): ImageVector {
        return iconMap[key] ?: Icons.Filled.Category
    }

    val accountIcons = listOf(
        "wallet", "bank", "investment", "credit_card", "savings",
        "payments", "currency_rupee", "attach_money", "store", "phone_android",
        "people"
    )
}
