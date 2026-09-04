package com.example.smsbackuprestore.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DummySmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
