package com.example.smsbackuprestore.data.extraction

import android.content.ContentResolver
import android.database.Cursor
import android.provider.Telephony
import com.example.smsbackuprestore.data.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SmsExtractionEngine(private val contentResolver: ContentResolver) {

    /**
     * Extracts SMS messages from the device asynchronously using a Flow.
     * This handles large datasets without blocking the main thread.
     */
    fun extractSms(): Flow<SmsMessage> = flow {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.BODY,
            Telephony.Sms.READ,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.LOCKED,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        val cursor: Cursor? = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use { c ->
            val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val dateSentIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT)
            val lockedIdx = c.getColumnIndexOrThrow(Telephony.Sms.LOCKED)
            val subIdIdx = c.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)

            while (c.moveToNext()) {
                val address = c.getString(addressIdx) ?: ""
                val date = c.getLong(dateIdx)
                val type = c.getInt(typeIdx)
                val body = c.getString(bodyIdx) ?: ""
                val read = c.getInt(readIdx)
                val dateSent = c.getLong(dateSentIdx)
                val locked = c.getInt(lockedIdx)
                val subId = c.getInt(subIdIdx)

                val message = SmsMessage(
                    address = address,
                    date = date,
                    type = type,
                    body = body,
                    read = read,
                    dateSent = dateSent,
                    locked = locked,
                    subscriptionId = subId,
                    contactName = null // Would need a separate query to ContactsContract
                )
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
}
