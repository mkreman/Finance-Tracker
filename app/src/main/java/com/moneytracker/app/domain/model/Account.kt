package com.moneytracker.app.domain.model

import com.moneytracker.app.data.local.database.entities.AccountType

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val initialBalance: Double,
    val currentBalance: Double,
    val currency: String = "INR",
    val colorHex: String,
    val iconKey: String = "wallet",
    val isActive: Boolean = true
)
