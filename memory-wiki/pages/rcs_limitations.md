# RCS (Rich Communication Services) Architecture & Limitations

## The "Blue Bubble" Problem
Google does not provide a public Android API or a standard `ContentProvider` for RCS, contrary to how legacy SMS/MMS operates. It is completely locked down and walled off to maintain security and control over the ecosystem.

## Storage: The `bugle_db` Database
Google Messages handles the vast majority of RCS traffic. It stores all of its E2EE (End-to-End Encrypted) RCS data, typing indicators, and read receipts in a private SQLite database called `bugle_db`.
* **Path:** `/data/data/com.google.android.apps.messaging/databases/bugle_db`
* **Access:** Due to Android's application sandboxing, this file is completely inaccessible to third-party apps without Root access (e.g., Magisk).

## The `content://mms` Fallback
Historically, Google Messages would quietly mirror standard RCS text payloads into the public `content://mms` database to maintain system compatibility. 
* **The Good News:** If a user's phone still does this mirroring, our app's `MmsExtractionEngine` automatically backs them up because it queries the entire `content://mms` provider.
* **The Bad News:** In recent Android 14 updates, Google has stopped mirroring E2EE RCS messages to the public MMS provider. Those messages simply do not exist outside of `bugle_db`.

## Conclusion
A flawless, native third-party RCS backup app is impossible by OS design without root access. The project relies on the OS mirroring legacy payloads to the standard `content://mms` database for any RCS coverage.
