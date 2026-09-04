import sys

with open(r'app\src\main\java\com\example\smsbackuprestore\MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun BiometricLockScreen(onUnlocked: () -> Unit)',
    'fun BiometricLockScreen(onUnlocked: @Composable () -> Unit)'
)

with open(r'app\src\main\java\com\example\smsbackuprestore\MainActivity.kt', 'w') as f:
    f.write(content)

print('Fixed Composable signature.')
