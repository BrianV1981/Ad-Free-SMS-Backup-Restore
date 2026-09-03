# Strategy & Licensing

**Date:** 2026-09-03

## The Dual-License Strategy

The project operates under a strict bifurcation of licensing based on the target audience and usage context, specifically designed to protect the consumer app while fueling the open-source A.I.M. ecosystem.

### 1. Consumer Android App (GPLv3)
The core Ad-Free SMS Backup & Restore application intended for the Google Play Store is licensed under **GPLv3**.
*   **Rationale:** The app directly targets legacy, ad-ridden competitors (e.g., SyncTech). Using a permissive license like MIT would allow these competitors to legally fork the codebase, inject their own proprietary tracking SDKs and AdMob, and release it as a closed-source competitor. 
*   **Protection:** GPLv3 (Copyleft) acts as a legal poison pill. Any entity modifying and distributing this codebase must also release their modified version under GPLv3, preventing closed-source proprietary clones.

### 2. A.I.M. Ecosystem Infrastructure (MIT)
Any standalone modules, CLI tools, or extraction engines (e.g., `aim-sms-backup`) built as part of this project that are meant for developer use are licensed under **MIT**.
*   **Rationale:** This aligns with the "Own your stack" philosophy of the A.I.M. ecosystem. It encourages other developers to study, fork, and integrate the core intelligence without legal friction.
