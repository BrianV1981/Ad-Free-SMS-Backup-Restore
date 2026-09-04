package com.example.smsbackuprestore.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.smsbackuprestore.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsbackuprestore.ui.viewmodel.BackupState
import com.example.smsbackuprestore.ui.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val backupState by viewModel.backupState.collectAsState()
    val context = LocalContext.current
    
    val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startBackup(context.cacheDir)
        } else {
            // Handle denied permission visually (TODO: show snackbar)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad-Free SMS Backup") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Hero Image Logo
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(160.dp)
                    .padding(top = 32.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Status Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when(backupState) {
                            is BackupState.Idle -> "System Ready"
                            is BackupState.BackingUp -> "Backing Up..."
                            is BackupState.Success -> "Backup Safe"
                            is BackupState.Error -> "Backup Failed"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when(backupState) {
                            is BackupState.Idle -> "Your messages and call logs are ready to be backed up to the cloud."
                            is BackupState.BackingUp -> "Encrypting and syncing your data... Please wait."
                            is BackupState.Success -> "Backup completed successfully and safely synced."
                            is BackupState.Error -> "An error occurred during backup."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (backupState is BackupState.BackingUp) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { (backupState as BackupState.BackingUp).progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Primary Actions
            Button(
                onClick = { 
                    val hasPermissions = requiredPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPermissions) {
                        viewModel.startBackup(context.cacheDir) 
                    } else {
                        permissionLauncher.launch(requiredPermissions)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = backupState !is BackupState.BackingUp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Backup Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { viewModel.fetchRestoreOptions() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = backupState !is BackupState.BackingUp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Restore Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    val restoreState by viewModel.restoreState.collectAsState()
    
    if (restoreState !is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Idle) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.resetRestoreState() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Restore Backup", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                when (restoreState) {
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Loading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching backup history from Google Drive...")
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Downloading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Downloading backup from Google Drive...")
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Extracting -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Decrypting and unzipping archive...")
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Parsing -> {
                        val parsingState = restoreState as com.example.smsbackuprestore.ui.viewmodel.RestoreState.Parsing
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Parsing XML: ${parsingState.smsCount} SMS, ${parsingState.mmsCount} MMS")
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Success -> {
                        val successState = restoreState as com.example.smsbackuprestore.ui.viewmodel.RestoreState.Success
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Success", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Dry Run Complete!", style = MaterialTheme.typography.titleLarge)
                        Text("Successfully parsed ${successState.smsCount} SMS and ${successState.mmsCount} MMS messages.")
                        Text("0 messages were actually written to your device.", color = MaterialTheme.colorScheme.secondary)
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.SuccessReal -> {
                        val successState = restoreState as com.example.smsbackuprestore.ui.viewmodel.RestoreState.SuccessReal
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Success", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Restore Complete!", style = MaterialTheme.typography.titleLarge)
                        Text("Successfully restored ${successState.smsCount} SMS and ${successState.mmsCount} MMS messages.")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text("Restore Original Default SMS App")
                        }
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Error -> {
                        val msg = (restoreState as com.example.smsbackuprestore.ui.viewmodel.RestoreState.Error).message
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    is com.example.smsbackuprestore.ui.viewmodel.RestoreState.Options -> {
                        val manifest = (restoreState as com.example.smsbackuprestore.ui.viewmodel.RestoreState.Options).manifest
                        
                        var selectedEntry by remember { mutableStateOf<com.example.smsbackuprestore.data.model.BackupEntry?>(null) }
                        
                        val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as? android.app.role.RoleManager
                        val defaultSmsLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == android.app.Activity.RESULT_OK) {
                                selectedEntry?.let { viewModel.startRealRestore(it) }
                            } else {
                                viewModel.setRestoreError("Permission to become Default SMS App was denied.")
                            }
                        }

                        if (selectedEntry != null) {
                            AlertDialog(
                                onDismissRequest = { selectedEntry = null },
                                title = { Text("Choose Restore Mode") },
                                text = { Text("Dry-Run will safely test the backup archive without modifying your device.\n\nReal Restore will securely request to become your Default SMS App, restore the messages, and hand control back to your original messenger.") },
                                confirmButton = {
                                    Button(onClick = {
                                        val entry = selectedEntry!!
                                        selectedEntry = null
                                        
                                        // Check if we are already default
                                        if (roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_SMS) == true) {
                                            viewModel.startRealRestore(entry)
                                        } else {
                                            // Request role
                                            val intent = roleManager?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
                                            if (intent != null) {
                                                defaultSmsLauncher.launch(intent)
                                            } else {
                                                viewModel.setRestoreError("RoleManager is unavailable on this device.")
                                            }
                                        }
                                    }) {
                                        Text("Real Restore")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = {
                                        val entry = selectedEntry!!
                                        selectedEntry = null
                                        viewModel.startRestoreDryRun(entry)
                                    }) {
                                        Text("Dry-Run Test")
                                    }
                                }
                            )
                        }

                        androidx.compose.foundation.lazy.LazyColumn {
                            items(manifest.entries.size) { index ->
                                // Reverse order for newest first
                                val entry = manifest.entries.reversed()[index]
                                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
                                
                                ListItem(
                                    headlineContent = { Text(dateStr) },
                                    supportingContent = { Text("${entry.messageCount} messages") },
                                    modifier = Modifier.clickable {
                                        selectedEntry = entry
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                    else -> {}
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

