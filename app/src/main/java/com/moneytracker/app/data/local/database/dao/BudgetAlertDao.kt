package com.moneytracker.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.moneytracker.app.data.local.database.entities.BudgetEntity

@Dao
interface BudgetAlertDao {
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: String, month: Int, year: Int): BudgetEntity?

    @Query("""
        SELECT SUM(s.amount) FROM transaction_splits s 
        INNER JOIN transactions t ON t.id = s.transactionId 
        WHERE s.categoryId = :categoryId 
        AND t.date >= :startDate AND t.date <= :endDate 
        AND t.type = 'EXPENSE' AND t.isDeleted = 0
    """)
    suspend fun getSpentAmountForCategory(categoryId: String, startDate: Long, endDate: Long): Double?
}