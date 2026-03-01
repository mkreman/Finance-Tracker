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
        "fastfood" to Icons.Filled.Fastfood,
        "local_cafe" to Icons.Filled.LocalCafe,
        "directions_car" to Icons.Filled.DirectionsCar,
        "directions_transit" to Icons.Filled.DirectionsTransit,
        "directions_bike" to Icons.Filled.DirectionsBike,
        "flight" to Icons.Filled.Flight,
        "local_gas_station" to Icons.Filled.LocalGasStation,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "local_grocery_store" to Icons.Filled.LocalGroceryStore,
        "movie" to Icons.Filled.Movie,
        "sports_esports" to Icons.Filled.SportsEsports,
        "palette" to Icons.Filled.Palette,
        "medical_services" to Icons.Filled.MedicalServices,
        "local_hospital" to Icons.Filled.LocalHospital,
        "local_pharmacy" to Icons.Filled.LocalPharmacy,
        "fitness_center" to Icons.Filled.FitnessCenter,
        "spa" to Icons.Filled.Spa,
        "school" to Icons.Filled.School,
        "child_care" to Icons.Filled.ChildCare,
        "receipt" to Icons.Filled.Receipt,
        "home" to Icons.Filled.Home,
        "build" to Icons.Filled.Build,
        "two_wheeler" to Icons.Filled.TwoWheeler,
        "pets" to Icons.Filled.Pets,
        "people" to Icons.Filled.People,
        "computer" to Icons.Filled.Computer,
        "music_note" to Icons.Filled.MusicNote,
        "weekend" to Icons.Filled.Weekend,
        "more_horiz" to Icons.Filled.MoreHoriz,
        // Income
        "work" to Icons.Filled.Work,
        "laptop" to Icons.Filled.Laptop,
        "trending_up" to Icons.Filled.TrendingUp,
        "trending_down" to Icons.Filled.TrendingDown,
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
        "autorenew" to Icons.Filled.Autorenew,
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
