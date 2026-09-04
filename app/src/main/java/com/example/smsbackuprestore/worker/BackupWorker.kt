package com.example.smsbackuprestore.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsbackuprestore.data.archiver.BackupArchiver
import com.example.smsbackuprestore.data.archiver.BackupOrchestrator
import com.example.smsbackuprestore.data.extraction.CallLogExtractionEngine
import com.example.smsbackuprestore.data.extraction.ContactsExtractionEngine
import com.example.smsbackuprestore.data.extraction.ExtractionRepository
import com.example.smsbackuprestore.data.extraction.MmsExtractionEngine
import com.example.smsbackuprestore.data.extraction.SmsExtractionEngine
import com.example.smsbackuprestore.data.sync.DriveSyncEngine
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.io.File
import java.io.FileOutputStream

class BackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        if (account == null) {
            // Cannot upload to drive if not signed in
            return Result.failure()
        }

        try {
            val contentResolver = appContext.contentResolver
            val extractionRepository = ExtractionRepository(
                SmsExtractionEngine(contentResolver),
                MmsExtractionEngine(contentResolver),
                CallLogExtractionEngine(contentResolver),
                ContactsExtractionEngine(contentResolver)
            )

            val archiver = BackupArchiver()
            val backupOrchestrator = BackupOrchestrator(contentResolver, extractionRepository, archiver)
            val driveSyncEngine = DriveSyncEngine(appContext)

            val backupFile = File(appContext.cacheDir, "sms_backup_${System.currentTimeMillis()}.zip")
            
            val prefs = appContext.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
            val isEncrypted = prefs.getBoolean("encryption_enabled", false)
            val includeMmsMedia = prefs.getBoolean("include_mms_media", false)
            val password = if (isEncrypted) "default_password".toCharArray() else null // TODO: retrieve secure password in a real app

            FileOutputStream(backupFile).use { fos ->
                backupOrchestrator.performBackup(fos, password, includeMmsMedia)
            }

            val messageCount = extractionRepository.getTotalMessagesCount()
            val fileId = driveSyncEngine.uploadBackupToDrive(account, backupFile, messageCount)

            backupFile.delete()

            return if (fileId != null) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
