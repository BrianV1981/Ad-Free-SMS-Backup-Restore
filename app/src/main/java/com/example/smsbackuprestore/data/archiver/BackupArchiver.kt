package com.example.smsbackuprestore.data.archiver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiver {

    /**
     * Initializes a zip stream and executes the block, providing the ZipOutputStream.
     * This is designed to stream data directly into the zip file (e.g. via XML serialization)
     * without writing intermediate massive files to disk.
     */
    suspend fun createArchive(
        outputStream: OutputStream,
        block: suspend (ZipOutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            ZipOutputStream(outputStream).use { zos ->
                // Use default DEFLATED compression method
                zos.setMethod(ZipOutputStream.DEFLATED)
                // Set compression level to max for best bandwidth savings
                zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
                block(zos)
            }
        }
    }

    /**
     * Helper to write a specific named entry into the zip archive.
     */
    suspend fun writeEntry(
        zos: ZipOutputStream,
        filename: String,
        writeAction: suspend (OutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val entry = ZipEntry(filename)
            zos.putNextEntry(entry)
            
            try {
                // The writeAction is responsible for writing bytes to the stream.
                // It must NOT close the stream, as that would close the entire ZipOutputStream.
                writeAction(zos)
            } finally {
                zos.closeEntry()
            }
        }
    }
}
