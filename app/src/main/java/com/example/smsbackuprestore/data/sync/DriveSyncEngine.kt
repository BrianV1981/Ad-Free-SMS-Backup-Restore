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
            val nestInFolder = prefs.getBoolean("nest_in_folder", false)
            
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

            // 5. Execute the upload request
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            // Return the uploaded file ID
            uploadedFile.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
