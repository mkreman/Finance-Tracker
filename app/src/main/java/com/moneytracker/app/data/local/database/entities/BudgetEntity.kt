package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitAmount: Double,
    val sortOrder: Int = 0,
    val month: Int,     // 1-12
    val year: Int,      // e.g., 2026
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
