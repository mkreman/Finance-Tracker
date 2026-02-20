package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.entities.CategoryEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getCategoryById(id: String): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(
            CategoryEntity(
                id = category.id,
                name = category.name,
                iconKey = category.iconKey,
                type = category.type,
                budgetLimit = category.budgetLimit,
                colorHex = category.colorHex,
                modifiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.softDelete(id)
    }

    suspend fun seedDefaultCategories() {
        val defaultCategories = listOf(
            // Expense categories
            Category("cat-food", "Food", "restaurant", TransactionType.EXPENSE, 0.0, "#FF5722"),
            Category("cat-transport", "Transport", "directions_car", TransactionType.EXPENSE, 0.0, "#2196F3"),
            Category("cat-shopping", "Shopping", "shopping_bag", TransactionType.EXPENSE, 0.0, "#9C27B0"),
            Category("cat-entertainment", "Entertainment", "movie", TransactionType.EXPENSE, 0.0, "#E91E63"),
            Category("cat-health", "Health", "medical_services", TransactionType.EXPENSE, 0.0, "#4CAF50"),
            Category("cat-education", "Education", "school", TransactionType.EXPENSE, 0.0, "#3F51B5"),
            Category("cat-bills", "Bills", "receipt", TransactionType.EXPENSE, 0.0, "#FF9800"),
            Category("cat-rent", "Rent", "home", TransactionType.EXPENSE, 0.0, "#795548"),
            Category("cat-bike", "Bike", "two_wheeler", TransactionType.EXPENSE, 0.0, "#607D8B"),
            Category("cat-cats", "Cats", "pets", TransactionType.EXPENSE, 0.0, "#FFEB3B"),
            Category("cat-people", "People", "people", TransactionType.EXPENSE, 0.0, "#00BCD4"),
            Category("cat-other-expense", "Other", "more_horiz", TransactionType.EXPENSE, 0.0, "#9E9E9E"),
            // Income categories
            Category("cat-salary", "Salary", "work", TransactionType.INCOME, 0.0, "#4CAF50"),
            Category("cat-freelance", "Freelance", "laptop", TransactionType.INCOME, 0.0, "#2196F3"),
            Category("cat-investment-income", "Investment", "trending_up", TransactionType.INCOME, 0.0, "#FF9800"),
            Category("cat-gift", "Gift", "card_giftcard", TransactionType.INCOME, 0.0, "#E91E63"),
            Category("cat-other-income", "Other", "more_horiz", TransactionType.INCOME, 0.0, "#9E9E9E")
        )
        defaultCategories.forEach { saveCategory(it) }
    }

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        iconKey = iconKey,
        type = type,
        budgetLimit = budgetLimit,
        colorHex = colorHex
    )
}
