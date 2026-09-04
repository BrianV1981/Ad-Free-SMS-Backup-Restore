# Default SMS App & Restricted Settings

On modern Android devices (Android 13+), restoring SMS messages is protected by rigorous security rules. To write directly to the content://sms provider, an app must be temporarily granted the Default SMS App role via RoleManager.ROLE_SMS.

## The AndroidManifest.xml Requirements
Android's RoleManager will instantly and silently reject any request for ROLE_SMS if the app does not declare the full suite of messaging intents and permissions in its manifest. Even if the app is purely a backup/restore tool and has no intention of being a daily driver messenger, it must still provide "Dummy" components:

### Required Permissions:
`xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />
<uses-permission android:name="android.permission.RECEIVE_WAP_PUSH" />
`

### Required Dummy Components:
1. An exported Activity listening for ACTION_SENDTO (sms:, smsto:, mms:, mmsto:).
2. A BroadcastReceiver listening for ndroid.provider.Telephony.SMS_DELIVER with ndroid.permission.BROADCAST_SMS.
3. A BroadcastReceiver listening for ndroid.provider.Telephony.WAP_PUSH_DELIVER with ndroid.permission.BROADCAST_WAP_PUSH.
4. A Service listening for ndroid.intent.action.RESPOND_VIA_MESSAGE with ndroid.permission.SEND_RESPOND_VIA_MESSAGE.

## The Android 13+ "Restricted Settings" Sideloading Block
If the app is installed via a non-session installer (e.g., downloading an APK directly or installing via a file manager), Android 13+ will flag the app as **sideloaded**. 

Sideloaded apps are subject to **Restricted Settings**, which explicitly blocks them from accessing sensitive permissions such as Accessibility, Notification Access, and Default SMS status. If a user triggers the RoleManager intent while restricted, the dialog will simply fail to appear.

### The Bypass (For Development/Sideloading)
To successfully test the Restore functionality during development, the user must explicitly lift the restriction manually:
1. Open the phone's **Settings** app.
2. Go to **Apps -> See all apps**.
3. Select **Ad-Free SMS Backup**.
4. In the top right corner, tap the three vertical dots (?).
5. Select **Allow restricted settings** (requires PIN/Fingerprint authentication).

Once allowed, RoleManager will successfully prompt the user for the Default SMS role. 

*Note: This issue completely disappears once the application is officially published and installed via the Google Play Store, as it utilizes a trusted, session-based installer API.*
