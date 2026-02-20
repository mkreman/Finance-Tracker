package com.moneytracker.app.di

import android.content.Context
import com.moneytracker.app.data.local.database.MoneyTrackerDatabase
import com.moneytracker.app.data.local.database.dao.AccountDao
import com.moneytracker.app.data.local.database.dao.BudgetDao
import com.moneytracker.app.data.local.database.dao.CategoryDao
import com.moneytracker.app.data.local.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyTrackerDatabase {
        return MoneyTrackerDatabase.buildDatabase(context)
    }

    @Provides
    fun provideAccountDao(database: MoneyTrackerDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideCategoryDao(database: MoneyTrackerDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideTransactionDao(database: MoneyTrackerDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideBudgetDao(database: MoneyTrackerDatabase): BudgetDao {
        return database.budgetDao()
    }
}
