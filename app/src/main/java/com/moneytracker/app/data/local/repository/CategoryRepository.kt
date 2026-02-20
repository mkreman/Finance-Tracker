package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.entities.CategoryEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        iconKey = iconKey,
        type = type,
        budgetLimit = budgetLimit,
        colorHex = colorHex
    )
}
