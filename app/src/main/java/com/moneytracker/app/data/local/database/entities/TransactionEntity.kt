package com.moneytracker.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("date"),
        Index("type"),
        Index("syncStatus")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val payee: String,
    val note: String? = null,
    val date: Long,
    val totalAmount: Double,
    val type: TransactionType,
    val toAccountId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isRecurring: Boolean = false,
    val recurringInterval: Int? = null,
    val recurringUnit: RecurringUnit? = null,
    val recurringEndDate: Long? = null,
    val parentRecurringId: String? = null,
    val notifyForRecurringEntries: Boolean = true,
    val receiptUri: String? = null
)

enum class RecurringUnit {
    DAY, WEEK, MONTH, YEAR
}
