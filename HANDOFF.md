# Ad-Free SMS Backup & Restore — Engineering Handoff

> **Updated:** 2026-09-03
> **Updated by:** Antigravity (fd81a7e3-b9a3-4a9f-9b09-367bb748f15c)
> **Priority Mission:** Scaffold the baseline Android project (Kotlin/Jetpack Compose)
> **Operator:** Brian

---

## 0. COMPLETED WORK (DO NOT REVISIT)
| Session | Work | Status |
|---------|------|--------|
| fd81a7e3 | Initialized local git repo and remote GitHub repository (private -> public) | ✅ RESOLVED |
| fd81a7e3 | Mapped roadmap to GitHub Issues #1-8 and Kanban Project #12 | ✅ RESOLVED |
| fd81a7e3 | Added Buy Me A Coffee link to README.md via GitOps | ✅ RESOLVED |
| fd81a7e3 | Enforced GPL-3.0 License for the consumer Android app | ✅ RESOLVED |
| fd81a7e3 | Bootstrapped persistent `memory-wiki/` and documented Dual-License Strategy | ✅ RESOLVED |

---

## 1. PROJECT IDENTITY
This is a native Android 14/15 application built to disrupt legacy SMS backup utilities (e.g., SyncTech) by offering a beautifully designed, 100% ad-free, privacy-respecting alternative. The project is strictly 100% Kotlin utilizing Jetpack Compose and Material You. The core monetization strategy relies purely on community goodwill via tipping.

### Your Knowledge Base
- [Project Wiki Index](c:\Users\kingb\Ad-Free SMS Backup & Restore\memory-wiki\index.md)
- [App Roadmap](c:\Users\kingb\Ad-Free SMS Backup & Restore\docs\sms_app_roadmap.md)
- [J.O.S.H.U.A. Rules / GEMINI.md](c:\Users\kingb\Ad-Free SMS Backup & Restore\GEMINI.md)

---

## 2. YOUR MISSION: ANDROID PROJECT SCAFFOLDING (Issue #8)
Your immediate goal is to initialize the baseline Android project structure so feature development can begin.

### Execution Queue (in order)
#### 1️⃣ Initialize Android App Structure
**Problem:** The repository currently only contains documentation, memory-wiki, and licensing. It lacks the actual Android project files (Gradle, `app/src/main`, `AndroidManifest.xml`, etc.).
**Fix:** Use the `android` CLI (via the enabled Android Antigravity plugin) or standard gradle tooling to generate a pristine, modern Kotlin/Compose Android project in the repository root.
**Key files:** `build.gradle.kts` (Root), `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`

---

## 3. DETAILED ANALYSIS / BREAKDOWN
- **Tech Stack Mandate:** 100% Kotlin. No Java.
- **UI Framework:** Strictly Jetpack Compose. No XML layouts for the UI.
- **Licensing Constraint:** This app is GPL-3.0. Do not include proprietary SDKs or ad networks.
- **GitOps Constraint:** You MUST perform this scaffolding inside an isolated GitOps worktree (e.g., `git worktree add -b chore/android-init workspace/android-init`). Do not generate the files directly in `master`.

---

## 4. IMPLEMENTATION STRATEGY
1. Claim Issue #8 on the GitHub Project Kanban board (#12).
2. Spawn a new GitOps worktree.
3. Utilize the `android-cli` skill to scaffold the project properly with modern Compose configurations.
4. Clean up any boilerplate code (e.g., standard "Hello World" texts) to prepare a clean canvas.
5. Commit, merge to `master`, delete the worktree, and close Issue #8.

---

## 5. THE CRITICAL TRAPS & WARNINGS
> **⚠️ EPISTEMIC / OPERATIONAL WARNINGS**
> - **Never code on `master`:** Zero-exemption mandate. You must branch out using `git worktree`.
> - **No Hallucinations:** Use the `android-cli` knowledge to write accurate, modern Compose code. Do not fall back to outdated Android View syntax.
> - **The iOS Pipedream:** Do not attempt or plan any iOS porting. This is strictly Android native.

---

## 6. KEY PATHS
- **Root Directory:** `c:\Users\kingb\Ad-Free SMS Backup & Restore`
- **Memory Wiki:** `c:\Users\kingb\Ad-Free SMS Backup & Restore\memory-wiki\index.md`
- **Rules Engine:** `c:\Users\kingb\Ad-Free SMS Backup & Restore\GEMINI.md`

---

## 7. THE FULL PICTURE / WHAT COMES AFTER
Once the project is scaffolded (Issue #8), the next critical path is **Issue #1: MVP SMS & MMS Extraction Engine**. This will require deep Android knowledge of querying the `Telephony.Sms.CONTENT_URI` and safely extracting large chunks of data asynchronously.

---

## 8. OPERATOR PREFERENCES
- Professional, direct, empirical.
- Write tests before or alongside implementation.
- All major decisions must be pushed to the `memory-wiki/` and merged via GitOps.

---

## 9. IMMEDIATE NEXT STEPS
1. Read `c:\Users\kingb\Ad-Free SMS Backup & Restore\GEMINI.md` to internalize the Operator's rules.
2. Read `c:\Users\kingb\.gemini\config\plugins\android-cli-plugin\skills\SKILL.md` to understand your Android tooling capabilities.
3. Spawn a GitOps worktree for Issue #8 and begin scaffolding the Android project.
