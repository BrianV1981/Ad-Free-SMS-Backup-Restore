package com.example.smsbackuprestore.data.model

data class BackupEntry(
    val fileId: String,
    val filename: String,
    val date: Long,
    val messageCount: Int
)

data class BackupManifest(
    var entries: List<BackupEntry> = emptyList()
)
