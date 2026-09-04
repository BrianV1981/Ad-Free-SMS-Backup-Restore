package com.example.smsbackuprestore.data.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class DriveSyncEngine(private val context: Context) {

    /**
     * Uploads the generated ZIP backup to Google Drive.
     * Uses the restricted DriveScopes.DRIVE_FILE scope so we only access files created by our app.
     */
    suspend fun uploadBackupToDrive(
        account: GoogleSignInAccount,
        backupFile: File,
        messageCount: Int,
        mimeType: String = "application/zip"
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Convert GoogleSignInAccount to a GoogleAccountCredential
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            // 2. Build the Drive service
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Ad-Free SMS Backup")
                .build()

            // 3. Check preferences for folder nesting
            val prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
            val nestInFolder = prefs.getBoolean("nest_in_folder", true)
            
            var parentFolderId: String? = null
            
            if (nestInFolder) {
                // Search for the folder
                val query = "mimeType = 'application/vnd.google-apps.folder' and name = 'Ad-Free SMS Backups' and trashed = false"
                val fileList = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()
                    
                if (fileList.files.isNotEmpty()) {
                    parentFolderId = fileList.files[0].id
                } else {
                    // Create the folder
                    val folderMetadata = com.google.api.services.drive.model.File().apply {
                        name = "Ad-Free SMS Backups"
                        this.mimeType = "application/vnd.google-apps.folder"
                    }
                    val folder = driveService.files().create(folderMetadata)
                        .setFields("id")
                        .execute()
                    parentFolderId = folder.id
                }
            }

            // 4. Prepare the metadata (file name and MIME type)
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupFile.name
                this.mimeType = mimeType
                if (parentFolderId != null) {
                    parents = listOf(parentFolderId)
                }
            }

            // 5. Create the media content
            val mediaContent = InputStreamContent(
                mimeType,
                FileInputStream(backupFile)
            ).apply {
                length = backupFile.length()
            }

            // 6. Execute the upload request
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            // 7. Handle Manifest
            if (parentFolderId != null) {
                updateManifest(driveService, parentFolderId, uploadedFile.id, backupFile.name, messageCount)
            }

            // Return the uploaded file ID
            uploadedFile.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun updateManifest(
        driveService: Drive,
        parentFolderId: String,
        newFileId: String,
        filename: String,
        messageCount: Int
    ) {
        val query = "mimeType = 'application/json' and name = 'history.json' and '$parentFolderId' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
            
        val gson = com.google.gson.Gson()
        var manifest = com.example.smsbackuprestore.data.model.BackupManifest()
        var existingManifestId: String? = null
        
        if (fileList.files.isNotEmpty()) {
            existingManifestId = fileList.files[0].id
            try {
                val outputStream = java.io.ByteArrayOutputStream()
                driveService.files().get(existingManifestId).executeMediaAndDownloadTo(outputStream)
                val json = String(outputStream.toByteArray())
                manifest = gson.fromJson(json, com.example.smsbackuprestore.data.model.BackupManifest::class.java) ?: manifest
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val newEntry = com.example.smsbackuprestore.data.model.BackupEntry(
            fileId = newFileId,
            filename = filename,
            date = System.currentTimeMillis(),
            messageCount = messageCount
        )
        
        manifest.entries = manifest.entries + newEntry
        
        val newJson = gson.toJson(manifest)
        val tempFile = File.createTempFile("history", ".json")
        tempFile.writeText(newJson)
        
        val manifestMetadata = com.google.api.services.drive.model.File().apply {
            name = "history.json"
            mimeType = "application/json"
            parents = listOf(parentFolderId)
        }
        
        val manifestContent = InputStreamContent("application/json", FileInputStream(tempFile))
        
        if (existingManifestId != null) {
            // Update existing
            driveService.files().update(existingManifestId, com.google.api.services.drive.model.File(), manifestContent).execute()
        } else {
            // Create new
            driveService.files().create(manifestMetadata, manifestContent).execute()
        }
        tempFile.delete()
    }
    
    suspend fun fetchManifest(account: GoogleSignInAccount): com.example.smsbackuprestore.data.model.BackupManifest? = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = account.account
            
            val driveService = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Ad-Free SMS Backup")
                .build()
                
            val folderQuery = "mimeType = 'application/vnd.google-apps.folder' and name = 'Ad-Free SMS Backups' and trashed = false"
            val folderList = driveService.files().list().setQ(folderQuery).setSpaces("drive").setFields("files(id)").execute()
            
            if (folderList.files.isEmpty()) return@withContext null
            
            val parentFolderId = folderList.files[0].id
            val query = "mimeType = 'application/json' and name = 'history.json' and '$parentFolderId' in parents and trashed = false"
            val fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id)").execute()
            
            if (fileList.files.isEmpty()) return@withContext null
            
            val manifestId = fileList.files[0].id
            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(manifestId).executeMediaAndDownloadTo(outputStream)
            val json = String(outputStream.toByteArray())
            return@withContext com.google.gson.Gson().fromJson(json, com.example.smsbackuprestore.data.model.BackupManifest::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadBackupFromDrive(
        account: GoogleSignInAccount,
        fileId: String,
        destFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = account.account
            
            val driveService = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Ad-Free SMS Backup")
                .build()

            val outputStream = java.io.FileOutputStream(destFile)
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
