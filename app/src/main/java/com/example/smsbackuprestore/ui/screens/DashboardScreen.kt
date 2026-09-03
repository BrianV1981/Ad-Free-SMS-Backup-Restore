package com.example.smsbackuprestore.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Hero Section
            HeroStatusIndicator(backupState = backupState)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = when(backupState) {
                    is BackupState.Idle -> "Your messages are ready to be backed up."
                    is BackupState.BackingUp -> "Backing up your data... Please wait."
                    is BackupState.Success -> "Backup completed successfully!"
                    is BackupState.Error -> "An error occurred during backup."
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Primary Actions
            FilledTonalButton(
                onClick = { 
                    // In a real app we'd prompt for directory, for now just use cache dir
                    viewModel.startBackup(context.cacheDir) 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = backupState !is BackupState.BackingUp
            ) {
                Text("Backup Now", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { /* TODO: Restore */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = backupState !is BackupState.BackingUp
            ) {
                Text("Restore", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeroStatusIndicator(backupState: BackupState) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        if (backupState is BackupState.BackingUp) {
            val infiniteTransition = rememberInfiniteTransition()
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing)
                )
            )
            
            CircularProgressIndicator(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer { rotationZ = angle },
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Static representation for Idle/Success
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(150.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (backupState is BackupState.Success) "SAFE" else "READY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
