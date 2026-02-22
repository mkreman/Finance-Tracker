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
import com.moneytracker.app.data.local.database.dao.BudgetDao
import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import com.moneytracker.app.data.local.database.entities.*
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionSplitEntity::class,
        BudgetEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MoneyTrackerDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "money_tracker_db"

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add recurring transaction fields
                database.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN recurringInterval INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN recurringUnit TEXT")
                database.execSQL("ALTER TABLE transactions ADD COLUMN recurringEndDate INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN parentRecurringId TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add custom account type name field
                database.execSQL("ALTER TABLE accounts ADD COLUMN customTypeName TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN notifyForRecurringEntries INTEGER NOT NULL DEFAULT 1")
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
                        // Ensure default data exists after migrations or destructive resets
                        try {
                            val cursor = db.query("SELECT COUNT(*) FROM categories")
                            cursor.use {
                                if (it.moveToFirst()) {
                                    val count = it.getInt(0)
                                    if (count == 0) seedDefaultData(db)
                                }
                            }
                        } catch (ignored: Exception) {
                            // If categories table missing or other issue, attempt to seed anyway
                            try { seedDefaultData(db) } catch (_: Exception) {}
                        }
                    }
                })
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun seedDefaultData(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            // Default Accounts — use INSERT OR IGNORE so pre-existing rows are kept
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

            // Default Expense Categories
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
                // Income categories
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
