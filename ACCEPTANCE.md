# MIZU — ACCEPTANCE CRITERIA & VERIFICATION CHECKLIST (ACCEPTANCE.md)

## Status Overview
- **Milestone B1**: Foundation, Rebranding, Network Excision, Welcome Permissions, and CI Pipeline.
- **Verification Target**: GitHub Actions CI with hardware-accelerated emulator on Android 14 (API 34) x86_64.

---

## 1. Safety, Keystore & Git History
- [x] Local release keystore at `~/.mizu-signing/mizu-release.jks` untouched and protected.
- [x] Legacy v1 code preserved on remote branch `origin/legacy-v1`.
- [x] Base built cleanly from Next Player `v0.21.0` commit `08845f882dfd3b296342cd2044f96ead9d621faa` with full commit history preserved.
- [x] Repository visibility verified as PUBLIC for hardware-accelerated emulator availability.
- [x] Zero keystore binaries, passwords, or secrets committed to git.

---

## 2. Rebranding & Credits Compliance
- [x] App name is "Mizu" across all strings, debug builds, and manifest declarations.
- [x] Application ID is `com.mizu.player` (`applicationIdSuffix = ".debug"` for debug).
- [x] App icon updated to bold ink play-drop glyph on Sun-Yellow (`#FFD84D`) for background, foreground, monochrome, and splash.
- [x] All upstream Next Player UI branding removed from user-facing screens.
- [x] First-launch popup displayed: *"Mizu is based on the open-source Next Player by Anil Beesetti (GPL-3.0). Not affiliated or endorsed."*
- [x] About screen includes credit attribution, source code link (`https://github.com/t4ulquiorra/Mizu`), and open-source license list.
- [x] All upstream GPL-3.0 file headers and LICENSE file preserved intact.

---

## 3. 100% Offline Guarantee & Attack Surface Excision
- [x] `:feature:network` module completely removed from repository and settings.
- [x] Network protocol dependencies stripped (`smbj`, `sshj`, `sardine-android`, `commons-net`, `coil-network-okhttp`).
- [x] `android.permission.INTERNET` removed from manifest and blocked via `tools:node="remove"`.
- [x] `android.permission.ACCESS_LOCAL_NETWORK` removed and blocked via `tools:node="remove"`.
- [x] Automated CI check asserts that `android.permission.INTERNET` is absent from all merged manifests (debug and release).

---

## 4. Permissions & Welcome Flow
- [x] Fresh install launches directly into `WelcomePermissionView`.
- [x] Zero system permission dialogs are requested until the user explicitly taps an action.
- [x] Option 1: "Allow full access (recommended)" requests `MANAGE_EXTERNAL_STORAGE` on Android 11+ via system settings, or storage permissions on Android 10.
- [x] Option 2: "Videos only" requests `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` on Android 13+, or storage permissions on Android 12 and below.
- [x] Returning from system settings with `MANAGE_EXTERNAL_STORAGE` granted is automatically detected on resume (`Lifecycle.Event.ON_RESUME`).
- [x] Initial library scan displays "Scanning your library…" loading state.
- [x] "No videos found" is never shown before the first library scan has completed.
- [x] Partial access on Android 14 (`READ_MEDIA_VISUAL_USER_SELECTED`) shows limited access banner offering upgrade to full access.

---

## 5. CI Pipeline & Automated Verification
- [x] Unified CI workflow at `.github/workflows/build.yml`.
- [x] Unit test execution via `./gradlew test --stacktrace`.
- [x] Lint verification via `./gradlew ktlintCheck`.
- [x] Signed release APK assembly via `./gradlew assembleRelease` signed with repository secrets.
- [x] Automated multi-track test media generated with FFmpeg (`t_mp4.mp4`, `t_mkv.mkv`, `t_avi.avi`, `t_audio.mp3`, `t_mp4.srt`).
- [x] Hardware-accelerated Android 14 (API 34) x86_64 emulator runner with KVM.
- [x] Upgrade continuity test: Downloads previous release `v0.1.0`, installs via `adb install`, then upgrades to newly built release APK via `adb install -r`.
- [x] UI screenshots and logcat uploaded as CI artifacts.
- [x] GitHub Release created with signed APKs (`arm64-v8a` listed first).
