package com.example.smsbackuprestore.ui

import android.os.Bundle
import androidx.activity.ComponentActivity

class DummyComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // Instantly close, we are just a dummy
    }
}
