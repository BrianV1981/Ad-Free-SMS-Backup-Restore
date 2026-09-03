package com.example.smsbackuprestore.data.extraction

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CallLog
import com.example.smsbackuprestore.data.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CallLogExtractionEngine(private val contentResolver: ContentResolver) {

    fun extractCallLogs(): Flow<CallLogEntry> = flow {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val numberIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val presentationIdx = c.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION)
            val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            
            // SubId / phone account ID handling might differ slightly based on Android version,
            // but we fetch PHONE_ACCOUNT_ID as a fallback for sub_id
            val subIdIdx = c.getColumnIndex("subscription_id")
            val phoneAccountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (c.moveToNext()) {
                val number = c.getString(numberIdx) ?: ""
                val date = c.getLong(dateIdx)
                val duration = c.getLong(durationIdx)
                val type = c.getInt(typeIdx)
                
                val presentation = if (presentationIdx != -1) c.getInt(presentationIdx) else 1
                val name = if (nameIdx != -1) c.getString(nameIdx) else null
                
                val subId = if (subIdIdx != -1) {
                    c.getInt(subIdIdx)
                } else if (phoneAccountIdIdx != -1) {
                    c.getString(phoneAccountIdIdx)?.toIntOrNull() ?: -1
                } else {
                    -1
                }

                val entry = CallLogEntry(
                    number = number,
                    date = date,
                    duration = duration,
                    type = type,
                    presentation = presentation,
                    subscriptionId = subId,
                    contactName = name
                )
                emit(entry)
            }
        }
    }.flowOn(Dispatchers.IO)
}
