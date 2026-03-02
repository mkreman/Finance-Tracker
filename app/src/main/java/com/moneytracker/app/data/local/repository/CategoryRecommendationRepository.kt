package com.moneytracker.app.data.local.repository

import com.moneytracker.app.data.local.database.dao.CategoryRecommendationDao
import com.moneytracker.app.data.local.database.entities.CategoryRecommendationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRecommendationRepository @Inject constructor(
    private val dao: CategoryRecommendationDao
) {
    suspend fun getRecommendation(payee: String): String? {
        return dao.getRecommendation(payee.lowercase())?.categoryId
    }

    suspend fun upsertRecommendation(payee: String, categoryId: String) {
        val pattern = payee.lowercase()
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