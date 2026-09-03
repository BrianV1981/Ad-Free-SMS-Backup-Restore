package com.example.smsbackuprestore.data.extraction

import com.example.smsbackuprestore.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class ExtractionRepository(
    private val smsEngine: SmsExtractionEngine,
    private val mmsEngine: MmsExtractionEngine,
    private val callLogEngine: CallLogExtractionEngine,
    private val contactsEngine: ContactsExtractionEngine
) {

    /**
     * Returns a combined flow of SMS and MMS messages.
     * Since these are backed by local database cursors, the flow will emit
     * as quickly as the cursor can read.
     */
    fun extractAllMessages(): Flow<Message> {
        return merge(smsEngine.extractSms(), mmsEngine.extractMms())
    }

    fun getTotalMessagesCount(): Int {
        return smsEngine.getCount() + mmsEngine.getCount()
    }

    /**
     * Returns a flow of all call log entries.
     */
    fun extractCallLogs() = callLogEngine.extractCallLogs()

    /**
     * Returns a flow of raw VCard strings for all contacts.
     */
    fun extractContactsAsVCard() = contactsEngine.extractContactsAsVCard()
}
