package com.moneytracker.app.domain.model

import com.moneytracker.app.data.local.database.entities.TransactionType

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val type: TransactionType,
    val budgetLimit: Double? = null,
    val colorHex: String
)
