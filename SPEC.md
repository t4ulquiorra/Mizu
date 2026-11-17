# MIZU — PRODUCT SPECIFICATION (SPEC.md)

## 1. Product Overview
**Mizu** (`com.mizu.player`) is an offline video and audio player for Android engineered for speed, privacy, and visual distinction. It delivers a desktop-class local playback experience with a custom Neubrutalism design system.

### Core Principles
- **100% Offline Integrity**: Mizu declares zero internet permissions in its manifest and includes zero network client dependencies. All processing, indexing, and playback occur on-device.
- **Privacy by Design**: No telemetry, no analytics, no accounts, no ads, and no external calls.
- **High Performance Playback**: Instant playback start, smooth seeking, hardware-accelerated decoding with software fallbacks, and multi-track audio and subtitle support.
- **Bespoke Visual Identity**: Neubrutalism design language with bold ink borders, crisp drop shadows, high-contrast color palettes (Sun-Yellow `#FFD84D` accent), and tablet-first adaptive layouts.

---

## 2. Technical Stack
- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Kotlin Compiler 2.x) with custom theme layer
- **Navigation**: AndroidX Navigation3
- **Media Engine**: AndroidX Media3 (ExoPlayer + MediaSessionService)
- **Dependency Injection**: Koin (annotations + compile-time verify)
- **Local Database**: Room 2.6+ with SQLite
- **Preferences**: Jetpack DataStore Preferences
- **Image / Thumbnail Loading**: Coil 3 (local file decoder only)
- **Target SDK**: Android 14 / 15 (compileSdk 35, minSdk 26)
- **Application ID**: `com.mizu.player`

---

## 3. Permissions & Welcome Flow
On fresh install, Mizu presents a dedicated Welcome screen with two clear options:
1. **Allow full access (recommended)**:
   - On Android 11+ (API 30+): Directs user to `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` for `package:com.mizu.player`.
   - On Android 10 and below: Requests `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`.
   - Enables recursive discovery of video/audio files across custom directories and automatic pairing of external sidecar subtitle files (`.srt`, `.ass`).
2. **Videos only**:
   - On Android 13+ (API 33+): Requests `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO`.
   - On Android 14+ (API 34+): Supports partial media access (`READ_MEDIA_VISUAL_USER_SELECTED`) with an in-app banner enabling full access upgrade.
   - On Android 12 and below: Requests `READ_EXTERNAL_STORAGE`.

### Flow Invariants:
- Zero permission dialogs or system prompts are triggered until user explicitly interacts with an option.
- Returning from system settings with `MANAGE_EXTERNAL_STORAGE` granted immediately transitions to the library scan.
- Initial load displays "Scanning your library…". An empty state ("No videos found") is never shown before the scan completes.

---

## 4. Playback & Engine Capabilities
- **Supported Video Formats**: H.264/AVC, H.265/HEVC, VP8, VP9, AV1, MPEG-4, in MP4, MKV, WebM, AVI, and TS containers.
- **Supported Audio Formats**: AAC, MP3, FLAC, Opus, Vorbis, PCM, AC-3, E-AC-3, DTS.
- **Subtitle Engines**:
  - Embedded subtitles: SRT, ASS/SSA, mov_text, TTML, VTT.
  - External sidecar subtitles: Auto-detection of `.srt`, `.ass`, `.vtt` matching media filename.
  - Manual subtitle file picker.
- **Playback Controls**:
  - Vertical swipe gestures: Left side for brightness, right side for volume.
  - Horizontal swipe gesture: High-precision scrubbing/seeking.
  - Playback speed control: 0.25x to 4.0x with pitch preservation.
  - Aspect ratio modes: Fit, Stretch, Crop, 100%.
  - Picture-in-Picture (PiP) support.
  - Lock controls to prevent accidental touches.

---

## 5. Build, Verification & Release Pipeline
- **CI Compiler**: GitHub Actions (`.github/workflows/build.yml`) on `ubuntu-latest`.
- **Manifest Check**: Automated validation ensuring `android.permission.INTERNET` is absent from all merged manifests.
- **Hardware-Accelerated Emulator**: Android 14 (API 34) x86_64 runner via KVM executing automated UI and playback test suites with multi-track media.
- **Upgrade Verification**: Automated download and install of previous release `v0.1.0` followed by upgrade via `adb install -r` to verify certificate and database continuity.
- **Artifacts**: Signed release APKs (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `universal`) published automatically on tagged builds.
