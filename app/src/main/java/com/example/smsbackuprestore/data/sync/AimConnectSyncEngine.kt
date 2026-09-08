package com.example.smsbackuprestore.data.sync

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class AimConnectSyncEngine(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    fun uploadBackupToAimConnect(serverUrl: String, token: String, backupFile: File): Boolean {
        return try {
            val normalizedUrl = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
            val uploadUrl = "${normalizedUrl}/api/upload?path=sms_backup_${backupFile.name}"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    backupFile.name,
                    backupFile.asRequestBody("application/zip".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .header("X-API-Token", token)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("AimConnectSync", "Upload successful")
                true
            } else {
                Log.e("AimConnectSync", "Upload failed: " + response.code)
                false
            }
        } catch (e: Exception) {
            Log.e("AimConnectSync", "Exception during upload", e)
            false
        }
    }
}
