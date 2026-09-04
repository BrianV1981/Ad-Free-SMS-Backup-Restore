package com.example.smsbackuprestore.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE) }
    
    var driveSyncEnabled by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context) != null) }
    var encryptionEnabled by remember { mutableStateOf(false) }
    var nestInFolderEnabled by remember { mutableStateOf(prefs.getBoolean("nest_in_folder", true)) }
    
    var autoBackupEnabled by remember { mutableStateOf(prefs.getBoolean("auto_backup_enabled", false)) }
    var requireWifi by remember { mutableStateOf(prefs.getBoolean("auto_backup_wifi", true)) }
    var requireCharging by remember { mutableStateOf(prefs.getBoolean("auto_backup_charging", true)) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            driveSyncEnabled = true
        } else {
            driveSyncEnabled = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Backup Options",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Google Drive Sync") },
                    supportingContent = { Text("Automatically upload backups to a secure, private folder in your Google Drive.") },
                    trailingContent = {
                        Switch(
                            checked = driveSyncEnabled,
                            onCheckedChange = { isChecked -> 
                                if (isChecked) {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } else {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                    GoogleSignIn.getClient(context, gso).signOut()
                                    driveSyncEnabled = false
                                }
                            }
                        )
                    }
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("AES-256 Encryption") },
                    supportingContent = { Text("Require a password to open your backup zip file.") },
                    trailingContent = {
                        Switch(
                            checked = encryptionEnabled,
                            onCheckedChange = { encryptionEnabled = it }
                        )
                    }
                )
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    text = "Automation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Scheduled Backups") },
                    supportingContent = { Text("Silently runs a backup in the background daily.") },
                    trailingContent = {
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { 
                                autoBackupEnabled = it
                                prefs.edit().putBoolean("auto_backup_enabled", it).apply()
                                if (it) {
                                    com.example.smsbackuprestore.worker.WorkManagerHelper.scheduleBackup(
                                        context = context,
                                        intervalDays = 1, // Daily
                                        requireWifi = requireWifi,
                                        requireCharging = requireCharging
                                    )
                                } else {
                                    com.example.smsbackuprestore.worker.WorkManagerHelper.cancelBackup(context)
                                }
                            },
                            enabled = driveSyncEnabled
                        )
                    }
                )
            }

            if (autoBackupEnabled) {
                item {
                    ListItem(
                        headlineContent = { Text("Require Wi-Fi") },
                        supportingContent = { Text("Wait for unmetered network connection") },
                        trailingContent = {
                            Switch(
                                checked = requireWifi,
                                onCheckedChange = { 
                                    requireWifi = it
                                    prefs.edit().putBoolean("auto_backup_wifi", it).apply()
                                    com.example.smsbackuprestore.worker.WorkManagerHelper.scheduleBackup(context, 1, it, requireCharging)
                                }
                            )
                        }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Require Charging") },
                        supportingContent = { Text("Wait until device is plugged in") },
                        trailingContent = {
                            Switch(
                                checked = requireCharging,
                                onCheckedChange = { 
                                    requireCharging = it
                                    prefs.edit().putBoolean("auto_backup_charging", it).apply()
                                    com.example.smsbackuprestore.worker.WorkManagerHelper.scheduleBackup(context, 1, requireWifi, it)
                                }
                            )
                        }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    text = "Developer Settings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Nest in Drive Folder") },
                    supportingContent = { Text("Organize backups inside a dedicated 'Ad-Free SMS Backups' folder on Google Drive.") },
                    trailingContent = {
                        Switch(
                            checked = nestInFolderEnabled,
                            onCheckedChange = { 
                                nestInFolderEnabled = it
                                prefs.edit().putBoolean("nest_in_folder", it).apply()
                            },
                            enabled = driveSyncEnabled
                        )
                    }
                )
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Buy Me a Coffee ☕") },
                    supportingContent = { Text("This app is 100% free and ad-free. If it saved your data, consider dropping a tip!") },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/leaddeeds"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Open Source License") },
                    supportingContent = { Text("Licensed under GPLv3. View source code.") },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BrianV1981/Ad-Free-SMS-Backup-Restore"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
