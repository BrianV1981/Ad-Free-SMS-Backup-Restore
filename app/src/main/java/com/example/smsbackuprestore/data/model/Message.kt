package com.example.smsbackuprestore.data.model

sealed class Message {
    abstract val date: Long
    abstract val address: String
}

data class SmsMessage(
    override val address: String,
    override val date: Long,
    val type: Int, // 1 = Inbox, 2 = Sent
    val body: String,
    val read: Int,
    val dateSent: Long,
    val locked: Int,
    val subscriptionId: Int = -1,
    val contactName: String? = null
) : Message()

data class MmsMessage(
    override val address: String,
    override val date: Long,
    val msgBox: Int, // 1 = Inbox, 2 = Sent
    val read: Int,
    val dateSent: Long,
    val locked: Int,
    val subscriptionId: Int = -1,
    val textBody: String? = null,
    val parts: List<MmsPart> = emptyList()
) : Message()

data class MmsPart(
    val partId: String,
    val contentType: String,
    val name: String?,
    val text: String?,
    val dataUri: String? // URI to the local content for extraction
)
