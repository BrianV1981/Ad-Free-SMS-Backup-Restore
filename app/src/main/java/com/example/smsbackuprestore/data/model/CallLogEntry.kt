package com.example.smsbackuprestore.data.model

data class CallLogEntry(
    val number: String,
    val date: Long,
    val duration: Long, // Duration in seconds
    val type: Int, // 1 = Incoming, 2 = Outgoing, 3 = Missed, 4 = Voicemail, 5 = Rejected, 6 = Blocked
    val presentation: Int, // 1 = Allowed, 2 = Restricted, 3 = Unknown, 4 = Payphone
    val subscriptionId: Int = -1,
    val contactName: String? = null
)
