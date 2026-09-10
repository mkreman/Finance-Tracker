package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "investment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = InvestmentEntity::class,
            parentColumns = ["symbol"],
            childColumns = ["symbol"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("symbol"),
        Index("date")
    ]
)
data class InvestmentTransactionEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val transactionType: String,
    val units: Double,
    val amount: Double,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
