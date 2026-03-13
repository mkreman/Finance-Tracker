package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.BudgetDao
import com.moneytracker.app.data.local.database.entities.BudgetEntity
import com.moneytracker.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun getBudgetsWithSpending(
        month: Int,
        year: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Budget>> =
        budgetDao.getBudgetsWithSpending(month, year, startDate, endDate).map { list ->
            list.map { bws ->
                Budget(
                    id = bws.budgetId,
                    categoryId = bws.categoryId,
                    categoryName = bws.categoryName,
                    categoryColor = bws.categoryColor,
                    categoryIcon = bws.categoryIcon,
                    limitAmount = bws.limitAmount,
                    sortOrder = bws.sortOrder,
                    spentAmount = bws.spentAmount
                )
            }
        }

    suspend fun saveBudget(categoryId: String, limitAmount: Double, month: Int, year: Int) {
        val existing = budgetDao.getBudgetForCategory(categoryId, month, year)
        if (existing != null) {
            budgetDao.updateBudget(
                existing.copy(
                    limitAmount = limitAmount,
                    modifiedAt = System.currentTimeMillis()
                )
            )
        } else {
            val nextSortOrder = budgetDao.getMaxSortOrderForMonth(month, year) + 1
            budgetDao.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    limitAmount = limitAmount,
                    sortOrder = nextSortOrder,
                    month = month,
                    year = year
                )
            )
        }
    }

    suspend fun updateBudgetOrder(orderedBudgetIds: List<String>) {
        val now = System.currentTimeMillis()
        orderedBudgetIds.forEachIndexed { index, budgetId ->
            budgetDao.updateBudgetSortOrder(budgetId, index, now)
        }
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.deleteBudget(id)
    }

    suspend fun clearAllBudgets() {
        budgetDao.clearAllBudgets()
    }

    suspend fun copyMissingBudgetsFromPreviousMonth(month: Int, year: Int): Int {
        val previous = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val previousMonth = previous.get(Calendar.MONTH) + 1
        val previousYear = previous.get(Calendar.YEAR)

        val allBudgets = budgetDao.getAllBudgets()
        val sourceBudgets = allBudgets
            .filter { it.month == previousMonth && it.year == previousYear }
            .sortedBy { it.sortOrder }
        if (sourceBudgets.isEmpty()) return 0

        val targetCategoryIds = allBudgets
            .asSequence()
            .filter { it.month == month && it.year == year }
            .map { it.categoryId }
            .toMutableSet()

        var nextSortOrder = budgetDao.getMaxSortOrderForMonth(month, year) + 1
        var copiedCount = 0
        val now = System.currentTimeMillis()

        sourceBudgets.forEach { source ->
            if (targetCategoryIds.add(source.categoryId)) {
                budgetDao.insertBudget(
                    BudgetEntity(
                        id = UUID.randomUUID().toString(),
                        categoryId = source.categoryId,
                        limitAmount = source.limitAmount,
                        sortOrder = nextSortOrder++,
                        month = month,
                        year = year,
                        createdAt = now,
                        modifiedAt = now
                    )
                )
                copiedCount++
            }
        }

        return copiedCount
    }

    suspend fun getAllBudgets(): List<BudgetEntity> {
        return budgetDao.getAllBudgets()
    }
}
