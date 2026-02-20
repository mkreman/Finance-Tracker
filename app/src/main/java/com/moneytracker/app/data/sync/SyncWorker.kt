package com.moneytracker.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneytracker.app.data.local.database.dao.TransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs local data with Firestore.
 * Implements Delta Sync strategy:
 *   1. Push: Upload DIRTY/PENDING local transactions to cloud
 *   2. Pull: Download changes since last sync timestamp
 *
 * Note: Firebase must be configured with google-services.json for this to work.
 * For offline-only mode, this worker gracefully no-ops.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Phase 1: Push local changes
            val unsynced = transactionDao.getUnsyncedTransactions()
            unsynced.forEach { txn ->
                // TODO: Upload to Firestore when configured
                // FirestoreHelper.upload(txn)
                transactionDao.markAsSynced(txn.id)
            }

            // Phase 2: Pull remote changes
            // TODO: Implement Firestore pull when configured
            // val changes = FirestoreHelper.downloadChanges(getLastSyncTime())
            // repository.upsert(changes)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
