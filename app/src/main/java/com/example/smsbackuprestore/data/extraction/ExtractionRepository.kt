package com.example.smsbackuprestore.data.extraction

import com.example.smsbackuprestore.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class ExtractionRepository(
    private val smsEngine: SmsExtractionEngine,
    private val mmsEngine: MmsExtractionEngine
) {

    /**
     * Returns a combined flow of SMS and MMS messages.
     * Since these are backed by local database cursors, the flow will emit
     * as quickly as the cursor can read.
     */
    fun extractAllMessages(): Flow<Message> {
        return merge(smsEngine.extractSms(), mmsEngine.extractMms())
    }
}
