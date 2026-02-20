package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val type: TransactionType,
    val budgetLimit: Double? = null,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
