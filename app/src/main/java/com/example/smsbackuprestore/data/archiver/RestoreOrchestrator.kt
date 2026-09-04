package com.example.smsbackuprestore.data.archiver

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

class RestoreOrchestrator {

    /**
     * Parses the messages.xml file using XmlPullParser in a memory-efficient stream.
     * In Dry-Run mode, this purely counts the messages and yields progress.
     */
    suspend fun parseMessagesXmlDryRun(
        xmlFile: File,
        onProgress: (Int, Int) -> Unit // smsCount, mmsCount
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var smsCount = 0
        var mmsCount = 0
        
        if (!xmlFile.exists()) return@withContext Pair(0, 0)
        
        FileInputStream(xmlFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "sms" -> {
                            smsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                        }
                        "mms" -> {
                            mmsCount++
                            if ((smsCount + mmsCount) % 100 == 0) {
                                onProgress(smsCount, mmsCount)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        
        // Final progress update
        onProgress(smsCount, mmsCount)
        
        Pair(smsCount, mmsCount)
    }
}
