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

            // 3. Prepare the metadata (file name and MIME type)
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupFile.name
                this.mimeType = mimeType
                // We could also set parents to a specific AppData folder if using DRIVE_APPDATA
            }

            // 4. Create the media content
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
