package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_recommendations")
data class CategoryRecommendationEntity(
    @PrimaryKey val payeePattern: String,
    val categoryId: String,
    val usageCount: Int = 1,
    val lastUsedTimestamp: Long
)