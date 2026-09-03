package com.example.smsbackuprestore.data.extraction

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.example.smsbackuprestore.data.model.MmsMessage
import com.example.smsbackuprestore.data.model.MmsPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MmsExtractionEngine(private val contentResolver: ContentResolver) {

    fun getCount(): Int {
        var count = 0
        val cursor: Cursor? = contentResolver.query(Telephony.Mms.CONTENT_URI, arrayOf(Telephony.Mms._ID), null, null, null)
        cursor?.use { count = it.count }
        return count
    }

    fun extractMms(): Flow<MmsMessage> = flow {
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.DATE_SENT,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.READ,
            Telephony.Mms.LOCKED,
            Telephony.Mms.SUBSCRIPTION_ID
        )

        val cursor: Cursor? = contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Mms.DATE} DESC"
        )

        cursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val dateSentIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE_SENT)
            val msgBoxIdx = c.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Mms.READ)
            val lockedIdx = c.getColumnIndexOrThrow(Telephony.Mms.LOCKED)
            val subIdIdx = c.getColumnIndexOrThrow(Telephony.Mms.SUBSCRIPTION_ID)

            while (c.moveToNext()) {
                val mmsId = c.getString(idIdx)
                val date = c.getLong(dateIdx)
                // MMS dates are sometimes in seconds rather than milliseconds. We must standardize to ms.
                val normalizedDate = if (date < 10000000000L) date * 1000L else date
                
                val dateSent = c.getLong(dateSentIdx)
                val normalizedDateSent = if (dateSent > 0 && dateSent < 10000000000L) dateSent * 1000L else dateSent
                
                val msgBox = c.getInt(msgBoxIdx)
                val read = c.getInt(readIdx)
                val locked = c.getInt(lockedIdx)
                val subId = c.getInt(subIdIdx)

                // 1. Get Address
                val address = getMmsAddress(mmsId)

                // 2. Get Parts
                val parts = getMmsParts(mmsId)
                
                // 3. Extract text body if any part is text/plain
                val textBody = parts.firstOrNull { it.contentType == "text/plain" }?.text

                val message = MmsMessage(
                    address = address,
                    date = normalizedDate,
                    msgBox = msgBox,
                    read = read,
                    dateSent = normalizedDateSent,
                    locked = locked,
                    subscriptionId = subId,
                    textBody = textBody,
                    parts = parts
                )
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun getMmsAddress(mmsId: String): String {
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val projection = arrayOf("address", "type")
        var address = "Unknown"

        contentResolver.query(uri, projection, null, null, null)?.use { c ->
            val addrIdx = c.getColumnIndexOrThrow("address")
            val typeIdx = c.getColumnIndexOrThrow("type")
            
            val addresses = mutableListOf<String>()
            while (c.moveToNext()) {
                val addr = c.getString(addrIdx)
                val type = c.getInt(typeIdx)
                // Type 137 is FROM, Type 151 is TO. We generally just want to collect all non-insert-address
                if (addr != "insert-address-token") {
                    addresses.add(addr)
                }
            }
            if (addresses.isNotEmpty()) {
                address = addresses.joinToString("~")
            }
        }
        return address
    }

    private fun getMmsParts(mmsId: String): List<MmsPart> {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid = ?"
        val selectionArgs = arrayOf(mmsId)
        val parts = mutableListOf<MmsPart>()

        contentResolver.query(uri, null, selection, selectionArgs, null)?.use { c ->
            val partIdIdx = c.getColumnIndexOrThrow("_id")
            val ctIdx = c.getColumnIndexOrThrow("ct")
            val nameIdx = c.getColumnIndex("name")
            val textIdx = c.getColumnIndex("text")
            
            while (c.moveToNext()) {
                val partId = c.getString(partIdIdx)
                val contentType = c.getString(ctIdx) ?: ""
                val name = if (nameIdx != -1) c.getString(nameIdx) else null
                var text: String? = null
                
                if (contentType == "text/plain") {
                    text = if (textIdx != -1) c.getString(textIdx) else null
                    if (text == null) {
                        // Sometimes the text is stored in the part file itself, need to read it
                        text = readTextPart(partId)
                    }
                }
                
                val dataUri = "content://mms/part/$partId"
                
                parts.add(
                    MmsPart(
                        partId = partId,
                        contentType = contentType,
                        name = name,
                        text = text,
                        dataUri = dataUri
                    )
                )
            }
        }
        return parts
    }

    private fun readTextPart(partId: String): String? {
        val partUri = Uri.parse("content://mms/part/$partId")
        return try {
            contentResolver.openInputStream(partUri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
