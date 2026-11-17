# Mizu Codebase Audit & Architectural Plan

## 1. Upstream Base & Origin
- **Base Project**: [Next Player](https://github.com/anilbeesetti/nextplayer) by Anil Beesetti.
- **Base Version**: `v0.21.0` (commit `08845f882dfd3b296342cd2044f96ead9d621faa`).
- **Licence**: GNU General Public License v3.0 (GPL-3.0). All license headers, copyrights, and attribution are preserved in full compliance with GPL-3.0.

---

## 2. Multi-Module Architecture
The project is organized into structured, unidirectional Gradle modules:
- `:app`: Application entry point (`MainActivity`), navigation backstack orchestration (`Navigation3`), top-level destinations, and release APK packaging.
- `:core:model`: Pure Kotlin data models (`Video`, `Folder`, `MediumState`, `PlaylistSummary`, `ApplicationPreferences`). Zero Android framework dependencies.
- `:core:database`: Room database definitions (`AppDatabase`), entity tables (`MediumStateEntity`, `PlaylistEntity`), and data access objects (`MediumStateDao`, `PlaylistDao`).
- `:core:data`: Repositories and data synchronization bindings between Room, DataStore preferences, and MediaStore.
- `:core:common`: Shared utilities, coroutine scopes, dispatchers, string/path helpers, and storage access verification.
- `:core:media`: Media playback engine layer, Media3 `MediaSessionService`, decoder configuration, audio/subtitle track selectors, and thumbnail generators.
- `:core:ui`: Reusable Compose UI components, base themes, design system tokens, icons, and dialogs.
- `:feature:videopicker`: Media library browser, folder navigation, multi-selection manager, search, and the initial Welcome permission flow.
- `:feature:player`: Video playback interface, gesture controls (volume, brightness, seek), track selectors, speed controls, and Picture-in-Picture.
- `:feature:playlist`: Playlist creation, management, reordering, and playback queues.
- `:feature:settings`: Application settings, appearance, playback preferences, decoder options, and About/Credits screens.

---

## 3. Excision of Network Features & 100% Offline Integrity
Next Player included local network streaming features (SMB, SFTP, WebDAV, FTP) which required broad network permissions and heavy external protocol client libraries:
- `com.hierynomus:smbj` (SMB2/SMB3 client)
- `com.hierynomus:sshj` (SSH/SFTP client)
- `com.github.thegrizzlylabs:sardine-android` (WebDAV client)
- `commons-net:commons-net` (FTP client)
- `coil3:coil-network-okhttp` (Network image loading)

### Architectural Remediation:
1. Entire `:feature:network` module deleted from source tree and removed from `settings.gradle.kts`.
2. All network dependencies and transitive client libraries removed from `core:media/build.gradle.kts` and `app/build.gradle.kts`.
3. `NextDataSourceFactory.kt` simplified to wrap `DefaultDataSource.Factory(context)` exclusively for local files and content URIs.
4. `android.permission.INTERNET` and `android.permission.ACCESS_LOCAL_NETWORK` stripped and explicitly blocked using `<uses-permission android:name="..." tools:node="remove" />`.
5. Automated CI step added in `.github/workflows/build.yml` which analyzes both source and merged release manifests, failing the build if `INTERNET` permission is introduced.

---

## 4. Permissions, Storage, & Welcome Flow
### Scoped Storage Landscape:
- **Android 11+ (API 30+)**: Full storage access requires `MANAGE_EXTERNAL_STORAGE` (`android.permission.MANAGE_EXTERNAL_STORAGE`) routed through `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.
- **Android 13+ (API 33+)**: Granular media permissions `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO`.
- **Android 14+ (API 34+)**: Partial visual media access `READ_MEDIA_VISUAL_USER_SELECTED`.
- **Android 10 and below**: Traditional `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`.

### Implementation:
- **WelcomePermissionView**: App presents a first-run welcome screen with two distinct options:
  1. *Allow full access (recommended)*: Invokes `MANAGE_EXTERNAL_STORAGE` settings on Android 11+, or requests storage permissions on Android 10.
  2. *Videos only*: Requests `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO` on Android 13+, or `READ_EXTERNAL_STORAGE` on older platforms.
- **Return Detection**: `MediaPickerScreen` monitors `Lifecycle.Event.ON_RESUME` to detect when the user returns from system settings after granting permissions, immediately triggering library scan.
- **Scanning State**: Displays "Scanning your library…" during initial load. "No videos found" is never shown while scanning.

---

## 5. Fragile Areas & Hardening Plan
1. **R8 / Minification**:
   - `isMinifyEnabled = true` and `isShrinkResources = true` in release builds.
   - Proguard rules must protect Media3 internal decoders, Koin annotations, and Room entities against reflection stripping.
2. **Subtitles & Complex Formats**:
   - Both embedded (SRT, ASS/SSA, mov_text) and external sidecar subtitles must be resolved accurately without crashing on unmapped MIME types.
3. **Database Migration Safety**:
   - Room schema versions and migrations must preserve user watch history and playlists across updates.
4. **Upgrade Compatibility**:
   - Verified on CI via hardware-accelerated emulator: installing prior signed release `v0.1.0` and updating to the latest release APK via `adb install -r`.

---

## 6. Skin System Architecture Plan
To support the flagship **Neubrutalism** skin while allowing future expansion (Minimal, Glass, Material You):
- Establish `MizuTheme` as the unified source of truth for UI styling.
- Decouple components from direct `MaterialTheme.colorScheme` calls into `MizuStyle` semantic tokens:
  - Surface fills, ink borders, drop shadows with hard offsets (e.g., 4.dp X/Y, zero blur), and distinctive typography.
  - High-contrast accessibility meeting WCAG AAA ratios.
