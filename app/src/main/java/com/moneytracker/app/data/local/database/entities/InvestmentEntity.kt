package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val type: String,
    val exchange: String,
    val totalUnits: Double,
    val totalInvestedAmount: Double,
    val currentPrice: Double? = null,
    val priceUpdatedAt: Long? = null,
    val dayChangePercent: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val currentValuation: Double
        get() = if (totalUnits > 0) {
            totalUnits * (currentPrice ?: (if (totalUnits > 0) totalInvestedAmount / totalUnits else 0.0))
        } else 0.0

    val totalPnl: Double
        get() = currentValuation - totalInvestedAmount

    val pnlPercentage: Double
        get() = if (totalInvestedAmount > 0) (totalPnl / totalInvestedAmount) * 100.0 else 0.0
}
