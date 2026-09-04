package com.example.smsbackuprestore

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.*
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.smsbackuprestore.ui.theme.AdFreeSmsBackupTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {

      val prefs = getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
      val requiresBiometric = prefs.getBoolean("biometric_enabled", false)
      
      AdFreeSmsBackupTheme { 
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
              if (requiresBiometric) {
                  BiometricLockScreen(
                      onUnlocked = { MainNavigation() }
                  )
              } else {
                  MainNavigation() 
              }
          } 
      }

    }
  }
}


@Composable
fun BiometricLockScreen(onUnlocked: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity, 
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    hasError = true
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    hasError = true
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Ad-Free SMS Backup")
            .setSubtitle("Authenticate to access your private backups")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    if (isUnlocked) {
        onUnlocked()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Button(onClick = { 
                    // Retry logic could go here, or just let them tap to retry
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(
                        context as FragmentActivity, 
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isUnlocked = true
                            }
                        }
                    )
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Ad-Free SMS Backup")
                        .setNegativeButtonText("Cancel")
                        .build()
                    biometricPrompt.authenticate(promptInfo)
                }) {
                    Text("Tap to Unlock")
                }
            } else {
                Text("Waiting for authentication...")
            }
        }
    }
}
