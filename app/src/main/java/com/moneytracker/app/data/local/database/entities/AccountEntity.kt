package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val customTypeName: String? = null,
    val initialBalance: Double,
    val currentBalance: Double,
    val currency: String = "INR",
    val colorHex: String,
    val iconKey: String = "wallet",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
