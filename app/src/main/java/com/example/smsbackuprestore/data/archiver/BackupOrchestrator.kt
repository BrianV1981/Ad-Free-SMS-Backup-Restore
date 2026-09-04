package com.example.smsbackuprestore.data.archiver

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.io.InputStream
import com.example.smsbackuprestore.data.extraction.ExtractionRepository
import com.example.smsbackuprestore.data.model.CallLogEntry
import com.example.smsbackuprestore.data.model.MmsMessage
import com.example.smsbackuprestore.data.model.SmsMessage
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupOrchestrator(
    private val contentResolver: ContentResolver,
    private val extractionRepository: ExtractionRepository,
    private val archiver: BackupArchiver
) {

    /**
     * Executes the full backup, extracting SMS, MMS, Call Logs, and Contacts,
     * serializing them to XML/VCard, and compressing them directly into the outputStream.
     * Optionally encrypts the backup with AES-256 if a password is provided.
     */
    suspend fun performBackup(
        outputStream: OutputStream, 
        password: CharArray? = null,
        includeMmsMedia: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ) {
        val totalMessages = extractionRepository.getTotalMessagesCount()
        var currentMessageCount = 0

        archiver.createArchive(outputStream, password) { zos ->
            
            val isEncrypted = password != null

            // 1. Backup SMS & MMS
            archiver.writeEntry(zos, "messages.xml", isEncrypted) { stream ->
                // Write XML Header
                stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<messages>\n".toByteArray(StandardCharsets.UTF_8))
                
                extractionRepository.extractAllMessages().collect { msg ->
                    currentMessageCount++
                    if (currentMessageCount % 50 == 0 && totalMessages > 0) {
                        onProgress(currentMessageCount.toFloat() / totalMessages.toFloat())
                    }
                    when (msg) {
                        is SmsMessage -> serializeSms(msg, stream)
                        is MmsMessage -> serializeMms(msg, stream, includeMmsMedia)
                    }
                }
                if (totalMessages > 0) {
                    onProgress(1f) // 100% when messages are done
                }
                
                stream.write("</messages>\n".toByteArray(StandardCharsets.UTF_8))
            }

            // 2. Backup Call Logs
            archiver.writeEntry(zos, "calls.xml", isEncrypted) { stream ->
                stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<calls>\n".toByteArray(StandardCharsets.UTF_8))
                
                extractionRepository.extractCallLogs().collect { call ->
                    val xmlChunk = serializeCall(call)
                    stream.write(xmlChunk.toByteArray(StandardCharsets.UTF_8))
                }
                
                stream.write("</calls>\n".toByteArray(StandardCharsets.UTF_8))
            }

            // 3. Backup Contacts (VCard)
            archiver.writeEntry(zos, "contacts.vcf", isEncrypted) { stream ->
                extractionRepository.extractContactsAsVCard().collect { vcard ->
                    stream.write(vcard.toByteArray(StandardCharsets.UTF_8))
                }
            }
        }
    }

    private fun serializeSms(sms: SmsMessage, stream: OutputStream) {
        val xml = "  <sms address=\"${escapeXml(sms.address)}\" date=\"${sms.date}\" type=\"${sms.type}\" body=\"${escapeXml(sms.body)}\" read=\"${sms.read}\" />\n"
        stream.write(xml.toByteArray(StandardCharsets.UTF_8))
    }

    private fun serializeMms(mms: MmsMessage, stream: OutputStream, includeMmsMedia: Boolean) {
        stream.write("  <mms address=\"${escapeXml(mms.address)}\" date=\"${mms.date}\" msg_box=\"${mms.msgBox}\" read=\"${mms.read}\">\n".toByteArray(StandardCharsets.UTF_8))
        stream.write("    <parts>\n".toByteArray(StandardCharsets.UTF_8))
        
        for (part in mms.parts) {
            if (part.contentType == "text/plain") {
                val textData = escapeXml(part.text ?: "")
                stream.write("      <part ct=\"${escapeXml(part.contentType)}\" name=\"${escapeXml(part.name ?: "null")}\" text=\"${textData}\" />\n".toByteArray(StandardCharsets.UTF_8))
            } else if (includeMmsMedia && part.dataUri != null) {
                // For media parts, we stream the file as Base64 to prevent OutOfMemory on huge videos/images
                stream.write("      <part ct=\"${escapeXml(part.contentType)}\" name=\"${escapeXml(part.name ?: "null")}\" data=\"".toByteArray(StandardCharsets.UTF_8))
                streamBase64Part(part.dataUri, stream)
                stream.write("\" />\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        
        stream.write("    </parts>\n".toByteArray(StandardCharsets.UTF_8))
        stream.write("  </mms>\n".toByteArray(StandardCharsets.UTF_8))
    }

    private fun streamBase64Part(dataUri: String, stream: OutputStream) {
        try {
            contentResolver.openInputStream(Uri.parse(dataUri))?.use { inputStream ->
                // Must be a multiple of 3 to avoid internal padding characters "=" breaking the continuous stream
                val buffer = ByteArray(8190)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val base64Bytes = Base64.encode(buffer, 0, bytesRead, Base64.NO_WRAP)
                    stream.write(base64Bytes)
                }
            }
        } catch (e: Exception) {
            // If the media part no longer exists on device or fails, just silently skip to prevent backup failure
            e.printStackTrace()
        }
    }

    private fun serializeCall(call: CallLogEntry): String {
        return "  <call number=\"${escapeXml(call.number)}\" date=\"${call.date}\" duration=\"${call.duration}\" type=\"${call.type}\" presentation=\"${call.presentation}\" name=\"${escapeXml(call.contactName ?: "")}\" />\n"
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
