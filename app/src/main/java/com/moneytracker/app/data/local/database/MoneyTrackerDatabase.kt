package com.moneytracker.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.app.data.local.database.converters.Converters
import com.moneytracker.app.data.local.database.dao.AccountDao
import com.moneytracker.app.data.local.database.dao.BudgetAlertDao
import com.moneytracker.app.data.local.database.dao.BudgetDao
import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.dao.CategoryRecommendationDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import com.moneytracker.app.data.local.database.entities.*
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionSplitEntity::class,
        BudgetEntity::class,
        CategoryRecommendationEntity::class // Added new entity
    ],
    version = 11, // Bumped to 11
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MoneyTrackerDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryRecommendationDao(): CategoryRecommendationDao
    abstract fun budgetAlertDao(): BudgetAlertDao

    companion object {
        const val DATABASE_NAME = "money_tracker_db"

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringInterval INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringUnit TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringEndDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN parentRecurringId TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN customTypeName TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN notifyForRecurringEntries INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Added Migration for Category Recommendations
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_recommendations` (
                        `payeePattern` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        `lastUsedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`payeePattern`)
                    )
                """)
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiptUri TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category_recommendations ADD COLUMN accountId TEXT")
            }
        }

        fun buildDatabase(context: Context): MoneyTrackerDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MoneyTrackerDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultData(db)
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            val cursor = db.query("SELECT COUNT(*) FROM categories")
                            cursor.use {
                                if (it.moveToFirst()) {
                                    val count = it.getInt(0)
                                    if (count == 0) seedDefaultData(db)
                                }
                            }
                        } catch (ignored: Exception) {
                            try { seedDefaultData(db) } catch (_: Exception) {}
                        }
                    }
                })
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun seedDefaultData(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            val accounts = listOf(
                Triple("Wallet", "CASH", "#FF9800"),
                Triple("Bank HDFC", "BANK", "#2196F3"),
                Triple("Bank SBI", "BANK", "#4CAF50")
            )
            accounts.forEach { (name, type, color) ->
                val id = UUID.randomUUID().toString()
                val icon = if (type == "CASH") "wallet" else "bank"
                db.execSQL(
                    """INSERT OR IGNORE INTO accounts
                        (id, name, type, customTypeName, initialBalance, currentBalance, currency, colorHex, iconKey, isActive, createdAt, modifiedAt, isDeleted, syncStatus)
                        VALUES ('$id', '$name', '$type', NULL, 0.0, 0.0, 'INR', '$color', '$icon', 1, $now, $now, 0, 'PENDING')"""
                )
            }

            data class CatSeed(val id: String, val name: String, val icon: String, val type: String, val color: String, val order: Int)
            val categories = listOf(
                CatSeed("cat-food", "Food", "restaurant", "EXPENSE", "#FF5722", 0),
                CatSeed("cat-transport", "Transport", "directions_car", "EXPENSE", "#2196F3", 1),
                CatSeed("cat-shopping", "Shopping", "shopping_bag", "EXPENSE", "#9C27B0", 2),
                CatSeed("cat-entertainment", "Entertainment", "movie", "EXPENSE", "#E91E63", 3),
                CatSeed("cat-health", "Health", "medical_services", "EXPENSE", "#4CAF50", 4),
                CatSeed("cat-education", "Education", "school", "EXPENSE", "#3F51B5", 5),
                CatSeed("cat-bills", "Bills", "receipt", "EXPENSE", "#FF9800", 6),
                CatSeed("cat-rent", "Rent", "home", "EXPENSE", "#795548", 7),
                CatSeed("cat-bike", "Bike", "two_wheeler", "EXPENSE", "#607D8B", 8),
                CatSeed("cat-cats", "Cats", "pets", "EXPENSE", "#FFEB3B", 9),
                CatSeed("cat-other-expense", "Other", "more_horiz", "EXPENSE", "#9E9E9E", 10),
                CatSeed("cat-auto-expense", "AutoDetected", "auto_awesome", "EXPENSE", "#607D8B", 11),
                CatSeed("cat-salary", "Salary", "work", "INCOME", "#4CAF50", 0),
                CatSeed("cat-freelance", "Freelance", "laptop", "INCOME", "#2196F3", 1),
                CatSeed("cat-investment-income", "Investment", "trending_up", "INCOME", "#FF9800", 2),
                CatSeed("cat-gift", "Gift", "card_giftcard", "INCOME", "#E91E63", 3),
                CatSeed("cat-other-income", "Other", "more_horiz", "INCOME", "#9E9E9E", 4),
                CatSeed("cat-auto-income", "AutoDetected", "auto_awesome", "INCOME", "#607D8B", 5),
            )
            categories.forEach { cat ->
                db.execSQL(
                    """INSERT OR IGNORE INTO categories
                      (id, name, iconKey, type, colorHex, sortOrder, isDeleted, createdAt, modifiedAt)
                      VALUES ('${cat.id}', '${cat.name}', '${cat.icon}', '${cat.type}', '${cat.color}', ${cat.order}, 0, $now, $now)"""
                )
            }
        }
    }
}
