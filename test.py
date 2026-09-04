import sys

with open(r'app\src\main\java\com\example\smsbackuprestore\data\sync\DriveSyncEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val query = "mimeType = \'application/vnd.google-apps.folder\' and name = \'Ad-Free SMS Backups\' and trashed = false"',
    'val folderName = prefs.getString("drive_folder_name", "Ad-Free SMS Backups") ?: "Ad-Free SMS Backups"\n                val query = "mimeType = \'application/vnd.google-apps.folder\' and name = \'\' and trashed = false"'
)

content = content.replace(
    'name = "Ad-Free SMS Backups"',
    'name = folderName'
)

content = content.replace(
    'val folderQuery = "mimeType = \'application/vnd.google-apps.folder\' and name = \'Ad-Free SMS Backups\' and trashed = false"',
    'val prefs = context.getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)\n            val folderName = prefs.getString("drive_folder_name", "Ad-Free SMS Backups") ?: "Ad-Free SMS Backups"\n            val folderQuery = "mimeType = \'application/vnd.google-apps.folder\' and name = \'\' and trashed = false"'
)

with open(r'app\src\main\java\com\example\smsbackuprestore\data\sync\DriveSyncEngine.kt', 'w') as f:
    f.write(content)

print('Updated DriveSyncEngine.kt')
