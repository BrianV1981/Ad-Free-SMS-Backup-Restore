import sys
import re

with open(r'app\src\main\java\com\example\smsbackuprestore\ui\screens\DashboardScreen.kt', 'r') as f:
    content = f.read()

# Add missing imports for Image
content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.Image\nimport androidx.compose.ui.res.painterResource\nimport com.example.smsbackuprestore.R\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.foundation.shape.RoundedCornerShape')

# Replace the layout from Column to the end of Column
new_layout = '''
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
'''

content = re.sub(r'Column\(\s*modifier = Modifier\s*\.fillMaxSize\(\)\s*\.padding\(innerPadding\)\s*\.padding\(24\.dp\),[\s\S]*?Spacer\(modifier = Modifier\.height\(32\.dp\)\)\s*\}', new_layout.strip(), content)

# Remove HeroStatusIndicator
content = re.sub(r'@Composable\s*fun HeroStatusIndicator\([\s\S]*', '', content)

with open(r'app\src\main\java\com\example\smsbackuprestore\ui\screens\DashboardScreen.kt', 'w') as f:
    f.write(content)

print('Updated DashboardScreen.kt')
