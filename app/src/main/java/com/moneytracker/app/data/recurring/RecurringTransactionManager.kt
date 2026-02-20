package com.moneytracker.app.data.recurring

import android.content.Context
import androidx.work.*
import com.moneytracker.app.workers.RecurringTransactionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a periodic worker that runs daily to check for 
     * recurring transactions and create new occurrences.
     */
    fun scheduleRecurringCheck() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val recurringRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            RECURRING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            recurringRequest
        )
    }

    /**
     * Trigger an immediate check for recurring transactions.
     */
    fun checkNow() {
        val recurringRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
            .build()

        workManager.enqueue(recurringRequest)
    }

    fun cancelRecurringCheck() {
        workManager.cancelUniqueWork(RECURRING_WORK_NAME)
    }

    companion object {
        const val RECURRING_WORK_NAME = "RecurringTransactionCheck"
    }
}
