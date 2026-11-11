# Mizu Architectural & Design Decisions

This document records key non-obvious technical and design choices made during the creation and evolution of Mizu.

---

### 1. Zero Internet Permission & Offline Guarantee
- **Decision:** The `android.permission.INTERNET` permission is completely omitted from `AndroidManifest.xml`.
- **Rationale:** Mizu is strictly an offline media player. Declaring no network permission provides an ironclad, operating-system-level guarantee to users that no telemetry, ads, tracking, or unexpected network traffic can ever occur.

### 2. Dual Playback Engine Architecture with Automatic Seamless Fallback
- **Decision:** Implement a unified `PlayerEngine` interface with two decoupled engines:
  1. **Standard Engine (`ExoPlayerEngine`)**: Jetpack Media3 (1.11.1) coupled with NextLib (1.11.1-0.16.0) FFmpeg extension renderers. Hardware decoders are prioritized via `MediaCodec`, with automatic software decoding fallback for non-standard or unsupported audio/video streams (AC3, E-AC3, DTS, TrueHD, Opus, FLAC).
  2. **Compatibility Engine (`VlcPlayerEngine`)**: LibVLC 3.6.5. Used for legacy or exotic containers (AVI, RMVB, WMV, FLV, VOB, TS, ISO) and complex subtitle rendering (styled ASS/SSA, VobSub, DVB).
- **Auto Engine Selection:** Mizu probes media containers and codecs. If ExoPlayer encounters an unrecoverable format or decoder error, the session automatically transitions to LibVLC at the exact millisecond timestamp, preserving selected track indices and playback rate.

### 3. Neubrutalism Design System over Material You
- **Decision:** Replaced default Material You dynamic color theming with a bespoke Neubrutalism visual language.
- **Tokens & Aesthetics:**
  - **Palette:** Ink (`#111111`), Paper (`#FFF6E3`), Surface (`#FFFFFF`), Sun (`#FFD84D`), Coral (`#FF6B57`), Aqua (`#5CE1E6`), Lavender (`#B9A4FF`), Lime (`#B8F36B`).
  - **Night Theme:** Charcoal Paper (`#1F1D1A`) with warm cream borders (`#F3EAD3`) and surface `#2A2723`, avoiding stark OLED pure blacks.
  - **Structure:** 2.5dp solid ink outlines, 0-blur hard offset shadows (3dp buttons/chips, 4dp cards, 6dp dialogs/sheets).
  - **Physics:** Physical tactile press transitions where cards and buttons translate by their shadow offset on click with light haptic feedback.
  - **Signature Elements:** Handcrafted vector droplet mascot (`MizuMascot`), rotated sticker-style badges (-2° to +2°), striped progress indicators, and deterministic folder accent colors derived from name hashes.

### 4. Granular Permissions & Welcome Screen Flow
- **Decision:** Never prompt for system permissions on the first frame.
- **Implementation:**
  - First launch presents a welcoming Neubrutalist onboarding screen with two clear options:
    1. **"Show all my videos"**: Requests scoped media permissions (`READ_MEDIA_VIDEO` on Android 13+, `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32` on Android 12 and below, and handles `READ_MEDIA_VISUAL_USER_SELECTED` partial access gracefully on Android 14+).
    2. **"Pick files or a folder"**: Uses Android Storage Access Framework (SAF `OpenDocument` / `OpenDocumentTree`) with persistable URI permission grants, requiring zero storage permissions.
  - `READ_MEDIA_AUDIO` is deferred strictly until the user accesses the Audio tab.
  - `POST_NOTIFICATIONS` is only prompted when background audio playback begins.
  - `MANAGE_EXTERNAL_STORAGE` is strictly avoided.

### 5. Background Playback via MediaSessionService
- **Decision:** Playback lifecycle is managed through `MizuPlaybackService`, extending `androidx.media3.session.MediaSessionService` with foreground service type `mediaPlayback`.
- **Rationale:** Ensures reliable background audio and video-as-audio playback, proper audio focus handling (ducking on notifications, pause on unplug / becoming noisy), and full lock-screen / Bluetooth headset control integration.

### 6. Dependency Versioning & Reproducible CI
- **Decision:** Standardized dependencies on Gradle Version Catalogs (`gradle/libs.versions.toml`) using stable releases:
  - AGP `8.9.2`, Kotlin `2.0.21`, KSP `2.0.21-1.0.27`
  - Jetpack Compose BOM `2024.12.01`
  - Media3 `1.11.1` and NextLib `1.11.1-0.16.0`
  - LibVLC `3.6.5`
  - Dagger Hilt `2.51.1`, Room `2.6.1`, DataStore `1.1.1`
- **CI / CD:** Automated GitHub Actions workflow (`.github/workflows/build.yml`) utilizing Temurin JDK 17, Gradle caching, unit testing, ABI split compilation (`arm64-v8a`, `armeabi-v7a`, `x86_64`, and universal), automated signing via repository secrets, and direct GitHub release creation.

### 7. Licensing
- **Decision:** Published under the GNU General Public License v3.0 (`GPL-3.0`).
- **Rationale:** Required due to linking with GPL-licensed components in NextLib and LibVLC FFmpeg decoders. An in-app open source licenses screen gives full attribution to all dependencies.


---

### 8. Milestone F1: Root Causes, Fixes & Hardening

#### A. ExoPlayer Black Screen (Bug K)
- **Root Cause:** In `PlayerScreen`, `AndroidView.factory` immediately invoked `viewModel.attachSurface(this)` before `activeEngine.prepare()` had been called. In `ExoPlayerEngine`, `attachSurfaceView` was guarded by `player?.setVideoSurfaceView(surfaceView)`. Because `player` was `null` prior to `prepare()`, the `SurfaceView` was silently ignored and discarded. Furthermore, surface destruction and recreation upon rotation or recomposition lacked active `SurfaceHolder.Callback` synchronization.
- **Fix:** `ExoPlayerEngine` retains a persistent reference to `currentSurfaceView`. Upon `initPlayer()`, the surface view is immediately attached. A dedicated `SurfaceHolder.Callback` monitors `surfaceCreated`, `surfaceChanged`, and `surfaceDestroyed` lifecycle events, keeping the decoder surface synchronized. In addition, an `AnalyticsListener` now logs decoder initialization, dropped frames, and codec exceptions to `PlaybackLog`.

#### B. LibVLC Compatibility Engine Non-Functional (Bug M)
- **Root Cause:** Passing `content://` URIs directly to `Media(libVLC, uri)` fails on modern Android because native LibVLC cannot open scoped storage content URIs via standard POSIX paths.
- **Fix:** In `VlcPlayerEngine.prepare()`, for `content://` URIs, the engine opens a `ParcelFileDescriptor` using `context.contentResolver.openFileDescriptor(uri, "r")` and constructs `Media(libVLC, pfd.fileDescriptor)`, keeping the descriptor open throughout playback. Rebuilt `vlcVout` view attachment with window size management and wired all `MediaPlayer.EventListener` events (`LengthChanged`, `TimeChanged`, `ESAdded`, `ESSelected`, `EncounteredError`) to update the playback state reactively.

#### C. Track Selection & Subtitle Rendering (Bug L)
- **Root Cause:** `selectAudioTrack` and `selectSubtitleTrack` in `ExoPlayerEngine` were no-op stubs that only updated local StateFlow properties without modifying `trackSelectionParameters`. Furthermore, `PlayerScreen` completely lacked a `SubtitleView`.
- **Fix:** Implemented `TrackSelectionOverride` mapping on `player.trackSelectionParameters` for both audio and text tracks. Mapped disabled subtitles to `setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)`. Attached an explicit `androidx.media3.ui.SubtitleView` positioned over the `SurfaceView` in `PlayerScreen`, styled with outline contrast to prevent invisible text.

#### D. Engine Selection & Auto-Fallback (Bug N)
- **Decision:** Defaulted engine preference to `PlayerEngine.AUTO`.
- **Implementation:** Added container and extension probing in `PlayerViewModel`. Exotic formats (`avi`, `rmvb`, `wmv`, `flv`, `vob`, `ts`, etc.) automatically route to LibVLC, while standard MP4/MKV routes to ExoPlayer + NextLib. On unrecoverable ExoPlayer playback or decoder errors, the session automatically falls back to LibVLC at the exact millisecond position, preserving speed and track choices.

#### E. Seek Duration Unification (Bug O)
- **Decision:** Unified seek preferences into a single `seekDuration` DataStore key (default 10s, supporting 5s, 10s, 15s, 30s, 60s).
- **Implementation:** Both the on-screen seek button labels (`-10s`, `+10s`) and double-tap gesture seek increments dynamically read from this single source of truth.

#### F. File-Backed Playback Diagnostics Log (Section 1.6)
- **Implementation:** Created `PlaybackLog`, a 2 MB file-backed ring buffer tracking engine choice, URI scheme, decoder initialization, dropped frames, VLC events, and uncaught exceptions. Integrated into `AboutScreen` with live text viewing, sharing via Android Sharesheet, and clearing.

#### G. WCAG 2.1 AA Theme Contrast Compliance (Section 1.4)
- **Implementation:** Evaluated and proved all Light and Dark theme token pairs achieve >= 4.5:1 contrast ratios through automated JVM unit tests (`ThemeContrastTest.kt`). Redesigned the dark palette to use a deep indigo-ink background (`#1A1830`), raised indigo surfaces (`#262344`), and cream text (`#FFF6E3`).

#### H. Pure Logic Unit Test Suite & Natural Order Sorting
- **Implementation:** Added comprehensive JVM unit tests covering:
  - `NaturalOrderComparatorTest`: Alphanumeric natural order sorting (e.g. `Episode 1`, `Episode 2`, `Episode 10`), case-insensitivity, and numerical chunk parsing.
  - `WatchStateTest`: Unwatched (< 3%), In Progress (3% to 95%), and Watched (>= 95%) state boundaries and percentage calculation.
  - `FolderGroupingTest`: Storage path sanitization (`Internal storage` and `SD card` friendly paths) and duplicate folder disambiguation (`Anime (Movies)` vs `Anime (Downloads)`).
  - `SubtitleUtilsTest`: SRT timestamp parsing to milliseconds, HTML/ASS tag stripping, and BOM/UTF-8/Latin-1 charset detection.
  - `FormatUtilsTest`: Release tag / junk pattern stripping, title cleaning, middle ellipsis truncation (`start…end`), duration and file size formatting.

#### I. Hardware-Accelerated Emulator CI Storage Synchronization
- **Root Cause:** On Android 14 (API 34), external storage (`/sdcard`) requires storage daemon and user unlock initialization. Executing `adb push` immediately after `adb wait-for-device` produced `Operation not permitted`.
- **Fix:** In `.github/workflows/build.yml`, added wait loop for `sys.boot_completed == 1`, enabled `adb root`, and added an active storage writability probe before pushing test media. Ensured test screenshots and logcats are captured regardless of test outcome. Also encapsulated emulator shell execution into `tools/run_emulator_tests.sh` to prevent GitHub Actions multi-line shell evaluation failures.

---

### 9. Milestone F2 & F3: Architecture, Polish & Hardening

#### A. Dedicated Folder Route & Navigation (Bugs B & C)
- **Implementation:** Added `folder?path={path}&name={name}` route in `MainActivity.kt` and created `FolderDetailScreen`. Clicking a folder card in `FoldersTab` transitions to this dedicated screen, displaying sanitized storage path headers, total video counts and duration, natural-order sorting, view toggling, watch state filters, and selection mode. Pressing back pops the back stack cleanly to the Folders tab.

#### B. High-Performance Thumbnail Pipeline (Bug A)
- **Implementation:** Built custom Coil `MediaThumbnailFetcher` in `data/thumbnail/`. On API 29+, it calls `ContentResolver.loadThumbnail(uri, Size(512, 384), null)` for fast OS-cached decoding. For fallback, it uses `MediaMetadataRetriever` targeted at 10% of duration (never 0 ms to avoid initial black frames). In `FolderCard`, the 4 newest videos by `dateModified` form the mosaic preview.

#### C. Live Filter Chips & Scroll Reset (Bugs D & E)
- **Implementation:** Added horizontal scrollable filter chips in `VideosTab` and `FolderDetailScreen`: All, Unwatched, In Progress, Watched, Subtitled, 4K, HDR, with dynamic live counts. Wired `LaunchedEffect(sortOrder, activeFilter)` to reset `LazyVerticalGrid` and `LazyColumn` scroll positions to item 0 on change.

#### D. Playlists Detail Screen & Multi-Select Integration (Bug H)
- **Implementation:** Created `PlaylistDetailScreen` with "Play All" and "Shuffle" actions, item removal, and playlist deletion. Exposed `getPlaylistItems` and `removeFromPlaylist` via `LibraryViewModel`.

#### E. Audio Sub-tabs & Library Inclusivity (Bugs I & J)
- **Implementation:** Added dynamic live counts to Tracks, Albums, Artists, and Folders sub-tabs in `AudioTab`. Verified media scanner queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` without `IS_MUSIC != 0` restriction, discovering all local audio formats.

#### F. Player UI Polish & Speed Panel (Bugs P, Q, T)
- **Implementation:** Added custom typed speed text field with range validation (0.1x to 8.0x) and preset chips (up to 3.0x). Added floating seek time bubble when dragging the progress scrubber. Displayed clean title with marquee scroll and parent folder name in the top bar.

#### G. Dark Mode Contrast Polish (Bug S)
- **Implementation:** Replaced hardcoded `Ink` references with theme-aware `colors.text` and `colors.textMuted` in `SettingsScreen` and `VideoTile` to ensure contrast compliance in both Light and Night modes.
