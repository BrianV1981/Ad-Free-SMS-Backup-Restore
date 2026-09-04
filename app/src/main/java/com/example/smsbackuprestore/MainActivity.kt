package com.example.smsbackuprestore

import android.os.Bundle
import android.content.ContextWrapper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import android.util.Log
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




fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun BiometricLockScreen(onUnlocked: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findFragmentActivity()
    var isUnlocked by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authenticate = {
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity, 
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        hasError = true
                        errorMessage = errString.toString()
                        Log.e("Biometric", "Auth Error: $errorCode - $errString")
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isUnlocked = true
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        hasError = true
                        errorMessage = "Authentication failed. Try again."
                        Log.e("Biometric", "Auth Failed")
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Ad-Free SMS Backup")
                .setSubtitle("Authenticate to access your private backups")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            hasError = true
            errorMessage = "Activity context not found."
        }
    }

    LaunchedEffect(Unit) {
        authenticate()
    }

    if (isUnlocked) {
        onUnlocked()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        hasError = false
                        authenticate() 
                    }) {
                        Text("Tap to Unlock")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}