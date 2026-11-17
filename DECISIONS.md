# MIZU — ARCHITECTURAL & DESIGN DECISIONS (DECISIONS.md)

## Decision 1: Foundation on Next Player v0.21.0
- **Context**: The master brief required rebasing on the latest stable Next Player (`anilbeesetti/nextplayer`, GPL-3.0) while preserving its git history for attribution and license integrity.
- **Decision**: Checked out release tag `v0.21.0` (commit `08845f882dfd3b296342cd2044f96ead9d621faa`) directly on `main` after backing up legacy v1 work to `legacy-v1`. Kept internal Kotlin package names (`dev.anilbeesetti.nextplayer`) for core media/engine layers to maintain clean future upstream mergeability, while changing the outward Application ID to `com.mizu.player`.

## Decision 2: Excision of `:feature:network` and Absolute Offline Guarantee
- **Context**: Next Player supported network browsing (SMB, SFTP, WebDAV, FTP), requiring `INTERNET`, `ACCESS_LOCAL_NETWORK`, and heavy third-party client dependencies (`smbj`, `sshj`, `sardine`, `commons-net`).
- **Decision**: Completely excised `:feature:network` from the repository and Gradle settings. Removed all network client libraries and network image loaders (`coil-network-okhttp`). Replaced `NextDataSourceFactory` with a pure local `DefaultDataSource.Factory(context)`. Blocked `INTERNET` and `ACCESS_LOCAL_NETWORK` using `tools:node="remove"` in `app/src/main/AndroidManifest.xml` and enforced zero network permissions via an automated CI verification check.

## Decision 3: Welcome Screen & Two-Path Permission Architecture
- **Context**: On Android 11+, apps querying MediaStore without `MANAGE_EXTERNAL_STORAGE` cannot see sidecar subtitles (`.srt`, `.ass`) stored alongside videos or arbitrary local directory structures without individual system document pickers.
- **Decision**: Implemented `WelcomePermissionView` featuring two explicit options:
  1. *Allow full access (recommended)*: Invokes `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` with fallback to `Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`.
  2. *Videos only*: Requests `READ_MEDIA_VIDEO` (and `READ_MEDIA_AUDIO`) on Android 13+, with in-app banner upgrades for partial access on Android 14 (`READ_MEDIA_VISUAL_USER_SELECTED`).
  No system permission dialogs are shown until user interaction. Returning from settings with `MANAGE_EXTERNAL_STORAGE` is detected dynamically via `Lifecycle.Event.ON_RESUME` to start the library scan without user intervention.

## Decision 4: Elimination of False Empty States During Scans
- **Context**: When a player starts scanning large media libraries on first launch, a brief empty state ("No videos found") can flash before MediaStore returns results, confusing users.
- **Decision**: `MediaPickerViewModel` maintains `DataState.Loading` while scanning. `MediaPickerScreen` renders a prominent "Scanning your library…" progress indicator, ensuring `NoVideosFound` is only rendered once a scan has completed successfully with zero videos and zero folders found.

## Decision 5: Unified CI with Upgrade Verification
- **Context**: Previous builds had split workflows for building, publishing, and bumping versions, with no automated proof that a new release would install over existing installations.
- **Decision**: Consolidated CI into `.github/workflows/build.yml`. Added an upgrade test on the Android 14 API 34 emulator runner: downloads previous signed release `v0.1.0` (`app-x86_64-release.apk`), installs it via `adb install`, and updates it via `adb install -r <new-release-apk>`. This validates that `applicationId`, signing keystore, and Room database migrations are compatible across versions.
