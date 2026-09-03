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
