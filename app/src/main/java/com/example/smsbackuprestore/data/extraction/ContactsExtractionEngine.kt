package com.example.smsbackuprestore.data.extraction

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

class ContactsExtractionEngine(private val contentResolver: ContentResolver) {

    /**
     * Extracts all contacts as VCard formatted strings.
     * Android OS natively provides a way to export contacts as VCards via ContactsContract.
     */
    fun extractContactsAsVCard(): Flow<String> = flow {
        // First, get all lookup keys for all contacts
        val lookupKeys = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.Contacts.LOOKUP_KEY)
        
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use { c ->
            val lookupKeyIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            while (c.moveToNext()) {
                val key = c.getString(lookupKeyIdx)
                if (!key.isNullOrBlank()) {
                    lookupKeys.add(key)
                }
            }
        }

        // Now fetch VCards in batches to avoid extremely long URIs
        val batchSize = 100
        for (i in lookupKeys.indices step batchSize) {
            val batch = lookupKeys.subList(i, minOf(i + batchSize, lookupKeys.size))
            val lookupKeysJoined = batch.joinToString(":")
            
            // The MULTI_VCARD_URI takes a colon-separated list of lookup keys
            val multiVcardUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI,
                Uri.encode(lookupKeysJoined)
            )

            try {
                contentResolver.openInputStream(multiVcardUri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var vcardChunk = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        vcardChunk.append(line).append("\n")
                        // Each vCard ends with END:VCARD
                        if (line?.trim() == "END:VCARD") {
                            emit(vcardChunk.toString())
                            vcardChunk.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                // If a batch fails, log it or handle appropriately
                e.printStackTrace()
            }
        }
    }.flowOn(Dispatchers.IO)
}
