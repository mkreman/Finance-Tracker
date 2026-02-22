package com.moneytracker.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.moneytracker.app.data.recurring.RecurringTransactionManager
import com.moneytracker.app.data.sync.SyncManager
import com.moneytracker.app.widget.MoneyTrackerWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var recurringTransactionManager: RecurringTransactionManager

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

    // 🌟 ADD THIS NATIVE ANDROID CALLBACK 🌟
    // We use the fully qualified name (android.content.res.Configuration) 
    // to avoid clashing with androidx.work.Configuration above.
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // This fires instantly whenever the user pulls down their quick settings 
        // and toggles System Dark Mode while the app is in the background.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MoneyTrackerWidget().updateAll(this@MoneyTrackerApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}