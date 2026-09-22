package com.example.smsbackuprestore.data.archiver

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

data class RestoreResult(
    var parsedSms: Int = 0,
    var insertedSms: Int = 0,
    var skippedSms: Int = 0,
    var errorSms: Int = 0,
    var parsedMms: Int = 0,
    var insertedMms: Int = 0,
    var lastError: String? = null
)

class RestoreOrchestrator {

    suspend fun parseMessagesXmlDryRun(
        xmlFile: File,
        onProgress: (Int, Int) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        val result = RestoreResult()
        
        if (!xmlFile.exists()) return@withContext result
        
        FileInputStream(xmlFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "sms" -> {
                            result.parsedSms++
                            if ((result.parsedSms + result.parsedMms) % 100 == 0) {
                                onProgress(result.parsedSms, result.parsedMms)
                            }
                        }
                        "mms" -> {
                            result.parsedMms++
                            if ((result.parsedSms + result.parsedMms) % 100 == 0) {
                                onProgress(result.parsedSms, result.parsedMms)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        
        onProgress(result.parsedSms, result.parsedMms)
        result
    }

    suspend fun parseAndRestoreMessages(
        context: android.content.Context,
        xmlFile: File,
        onProgress: (Int, Int) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val result = RestoreResult()
        
        if (!xmlFile.exists()) return@withContext result

        // 1. Pre-load existing SMS hashes for duplicate detection
        val existingSmsHashes = hashSetOf<String>()
        try {
            contentResolver.query(android.provider.Telephony.Sms.CONTENT_URI, arrayOf("address", "date"), null, null, null)?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val dateIndex = cursor.getColumnIndex("date")
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIndex) ?: ""
                    val date = cursor.getString(dateIndex) ?: ""
                    existingSmsHashes.add("-")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        FileInputStream(xmlFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "sms" -> {
                            val address = parser.getAttributeValue(null, "address") ?: ""
                            val date = parser.getAttributeValue(null, "date") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: "1"
                            val body = parser.getAttributeValue(null, "body") ?: ""
                            val read = parser.getAttributeValue(null, "read") ?: "1"
                            
                            val hash = "-"
                            if (!existingSmsHashes.contains(hash)) {
                                val values = android.content.ContentValues().apply {
                                    put(android.provider.Telephony.Sms.ADDRESS, address)
                                    put(android.provider.Telephony.Sms.DATE, date)
                                    put(android.provider.Telephony.Sms.TYPE, type)
                                    put(android.provider.Telephony.Sms.BODY, body)
                                    put(android.provider.Telephony.Sms.READ, read)
                                    
                                    try {
                                        if (address.isNotBlank()) {
                                            val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(context, address)
                                            put(android.provider.Telephony.Sms.THREAD_ID, threadId)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                try {
                                    val uri = contentResolver.insert(android.provider.Telephony.Sms.CONTENT_URI, values)
                                    if (uri != null) {
                                        existingSmsHashes.add(hash)
                                        result.insertedSms++
                                    } else {
                                        result.errorSms++
                                        if (result.lastError == null) result.lastError = "insert() returned null URI"
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    result.errorSms++
                                    result.lastError = e.localizedMessage ?: e.javaClass.simpleName
                                }
                            } else {
                                result.skippedSms++
                            }
                            
                            result.parsedSms++
                            if ((result.parsedSms + result.parsedMms) % 100 == 0) {
                                onProgress(result.parsedSms, result.parsedMms)
                            }
                        }
                        "mms" -> {
                            result.parsedMms++
                            if ((result.parsedSms + result.parsedMms) % 100 == 0) {
                                onProgress(result.parsedSms, result.parsedMms)
                            }
                            // Note: MMS insertion requires multi-table inserts (pdu, addr, part).
                            // Currently stubbed for dry-run parsing
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        
        onProgress(result.parsedSms, result.parsedMms)
        result
    }
}
