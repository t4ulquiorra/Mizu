# Mizu Player 🌊

Mizu (水) is a fast, completely offline video & audio player for Android built with Jetpack Compose and a bold Neubrutalism design system.

## ✨ Features

- **Neubrutalism Design System:** Distinctive high-contrast visual styling featuring bold ink borders, crisp hard shadows, saturated accent colors, and custom tactile press physics.
- **Dual Playback Engines:**
  - **Standard Engine:** Powered by AndroidX Media3 (ExoPlayer) with NextLib software decoders for hardware-accelerated playback.
  - **Compatibility Engine:** Powered by LibVLC (VideoLAN 3.6.5) for legacy container support, custom formats, and seamless auto-fallback.
- **100% Offline & Private:** Zero internet permission (`android.permission.INTERNET` is completely omitted). No analytics, telemetry, or remote connections.
- **Privacy-First Permissions:**
  - Zero-permission access via Android Storage Access Framework (SAF) folder picker.
  - Scoped media permissions on modern Android versions (`READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`).
  - Audio permissions requested on-demand only when opening the audio tab.
- **Rich Media Library:**
  - Folders tab with mosaic album previews, item counts, and newly-detected folder badges.
  - Videos tab supporting both Grid and List modes, sorting, and Continue Watching resume progress.
  - Audio tab supporting Tracks, Albums, Artists, and Folders.
  - Full playlist creation, editing, and management.
- **Gesture Controls & Custom Player:**
  - Double-tap seek (left/right).
  - Subtitle and audio track pickers.
  - Variable speed playback (0.5x to 2.0x).
  - Background audio playback via `MediaSessionService`.

## 🛠️ Tech Stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** Clean Architecture (UI / Domain / Data / Player), Kotlin Coroutines & Flow
- **Dependency Injection:** Hilt
- **Local Persistence:** Room Database, DataStore Preferences
- **Image Loading:** Coil
- **Media Engines:** AndroidX Media3 ExoPlayer 1.11.1, NextLib 1.11.1-0.16.0, LibVLC 3.6.5
- **Tooling:** AGP 8.9.2, Kotlin 2.0.21, KSP, Gradle 8.11.1

## 📄 License

GPL-3.0 License
