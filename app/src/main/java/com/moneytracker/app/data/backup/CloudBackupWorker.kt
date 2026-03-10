package com.moneytracker.app.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cloudBackupService: GoogleDriveBackupService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return when (cloudBackupService.backupNow()) {
            BackupResult.Success -> Result.success()
            BackupResult.NotSignedIn -> Result.success()
            BackupResult.NoBackupFound -> Result.success()
            is BackupResult.Error -> Result.retry()
        }
    }
}
