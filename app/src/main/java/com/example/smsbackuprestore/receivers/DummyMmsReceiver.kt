package com.example.smsbackuprestore.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DummyMmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Dummy receiver required by Android to hold ROLE_SMS
    }
}
