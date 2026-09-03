package com.example.smsbackuprestore.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsbackuprestore.data.archiver.BackupArchiver
import com.example.smsbackuprestore.data.archiver.BackupOrchestrator
import com.example.smsbackuprestore.data.extraction.ExtractionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date

sealed class BackupState {
    object Idle : BackupState()
    data class BackingUp(val progress: Float = 0f) : BackupState()
    data class Success(val file: File, val date: Date) : BackupState()
    data class Error(val message: String) : BackupState()
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val extractionRepository by lazy { 
        ExtractionRepository(
            com.example.smsbackuprestore.data.extraction.SmsExtractionEngine(application.contentResolver),
            com.example.smsbackuprestore.data.extraction.MmsExtractionEngine(application.contentResolver),
            com.example.smsbackuprestore.data.extraction.CallLogExtractionEngine(application.contentResolver),
            com.example.smsbackuprestore.data.extraction.ContactsExtractionEngine(application.contentResolver)
        ) 
    }
    private val backupArchiver by lazy { BackupArchiver() }
    private val backupOrchestrator by lazy { BackupOrchestrator(extractionRepository, backupArchiver) }

    fun startBackup(outputDir: File) {
        if (_backupState.value is BackupState.BackingUp) return
        
        _backupState.value = BackupState.BackingUp(0f)
        
        viewModelScope.launch {
            try {
                val backupFile = File(outputDir, "sms_backup_${System.currentTimeMillis()}.zip")
                FileOutputStream(backupFile).use { fos ->
                    backupOrchestrator.performBackup(fos) { progress ->
                        _backupState.value = BackupState.BackingUp(progress)
                    }
                }
                
                // Check if Google Drive is enabled (Signed In)
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account != null) {
                    val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
                    val fileId = driveSyncEngine.uploadBackupToDrive(account, backupFile)
                    if (fileId == null) {
                        _backupState.value = BackupState.Error("Backup created locally, but Google Drive upload failed.")
                        return@launch
                    }
                }
                
                _backupState.value = BackupState.Success(backupFile, Date())
            } catch (e: Exception) {
                e.printStackTrace()
                _backupState.value = BackupState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
    
    fun resetState() {
        _backupState.value = BackupState.Idle
    }
}
