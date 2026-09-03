package com.example.smsbackuprestore.data.archiver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.OutputStream

class BackupArchiver {

    /**
     * Initializes a zip stream and executes the block, providing the ZipOutputStream.
     * Uses Zip4j to support optional AES-256 encryption.
     */
    suspend fun createArchive(
        outputStream: OutputStream,
        password: CharArray? = null,
        block: suspend (ZipOutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val zos = if (password != null) {
                ZipOutputStream(outputStream, password)
            } else {
                ZipOutputStream(outputStream)
            }
            
            zos.use {
                block(it)
            }
        }
    }

    /**
     * Helper to write a specific named entry into the zip archive.
     */
    suspend fun writeEntry(
        zos: ZipOutputStream,
        filename: String,
        isEncrypted: Boolean = false,
        writeAction: suspend (OutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val zipParameters = ZipParameters().apply {
                fileNameInZip = filename
                compressionMethod = CompressionMethod.DEFLATE
                if (isEncrypted) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
            
            zos.putNextEntry(zipParameters)
            try {
                writeAction(zos)
            } finally {
                zos.closeEntry()
            }
        }
    }
}
