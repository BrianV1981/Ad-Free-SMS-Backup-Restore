package com.example.smsbackuprestore.data.archiver

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

class RestoreOrchestrator {

    suspend fun parseMessagesXmlDryRun(
        xmlFile: File,
        onProgress: (Int, Int) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var smsCount = 0
        var mmsCount = 0
        
        if (!xmlFile.exists()) return@withContext Pair(0, 0)
        
        FileInputStream(xmlFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "sms" -> {
                            smsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                        }
                        "mms" -> {
                            mmsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        
        onProgress(smsCount, mmsCount)
        Pair(smsCount, mmsCount)
    }

    suspend fun parseAndRestoreMessages(
        context: android.content.Context,
        xmlFile: File,
        onProgress: (Int, Int) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var smsCount = 0
        var mmsCount = 0
        
        if (!xmlFile.exists()) return@withContext Pair(0, 0)

        // 1. Pre-load existing SMS hashes for duplicate detection
        val existingSmsHashes = hashSetOf<String>()
        try {
            contentResolver.query(android.provider.Telephony.Sms.CONTENT_URI, arrayOf("address", "date"), null, null, null)?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val dateIndex = cursor.getColumnIndex("date")
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIndex) ?: ""
                    val date = cursor.getString(dateIndex) ?: ""
                    existingSmsHashes.add("${address}-${date}")
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
                            
                            val hash = "${address}-${date}"
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
                                    contentResolver.insert(android.provider.Telephony.Sms.CONTENT_URI, values)
                                    existingSmsHashes.add(hash)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            smsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                        }
                        "mms" -> {
                            mmsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                            // Note: MMS insertion requires multi-table inserts (pdu, addr, part).
                            // Simplified for this iteration as it requires handling parts separately.
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        
        onProgress(smsCount, mmsCount)
        Pair(smsCount, mmsCount)
    }
}
