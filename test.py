import sys

with open(r'app\src\main\java\com\example\smsbackuprestore\data\archiver\RestoreOrchestrator.kt', 'r') as f:
    content = f.read()

new_method = """
    suspend fun parseAndRestoreMessages(
        contentResolver: android.content.ContentResolver,
        xmlFile: File,
        onProgress: (Int, Int) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var smsCount = 0
        var mmsCount = 0
        
        if (!xmlFile.exists()) return@withContext Pair(0, 0)

        // 1. Pre-load existing SMS hashes for duplicate detection
        val existingSmsHashes = hashSetOf<String>()
        try {
            contentResolver.query(android.provider.Telephony.Sms.CONTENT_URI, arrayOf("address", "date"), null, null, null)?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val dateIndex = cursor.getColumnIndex("date")
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIndex) ?: ""
                    val date = cursor.getString(dateIndex) ?: ""
                    existingSmsHashes.add("\-\")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        FileInputStream(xmlFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "sms" -> {
                            val address = parser.getAttributeValue(null, "address") ?: ""
                            val date = parser.getAttributeValue(null, "date") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: "1"
                            val body = parser.getAttributeValue(null, "body") ?: ""
                            val read = parser.getAttributeValue(null, "read") ?: "1"
                            
                            val hash = "\-\"
                            if (!existingSmsHashes.contains(hash)) {
                                val values = android.content.ContentValues().apply {
                                    put(android.provider.Telephony.Sms.ADDRESS, address)
                                    put(android.provider.Telephony.Sms.DATE, date)
                                    put(android.provider.Telephony.Sms.TYPE, type)
                                    put(android.provider.Telephony.Sms.BODY, body)
                                    put(android.provider.Telephony.Sms.READ, read)
                                }
                                try {
                                    contentResolver.insert(android.provider.Telephony.Sms.CONTENT_URI, values)
                                    existingSmsHashes.add(hash)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
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
        
        onProgress(smsCount, mmsCount)
        Pair(smsCount, mmsCount)
    }
"""

content = content.replace("}", new_method + "\n}")

with open(r'app\src\main\java\com\example\smsbackuprestore\data\archiver\RestoreOrchestrator.kt', 'w') as f:
    f.write(content)
