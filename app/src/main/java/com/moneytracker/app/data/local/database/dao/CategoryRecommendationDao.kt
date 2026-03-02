package com.moneytracker.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneytracker.app.data.local.database.entities.CategoryRecommendationEntity

@Dao
interface CategoryRecommendationDao {
    @Query("SELECT * FROM category_recommendations WHERE payeePattern = :payeePattern LIMIT 1")
    suspend fun getRecommendation(payeePattern: String): CategoryRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recommendation: CategoryRecommendationEntity)

    @Query("""
        DELETE FROM category_recommendations 
        WHERE payeePattern NOT IN (
            SELECT payeePattern FROM category_recommendations 
            ORDER BY usageCount DESC, lastUsedTimestamp DESC 
            LIMIT 15000
        )
    """)
    suspend fun deleteLeastUsedRecords()
}