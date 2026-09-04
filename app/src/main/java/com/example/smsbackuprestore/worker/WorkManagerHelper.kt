package com.example.smsbackuprestore.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val BACKUP_WORK_NAME = "auto_backup_work"

    fun scheduleBackup(
        context: Context,
        intervalDays: Long,
        requireWifi: Boolean,
        requireCharging: Boolean
    ) {
        val constraintsBuilder = Constraints.Builder()
        
        if (requireWifi) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED)
        } else {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        
        if (requireCharging) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalDays, TimeUnit.DAYS
        )
        .setConstraints(constraintsBuilder.build())
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupWorkRequest
        )
    }

    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }
}
