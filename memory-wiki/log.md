# Memory Log

## [2026-09-03] ingest | Project Bootstrapping & Licensing Strategy
- Bootstrapped the initial repository, created GitHub Kanban Project (#12), and generated roadmap issues.
- Established strict licensing boundary: The consumer-facing Android app is protected under **GPLv3** to prevent corporate clone-and-monetize theft.
- Developer infrastructure and standalone extraction engines integrated into the broader A.I.M. ecosystem will be licensed under **MIT** ("Own your stack").
- Enabled the official Antigravity Android plugin to inject strict Jetpack Compose/Kotlin guidelines and the `android-cli` skill.
- Created the initial memory wiki.

## [2026-09-03] ingest | Data Extraction Engine Architecture
- Documented the memory-efficient streaming pipeline combining SQLite Cursors, Kotlin Flow, and ZipOutputStream.
- Added [Data Extraction & Backup Pipeline](pages/data_extraction_architecture.md).
- Updated index.md.

## [2026-09-03] ingest | RCS Limitations & Architecture
- Researched native RCS extraction and verified it is blocked by Google's private `bugle_db` implementation.
- Added [RCS Limitations & Storage](pages/rcs_limitations.md).
- Updated index.md.
## [2026-09-04] ingest | Default SMS App & Restricted Settings
- Documented the RoleManager.ROLE_SMS requirements (dummy receivers and manifest permissions).
- Documented the Android 13+ "Restricted Settings" sideloading block and how to manually bypass it during development.
- Added [Default SMS App & Restricted Settings](pages/default_sms_restricted_settings.md).
- Updated index.md.
