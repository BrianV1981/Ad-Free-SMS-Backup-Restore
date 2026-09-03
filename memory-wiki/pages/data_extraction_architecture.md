# Data Extraction & Backup Architecture

## Overview
The core MVP functionality revolves around extracting data natively from the Android OS databases, serializing it, and compressing it efficiently before upload. The architecture prioritizes memory efficiency by avoiding the creation of massive intermediate XML files on the device storage.

## The Pipeline
The data pipeline consists of three main layers:

1. **Extraction Engines (`com.example.smsbackuprestore.data.extraction`)**
   - **`SmsExtractionEngine`**: Queries `Telephony.Sms.CONTENT_URI` asynchronously using Kotlin Coroutines (`Flow`).
   - **`MmsExtractionEngine`**: Handles dual-table complexity, querying `content://mms` and extracting payload attachments from `content://mms/part`.
   - **`CallLogExtractionEngine`**: Queries `CallLog.Calls.CONTENT_URI` to extract incoming, outgoing, and missed call history.
   - **`ContactsExtractionEngine`**: Uses a modern approach, querying all `LOOKUP_KEY`s from `ContactsContract.Contacts` and pulling perfectly formatted vCard strings in batches directly via `CONTENT_MULTI_VCARD_URI`.

2. **Data Models (`com.example.smsbackuprestore.data.model`)**
   - **`Message.kt`**: A sealed class representing unified `SmsMessage` and `MmsMessage` entries.
   - **`CallLogEntry.kt`**: Data model for call history.

3. **Archiver & Orchestrator (`com.example.smsbackuprestore.data.archiver`)**
   - **`BackupArchiver.kt`**: Manages a `ZipOutputStream` using DEFLATED maximum compression.
   - **`BackupOrchestrator.kt`**: Pipes the extracted `Flow` cursors directly into the `ZipOutputStream`. It performs on-the-fly XML serialization and writes bytes directly to the compressed stream without buffering huge amounts of data in memory or on disk.

## Memory Efficiency Wins
By integrating the extraction SQLite cursors directly into a `Flow`, applying string transformations (XML serialization), and immediately passing bytes into `java.util.zip.ZipOutputStream`, we achieve a near-zero RAM overhead backup pipeline capable of handling gigabytes of SMS history on low-end devices.
