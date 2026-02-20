package com.moneytracker.app.data.local.database.dao

import androidx.room.*
import com.moneytracker.app.data.local.database.entities.BudgetEntity
import com.moneytracker.app.data.local.database.entities.BudgetWithSpending
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("""
        SELECT 
            b.id as budgetId,
            b.categoryId as categoryId,
            c.name as categoryName,
            c.colorHex as categoryColor,
            c.iconKey as categoryIcon,
            b.limitAmount as limitAmount,
            COALESCE(
                (SELECT SUM(s.amount) 
                 FROM transaction_splits s
                 JOIN transactions t ON s.transactionId = t.id
                 WHERE s.categoryId = b.categoryId
                 AND t.type = 'EXPENSE'
                 AND t.isDeleted = 0
                 AND t.date BETWEEN :startDate AND :endDate
                ), 0.0
            ) as spentAmount
        FROM budgets b
        JOIN categories c ON b.categoryId = c.id
        WHERE b.month = :month AND b.year = :year
        ORDER BY c.name ASC
    """)
    fun getBudgetsWithSpending(
        month: Int,
        year: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<BudgetWithSpending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year")
    suspend fun getBudgetForCategory(categoryId: String, month: Int, year: Int): BudgetEntity?

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()
}
