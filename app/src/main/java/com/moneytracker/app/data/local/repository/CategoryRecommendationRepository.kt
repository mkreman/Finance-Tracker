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
    private fun patternFor(payee: String, type: TransactionType): String {
        return "${payee.trim().lowercase()}_${type.name}"
    }

    suspend fun getRecommendationEntity(
        payee: String,
        type: TransactionType
    ): CategoryRecommendationEntity? {
        val specific = dao.getRecommendation(patternFor(payee, type))
        if (specific != null) return specific

        // Fallback for older database records from before typed pattern keys.
        return dao.getRecommendation(payee.trim().lowercase())
    }

    suspend fun getRecommendation(payee: String, type: TransactionType): String? {
        return getRecommendationEntity(payee, type)?.categoryId
    }

    suspend fun getRecommendedAccountId(payee: String, type: TransactionType): String? {
        return getRecommendationEntity(payee, type)?.accountId
    }

    suspend fun upsertRecommendation(
        payee: String,
        type: TransactionType,
        categoryId: String,
        accountId: String? = null
    ) {
        // Prevent saving generic fallback words into the recommendation engine
        if (payee.isBlank() || payee.lowercase() == "income" || payee.lowercase() == "expense") return

        val pattern = patternFor(payee, type)
        val existing = dao.getRecommendation(pattern)

        val count = (existing?.usageCount ?: 0) + 1

        val entity = CategoryRecommendationEntity(
            payeePattern = pattern,
            categoryId = categoryId,
            accountId = accountId ?: existing?.accountId,
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