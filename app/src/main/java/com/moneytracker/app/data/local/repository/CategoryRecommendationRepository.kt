package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.CategoryRecommendationDao
import com.moneytracker.app.data.local.database.entities.CategoryRecommendationEntity
import com.moneytracker.app.data.local.database.entities.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRecommendationRepository @Inject constructor(
    private val dao: CategoryRecommendationDao
) {
    suspend fun getRecommendation(payee: String, type: TransactionType): String? {
        // Look for the specific payee + transaction type combination
        val pattern = "${payee.trim().lowercase()}_${type.name}"
        val specific = dao.getRecommendation(pattern)
        if (specific != null) return specific.categoryId
        
        // Fallback for older database records from before this update
        return dao.getRecommendation(payee.trim().lowercase())?.categoryId
    }

    suspend fun upsertRecommendation(payee: String, type: TransactionType, categoryId: String) {
        // Prevent saving generic fallback words into the recommendation engine
        if (payee.isBlank() || payee.lowercase() == "income" || payee.lowercase() == "expense") return
        
        val pattern = "${payee.trim().lowercase()}_${type.name}"
        val existing = dao.getRecommendation(pattern)
        
        val count = (existing?.usageCount ?: 0) + 1
        
        val entity = CategoryRecommendationEntity(
            payeePattern = pattern,
            categoryId = categoryId,
            usageCount = count,
            lastUsedTimestamp = System.currentTimeMillis()
        )
        
        dao.insertOrUpdate(entity)
        
        // Probabilistic eviction to avoid heavy checks on every single insert (runs ~5% of the time)
        if (Math.random() < 0.05) {
            dao.deleteLeastUsedRecords()
        }
    }
}