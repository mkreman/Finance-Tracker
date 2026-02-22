package com.moneytracker.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.moneytracker.app.data.recurring.RecurringTransactionManager
import com.moneytracker.app.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var recurringTransactionManager: RecurringTransactionManager

    // Required for Hilt to inject dependencies into your Workers
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic cloud sync
        syncManager.schedulePeriodicSync()
        
        // Schedule recurring transaction background check
        recurringTransactionManager.scheduleRecurringCheck()
    }
}
