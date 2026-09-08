package com.example.smsbackuprestore.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)

    var driveSyncEnabled by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context) != null) }
    var folderName by remember { mutableStateOf(prefs.getString("drive_folder_name", "Ad-Free SMS Backups") ?: "Ad-Free SMS Backups") }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf(prefs.getString("encryption_password", "") ?: "") }
    
    var encryptionEnabled by remember { mutableStateOf(prefs.getBoolean("encryption_enabled", false)) }
    var includeMmsMedia by remember { mutableStateOf(prefs.getBoolean("include_mms_media", false)) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    
    var autoBackupEnabled by remember { mutableStateOf(prefs.getBoolean("auto_backup_enabled", false)) }
    var requireWifi by remember { mutableStateOf(prefs.getBoolean("auto_backup_wifi", true)) }
    var requireCharging by remember { mutableStateOf(prefs.getBoolean("auto_backup_charging", false)) }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(ApiException::class.java)
            driveSyncEnabled = true
        } catch (e: ApiException) {
            driveSyncEnabled = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            // Category: Backup Security
            item {
                Text(
                    text = "Backup Security",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("AES-256 Encryption") },
                    supportingContent = { Text("Require a password to open your backup zip file.") },
                    trailingContent = {
                        Switch(
                            checked = encryptionEnabled,
                            onCheckedChange = { 
                                if (it) {
                                    showPasswordDialog = true
                                } else {
                                    encryptionEnabled = false
                                    prefs.edit().putBoolean("encryption_enabled", false).apply()
                                }
                            }
                        )
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: Sovereign Sync (AIM-Connect)
            item {
                Text(
                    text = "Sovereign Sync (AIM-Connect)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("What is AIM-Connect?") },
                    supportingContent = { Text("AIM-Connect is an open-source, local terminal multiplexer and backend server. By entering your server URL and Webhook Secret below, this app will bypass Google Drive entirely and pipe your encrypted backups directly to your own self-hosted server for true data sovereignty.") }
                )
            }
            item {
                var aimConnectUrl by remember { mutableStateOf(prefs.getString("aim_connect_url", "") ?: "") }
                OutlinedTextField(
                    value = aimConnectUrl,
                    onValueChange = { aimConnectUrl = it; prefs.edit().putString("aim_connect_url", it).apply() },
                    label = { Text("AIM-Connect Server URL") },
                    placeholder = { Text("e.g. http://192.168.1.100:8000") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                var aimConnectToken by remember { mutableStateOf(prefs.getString("aim_connect_token", "") ?: "") }
                OutlinedTextField(
                    value = aimConnectToken,
                    onValueChange = { aimConnectToken = it; prefs.edit().putString("aim_connect_token", it).apply() },
                    label = { Text("Aim-Connect-Webhook-Secret") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: Cloud & Storage
            item {
                Text(
                    text = "Cloud & Storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Google Drive Sync") },
                    supportingContent = { Text("Automatically upload backups to your Google Drive.") },
                    trailingContent = {
                        Switch(
                            checked = driveSyncEnabled,
                            onCheckedChange = { 
                                if (it) {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } else {
                                    googleSignInClient.signOut()
                                    driveSyncEnabled = false
                                }
                            }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Drive Folder Name") },
                    supportingContent = { Text(folderName) },
                    modifier = Modifier.clickable(enabled = driveSyncEnabled) {
                        showFolderDialog = true
                    }
                )
            }


            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: Backup Preferences
            item {
                Text(
                    text = "Backup Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Include MMS Media") },
                    supportingContent = { Text("Backup pictures and videos. Dramatically increases backup size.") },
                    trailingContent = {
                        Switch(
                            checked = includeMmsMedia,
                            onCheckedChange = { 
                                includeMmsMedia = it
                                prefs.edit().putBoolean("include_mms_media", it).apply()
                            }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: Automation
            item {
                Text(
                    text = "Automation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
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
                                        context, 1, requireWifi, requireCharging
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
                        supportingContent = { Text("Wait for unmetered network connection.") },
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
                        supportingContent = { Text("Wait until device is plugged in.") },
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

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: Security (Placeholder for Biometrics)
            item {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Biometric App-Lock") },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { 
                                biometricEnabled = it
                                prefs.edit().putBoolean("biometric_enabled", it).apply()
                            }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Category: About & Support
            item {
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Buy Me a Coffee ☕") },
                    supportingContent = { Text("This app is 100% free and ad-free. If it saved your data, consider dropping a tip!") },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/brianv1981")))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Open Source License") },
                    supportingContent = { Text("Licensed under GPLv3. View source code.") },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BrianV1981/Ad-Free-SMS-Backup-Restore")))
                    }
                )
            }
        }
    }

    
    if (showPasswordDialog) {
        var tempPassword by remember { mutableStateOf(currentPassword) }
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false 
                if (currentPassword.isBlank()) encryptionEnabled = false
            },
            title = { Text("Set Encryption Password") },
            text = {
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    singleLine = true,
                    label = { Text("Password") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempPassword.isNotBlank()) {
                        currentPassword = tempPassword
                        encryptionEnabled = true
                        prefs.edit().putBoolean("encryption_enabled", true).apply()
                        prefs.edit().putString("encryption_password", tempPassword).apply()
                    } else {
                        encryptionEnabled = false
                        prefs.edit().putBoolean("encryption_enabled", false).apply()
                    }
                    showPasswordDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showPasswordDialog = false 
                    if (currentPassword.isBlank()) encryptionEnabled = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFolderDialog) {
        var tempName by remember { mutableStateOf(folderName) }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Set Drive Folder Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Folder Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) {
                        folderName = tempName
                        prefs.edit().putString("drive_folder_name", tempName).apply()
                    }
                    showFolderDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
