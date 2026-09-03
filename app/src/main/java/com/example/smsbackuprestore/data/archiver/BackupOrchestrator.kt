package com.example.smsbackuprestore.data.archiver

import com.example.smsbackuprestore.data.extraction.ExtractionRepository
import com.example.smsbackuprestore.data.model.CallLogEntry
import com.example.smsbackuprestore.data.model.MmsMessage
import com.example.smsbackuprestore.data.model.SmsMessage
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupOrchestrator(
    private val extractionRepository: ExtractionRepository,
    private val archiver: BackupArchiver
) {

    /**
     * Executes the full backup, extracting SMS, MMS, Call Logs, and Contacts,
     * serializing them to XML/VCard, and compressing them directly into the outputStream.
     * Optionally encrypts the backup with AES-256 if a password is provided.
     */
    suspend fun performBackup(outputStream: OutputStream, password: CharArray? = null) {
        archiver.createArchive(outputStream, password) { zos ->
            
            val isEncrypted = password != null

            // 1. Backup SMS & MMS
            archiver.writeEntry(zos, "messages.xml", isEncrypted) { stream ->
                // Write XML Header
                stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<messages>\n".toByteArray(StandardCharsets.UTF_8))
                
                extractionRepository.extractAllMessages().collect { msg ->
                    val xmlChunk = when (msg) {
                        is SmsMessage -> serializeSms(msg)
                        is MmsMessage -> serializeMms(msg)
                    }
                    stream.write(xmlChunk.toByteArray(StandardCharsets.UTF_8))
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

    private fun serializeSms(sms: SmsMessage): String {
        // Simplified XML serialization for MVP. In production, use XmlSerializer to escape entities properly.
        return "  <sms address=\"${escapeXml(sms.address)}\" date=\"${sms.date}\" type=\"${sms.type}\" body=\"${escapeXml(sms.body)}\" read=\"${sms.read}\" />\n"
    }

    private fun serializeMms(mms: MmsMessage): String {
        return "  <mms address=\"${escapeXml(mms.address)}\" date=\"${mms.date}\" msg_box=\"${mms.msgBox}\" read=\"${mms.read}\">\n" +
               "    <text>${escapeXml(mms.textBody ?: "")}</text>\n" +
               "  </mms>\n"
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
