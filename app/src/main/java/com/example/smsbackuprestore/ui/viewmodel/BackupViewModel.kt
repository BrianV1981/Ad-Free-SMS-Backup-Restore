package com.example.smsbackuprestore.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsbackuprestore.data.archiver.BackupArchiver
import com.example.smsbackuprestore.data.archiver.BackupOrchestrator
import com.example.smsbackuprestore.data.extraction.ExtractionRepository
import com.example.smsbackuprestore.data.extraction.SmsExtractionEngine
import com.example.smsbackuprestore.data.extraction.MmsExtractionEngine
import com.example.smsbackuprestore.data.extraction.CallLogExtractionEngine
import com.example.smsbackuprestore.data.extraction.ContactsExtractionEngine
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

    private val contentResolver = getApplication<android.app.Application>().contentResolver
    private val extractionRepository = ExtractionRepository(
        SmsExtractionEngine(contentResolver),
        MmsExtractionEngine(contentResolver),
        CallLogExtractionEngine(contentResolver),
        ContactsExtractionEngine(contentResolver)
    )
    private val backupArchiver by lazy { BackupArchiver() }
    private val backupOrchestrator by lazy { BackupOrchestrator(contentResolver, extractionRepository, backupArchiver) }

    fun startBackup(outputDir: File) {
        if (_backupState.value is BackupState.BackingUp) return
        
        _backupState.value = BackupState.BackingUp(0f)
        
        viewModelScope.launch {
            try {
                val backupFile = File(outputDir, "sms_backup_${System.currentTimeMillis()}.zip")
                val prefs = getApplication<android.app.Application>().getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)
                val includeMmsMedia = prefs.getBoolean("include_mms_media", false)
                
                FileOutputStream(backupFile).use { fos ->
                    val isEncrypted = prefs.getBoolean("encryption_enabled", false)
                    val password = if (isEncrypted) prefs.getString("encryption_password", "")?.toCharArray() else null
                    backupOrchestrator.performBackup(fos, password, includeMmsMedia) { progress ->
                        _backupState.value = BackupState.BackingUp(progress)
                    }
                }
                
                // Check if Google Drive is enabled (Signed In)
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account != null) {
                    val messageCount = extractionRepository.getTotalMessagesCount()
                    val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
                    val fileId = driveSyncEngine.uploadBackupToDrive(account, backupFile, messageCount)
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
    
    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()
    
    fun fetchRestoreOptions() {
        _restoreState.value = RestoreState.Loading
        viewModelScope.launch {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
            if (account == null) {
                _restoreState.value = RestoreState.Error("Google Drive is not connected. Enable it in settings first.")
                return@launch
            }
            
            val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
            val manifest = driveSyncEngine.fetchManifest(account)
            
            if (manifest == null || manifest.entries.isEmpty()) {
                _restoreState.value = RestoreState.Error("No backups found in Google Drive.")
            } else {
                _restoreState.value = RestoreState.Options(manifest)
            }
        }
    }
    
    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }

    fun startRestoreDryRun(manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry) {
        viewModelScope.launch {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account == null) {
                    _restoreState.value = RestoreState.Error("Not signed into Google Drive.")
                    return@launch
                }
                
                _restoreState.value = RestoreState.Downloading
                
                val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
                val destFile = File(getApplication<android.app.Application>().cacheDir, "restore_temp.zip")
                val downloadSuccess = driveSyncEngine.downloadBackupFromDrive(account, manifestEntry.fileId, destFile)
                
                if (!downloadSuccess) {
                    _restoreState.value = RestoreState.Error("Failed to download backup from Google Drive.")
                    return@launch
                }
                
                _restoreState.value = RestoreState.Extracting
                val extractDir = File(getApplication<android.app.Application>().cacheDir, "restore_extracted")
                if (extractDir.exists()) extractDir.deleteRecursively()
                extractDir.mkdirs()
                
                // Get password if encrypted
                val prefs = getApplication<android.app.Application>().getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)
                val isEncrypted = prefs.getBoolean("encryption_enabled", false)
                val password = if (isEncrypted) prefs.getString("encryption_password", "")?.toCharArray() else null
                
                val archiver = BackupArchiver()
                archiver.extractArchive(destFile, extractDir, password)
                
                _restoreState.value = RestoreState.Parsing(0, 0)
                val messagesFile = File(extractDir, "messages.xml")
                
                val restoreOrchestrator = com.example.smsbackuprestore.data.archiver.RestoreOrchestrator()
                val counts = restoreOrchestrator.parseMessagesXmlDryRun(messagesFile) { sms, mms ->
                    _restoreState.value = RestoreState.Parsing(sms, mms)
                }
                
                // Clean up
                destFile.delete()
                extractDir.deleteRecursively()
                
                _restoreState.value = RestoreState.Success(counts.first, counts.second)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _restoreState.value = RestoreState.Error(e.localizedMessage ?: "Unknown error during dry-run")
            }
        }
    }

    fun setRestoreError(message: String) {
        _restoreState.value = RestoreState.Error(message)
    }
    
    fun startRealRestore(manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry) {
        viewModelScope.launch {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account == null) {
                    _restoreState.value = RestoreState.Error("Not signed into Google Drive.")
                    return@launch
                }
                
                _restoreState.value = RestoreState.Downloading
                
                val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
                val destFile = File(getApplication<android.app.Application>().cacheDir, "restore_temp.zip")
                val downloadSuccess = driveSyncEngine.downloadBackupFromDrive(account, manifestEntry.fileId, destFile)
                
                if (!downloadSuccess) {
                    _restoreState.value = RestoreState.Error("Failed to download backup from Google Drive.")
                    return@launch
                }
                
                _restoreState.value = RestoreState.Extracting
                val extractDir = File(getApplication<android.app.Application>().cacheDir, "restore_extracted")
                if (extractDir.exists()) extractDir.deleteRecursively()
                extractDir.mkdirs()
                
                val prefs = getApplication<android.app.Application>().getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)
                val isEncrypted = prefs.getBoolean("encryption_enabled", false)
                val password = if (isEncrypted) prefs.getString("encryption_password", "")?.toCharArray() else null 
                
                val archiver = BackupArchiver()
                archiver.extractArchive(destFile, extractDir, password)
                
                _restoreState.value = RestoreState.Parsing(0, 0)
                val messagesFile = File(extractDir, "messages.xml")
                
                val restoreOrchestrator = com.example.smsbackuprestore.data.archiver.RestoreOrchestrator()
                val counts = restoreOrchestrator.parseAndRestoreMessages(contentResolver, messagesFile) { sms, mms ->
                    _restoreState.value = RestoreState.Parsing(sms, mms)
                }
                
                // Clean up
                destFile.delete()
                extractDir.deleteRecursively()
                
                _restoreState.value = RestoreState.SuccessReal(counts.first, counts.second)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _restoreState.value = RestoreState.Error(e.localizedMessage ?: "Unknown error during restore")
            }
        }
    }
}

sealed class RestoreState {
    object Idle : RestoreState()
    object Loading : RestoreState()
    object Downloading : RestoreState()
    object Extracting : RestoreState()
    data class Parsing(val smsCount: Int, val mmsCount: Int) : RestoreState()
    data class Success(val smsCount: Int, val mmsCount: Int) : RestoreState()
    data class SuccessReal(val smsCount: Int, val mmsCount: Int) : RestoreState()
    data class Options(val manifest: com.example.smsbackuprestore.data.model.BackupManifest) : RestoreState()
    data class Error(val message: String) : RestoreState()
}
