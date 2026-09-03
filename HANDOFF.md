# Ad-Free SMS Backup & Restore — Engineering Handoff

> **Updated:** 2026-09-03
> **Updated by:** Antigravity (1a024aa9-bd63-4b78-8d6b-ad43bdcbb587)
> **Priority Mission:** Implement Google Drive Cloud Sync (OAuth Setup Required)
> **Operator:** Brian

---

## 0. COMPLETED WORK (DO NOT REVISIT)
| Session | Work | Status |
|---------|------|--------|
| fd81a7e3 | Initialized local git repo, GitHub repo, Roadmap, License, Wiki | ✅ RESOLVED |
| 1a024aa9 | Issue #8: Android Project Scaffolding | ✅ RESOLVED |
| 1a024aa9 | Issue #1: MVP SMS & MMS Extraction Engine (Telephony ContentResolver) | ✅ RESOLVED |
| 1a024aa9 | Issue #2: MVP Call Log Extraction (CallLog.Calls) | ✅ RESOLVED |
| 1a024aa9 | Issue #3: MVP Contacts vCard Extraction (MULTI_VCARD_URI) | ✅ RESOLVED |
| 1a024aa9 | Issue #5: In-memory streaming XML serialization and ZIP compression | ✅ RESOLVED |

---

## 1. PROJECT IDENTITY
This is a native Android 14/15 application built to disrupt legacy SMS backup utilities. The project is strictly 100% Kotlin utilizing Jetpack Compose and Material You. The core monetization strategy relies purely on community goodwill via tipping. No ads, no telemetry.

### Your Knowledge Base
- [Project Wiki Index](c:\Users\kingb\Ad-Free SMS Backup & Restore\memory-wiki\index.md)
- [App Roadmap](c:\Users\kingb\Ad-Free SMS Backup & Restore\docs\sms_app_roadmap.md)
- [J.O.S.H.U.A. Rules / GEMINI.md](c:\Users\kingb\Ad-Free SMS Backup & Restore\GEMINI.md)

---

## 2. YOUR MISSION: ISSUE #4 (GOOGLE DRIVE CLOUD SYNC)
The core extraction, serialization, and compression engines are completely built and proven locally. The incoming session must hook this data pipeline up to Google Drive via OAuth (`drive.file` scope). However, **this requires the Operator to provision Google Cloud Console credentials**.

### Execution Queue (in order)
#### 1️⃣ Obtain Google Cloud Credentials
**Problem:** We cannot execute an OAuth flow on Android without a registered Client ID.
**Fix:** The Operator must generate a Google Cloud Console OAuth 2.0 Client ID for Android, providing the package name (`com.example.smsbackuprestore`) and SHA-1 fingerprint.
**Key files:** `UNKNOWN` (likely `secrets.properties` or `google-services.json` once provided).

#### 2️⃣ Implement Cloud Sync Engine
**Problem:** The `BackupOrchestrator` outputs a compressed stream locally, but we need it shipped to the cloud in the background.
**Fix:** Use the Google Drive API for Android to upload the generated ZIP.

---

## 3. DETAILED ANALYSIS / BREAKDOWN
- **Extraction Pipeline:** The local pipeline is flawlessly memory-efficient. `BackupOrchestrator.performBackup(outputStream)` extracts cursors and pipes them through a `ZipOutputStream` on-the-fly.
- **The OAuth Blocker:** Do not attempt to guess or hallucinate an OAuth Client ID. Wait for the Operator to provide the necessary Google Cloud configurations.

---

## 4. IMPLEMENTATION STRATEGY
1. Await Operator configuration of Google Cloud credentials.
2. Claim Issue #4 on the Kanban board (Project #12).
3. Spawn `feat/issue-4-gdrive` via GitOps worktree.
4. Integrate Google Auth library and Google Drive SDK.
5. Create a `DriveSyncEngine.kt` that accepts the stream from `BackupOrchestrator`.

---

## 5. THE CRITICAL TRAPS & WARNINGS
> **⚠️ EPISTEMIC / OPERATIONAL WARNINGS**
> - **Never code on `master`:** Zero-exemption mandate. You must branch out using `git worktree`.
> - **TDD Mandate:** Ensure you can test the Drive upload. You might need to mock the Drive API for unit tests.
> - **The YOLO Restraint Mandate:** If the Operator asks questions about OAuth, answer them and stop. Do not modify files unprompted.

---

## 6. KEY PATHS
- **Root Directory:** `c:\Users\kingb\Ad-Free SMS Backup & Restore`
- **Archiver Logic:** `c:\Users\kingb\Ad-Free SMS Backup & Restore\app\src\main\java\com\example\smsbackuprestore\data\archiver\BackupOrchestrator.kt`
- **Extraction Logic:** `c:\Users\kingb\Ad-Free SMS Backup & Restore\app\src\main\java\com\example\smsbackuprestore\data\extraction\`

---

## 7. THE FULL PICTURE / WHAT COMES AFTER
Once Cloud Sync (Issue #4) is implemented, the data export pipeline is feature-complete. The next phase will be **Issue #6 (AES-256 Encryption)** to password-protect the backups, followed by **UI/UX Design** for the Compose front-end.

---

## 8. OPERATOR PREFERENCES
- Professional, direct, empirical.
- Write tests before or alongside implementation.
- Execute all code inside a physically isolated `workspace/` git worktree. No exceptions.

---

## 9. IMMEDIATE NEXT STEPS
1. Wait for the Operator to provide the Google Cloud Console OAuth 2.0 Client ID for the Android app.
2. Read the `BackupOrchestrator.kt` file to understand how to pipe the output into the Google Drive API.
3. Spawn the `feat/issue-4-gdrive` worktree and begin implementation.
