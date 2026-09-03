# Project Gameplan: Ad-Free SMS Backup & Restore

## 1. Executive Summary

**Objective:** Disrupt the stagnant, ad-ridden monopoly of legacy SMS backup utilities (e.g., SyncTech) by releasing a beautifully designed, modern, 100% ad-free alternative.
**Go-to-Market Strategy:** Utilize the exact keywords ("SMS Backup & Restore") in the app title to hijack organic Google Play Store SEO traffic. Offer all legacy "Pro" features (cloud syncing, encryption) completely for free.
**Monetization:** A frictionless "Buy me a Coffee" / "Support the Developer" tipping model in the settings menu, relying on massive user volume and community goodwill.

## 2. Core Feature Set (The MVP)

To guarantee users can seamlessly transition from the legacy app, the MVP must perfectly replicate its core extraction capabilities and output format:

1. **SMS & MMS:** Full extraction of texts, group chats, pictures, and audio attachments.
2. **Call Logs:** Extraction of incoming, outgoing, and missed call history.
3. **Contacts:** Generating a `.vcf` (vCard) file of the phonebook to prevent orphaned phone numbers.
4. **Cloud Sync:** Background, scheduled uploads to Google Drive via OAuth.

## 3. The "Category Killer" Features

To instantly win over Reddit and XDA-Developers:

1. **RCS Support:** Research and implement extraction of Rich Communication Services ("blue bubble") messages, which legacy apps notoriously struggle with.
2. **AES-256 Encryption:** Allow users to password-protect their `.xml` backups for free.
3. **ZIP Compression:** Compress the heavy XML files before cloud upload to save user bandwidth.

## 4. Technical Architecture

- **Language:** 100% Kotlin.
- **UI Framework:** Jetpack Compose (Material You design language to make it feel native to Android 14/15).
- **Permissions Required:** `READ_SMS`, `WRITE_SMS`, `READ_CALL_LOG`, `WRITE_CALL_LOG`, `READ_CONTACTS`, `INTERNET`.
- **Data Extraction:** Query the `Telephony.Sms.CONTENT_URI` and `CallLog.Calls`.
