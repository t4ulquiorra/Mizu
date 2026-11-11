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
