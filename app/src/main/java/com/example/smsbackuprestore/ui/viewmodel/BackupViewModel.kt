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
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.util.Date

sealed class BackupState {
    object Idle : BackupState()
    data class BackingUp(val progress: Float = 0f) : BackupState()
    data class Success(val file: File, val date: Date, val messageCount: Int = 0) : BackupState()
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


    private val _availableCounts = MutableStateFlow<Pair<Int, Int>?>(null)
    val availableCounts: StateFlow<Pair<Int, Int>?> = _availableCounts.asStateFlow()

    fun fetchCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = extractionRepository.getTotalMessagesCount()
                val contacts = extractionRepository.getTotalContactsCount()
                _availableCounts.value = Pair(messages, contacts)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

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
                val messageCount = extractionRepository.getTotalMessagesCount()
                
                // AIM-Connect Sync
                val aimConnectUrl = prefs.getString("aim_connect_url", "")
                val aimConnectToken = prefs.getString("aim_connect_token", "")
                if (!aimConnectUrl.isNullOrBlank() && !aimConnectToken.isNullOrBlank()) {
                    val aimConnectSyncEngine = com.example.smsbackuprestore.data.sync.AimConnectSyncEngine(getApplication())
                    val success = aimConnectSyncEngine.uploadBackupToAimConnect(aimConnectUrl, aimConnectToken, backupFile)
                    if (!success) {
                        _backupState.value = BackupState.Error("Backup created locally, but AIM-Connect upload failed.")
                        return@launch
                    }
                }

                // Google Drive Sync
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account != null) {
                    val driveSyncEngine = com.example.smsbackuprestore.data.sync.DriveSyncEngine(getApplication())
                    val fileId = driveSyncEngine.uploadBackupToDrive(account, backupFile, messageCount)
                    if (fileId == null) {
                        _backupState.value = BackupState.Error("Backup created locally, but Google Drive upload failed.")
                        return@launch
                    }
                }
                
                _backupState.value = BackupState.Success(backupFile, Date(), messageCount)
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
        startRestoreProcess(manifestEntry, isDryRun = true)
    }

    fun startRealRestore(manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry) {
        startRestoreProcess(manifestEntry, isDryRun = false)
    }

    private fun startRestoreProcess(manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry, isDryRun: Boolean) {
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
                
                // Get password from prefs if it exists, otherwise null
                val prefs = getApplication<android.app.Application>().getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)
                val isEncryptedPref = prefs.getBoolean("encryption_enabled", false)
                val savedPassword = if (isEncryptedPref) prefs.getString("encryption_password", "")?.toCharArray() else null
                
                executeExtractionAndParsing(manifestEntry, isDryRun, destFile, savedPassword)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _restoreState.value = RestoreState.Error(e.localizedMessage ?: "Unknown error during restore process")
            }
        }
    }
    
    fun resumeRestoreWithPassword(manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry, isDryRun: Boolean, password: CharArray) {
        viewModelScope.launch {
            val destFile = File(getApplication<android.app.Application>().cacheDir, "restore_temp.zip")
            if (!destFile.exists()) {
                _restoreState.value = RestoreState.Error("Downloaded backup file is missing. Please try again.")
                return@launch
            }
            executeExtractionAndParsing(manifestEntry, isDryRun, destFile, password)
        }
    }
    
    private suspend fun executeExtractionAndParsing(
        manifestEntry: com.example.smsbackuprestore.data.model.BackupEntry,
        isDryRun: Boolean,
        destFile: File,
        password: CharArray?
    ) {
        try {
            _restoreState.value = RestoreState.Extracting
            val extractDir = File(getApplication<android.app.Application>().cacheDir, "restore_extracted")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()
            
            // Check if ZIP is encrypted using Zip4j before trying to extract blindly
            val zipFile = net.lingala.zip4j.ZipFile(destFile)
            if (zipFile.isEncrypted && (password == null || password.isEmpty())) {
                _restoreState.value = RestoreState.RequirePassword(manifestEntry, isDryRun)
                return
            }
            
            val archiver = BackupArchiver()
            archiver.extractArchive(destFile, extractDir, password)
            
            _restoreState.value = RestoreState.Parsing(0, 0)
            val messagesFile = File(extractDir, "messages.xml")
            val restoreOrchestrator = com.example.smsbackuprestore.data.archiver.RestoreOrchestrator()
            
            val counts = if (isDryRun) {
                restoreOrchestrator.parseMessagesXmlDryRun(messagesFile) { sms, mms ->
                    _restoreState.value = RestoreState.Parsing(sms, mms)
                }
            } else {
                restoreOrchestrator.parseAndRestoreMessages(contentResolver, messagesFile) { sms, mms ->
                    _restoreState.value = RestoreState.Parsing(sms, mms)
                }
            }
            
            // Clean up
            destFile.delete()
            extractDir.deleteRecursively()
            
            if (isDryRun) {
                _restoreState.value = RestoreState.Success(counts.first, counts.second)
            } else {
                _restoreState.value = RestoreState.SuccessReal(counts.first, counts.second)
            }
            
        } catch (e: net.lingala.zip4j.exception.ZipException) {
            e.printStackTrace()
            // If the zip exception is due to a bad password, ask again
            if (e.message?.contains("password", ignoreCase = true) == true || e.message?.contains("mac", ignoreCase = true) == true) {
                _restoreState.value = RestoreState.RequirePassword(manifestEntry, isDryRun)
            } else {
                _restoreState.value = RestoreState.Error("Corrupt ZIP or extraction failed: ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _restoreState.value = RestoreState.Error(e.localizedMessage ?: "Unknown error during extraction")
        }
    }

    fun setRestoreError(message: String) {
        _restoreState.value = RestoreState.Error(message)
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
    data class RequirePassword(val entry: com.example.smsbackuprestore.data.model.BackupEntry, val isDryRun: Boolean) : RestoreState()
    data class Error(val message: String) : RestoreState()
}
