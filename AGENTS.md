# Mizu Developer & Agent Architecture Rules

This document outlines the architectural conventions, file organization, state management rules, and development guidelines for the Mizu Android video player codebase.

---

## 1. Project & File Structure

The project is structured under `com.mizu.player`:

- **`MainActivity.kt`**: Single-activity host. Handles Picture-in-Picture (PiP) parameters/actions, Android configuration changes without activity recreation, permissions lifecycle, and top-level navigation between `LibraryScreen`, `PlayerScreen`, and `SettingsScreen`.
- **`model/MediaModels.kt`**: Domain models including `VideoItem`, `FolderItem`, and `SortOption`.
- **`data/MediaScanner.kt`**: Scans device storage using MediaStore and direct filesystem fallback for companion subtitles (`.srt`, `.ssa`, `.ass`, `.vtt`).
- **`data/MediaRepository.kt`**: Singleton reactive repository holding library state in memory. Manages loaded videos, folders, active folder drilldown, reactive playback resume positions, and sorting.
- **`data/UserPreferences.kt`**: Persistence layer backed by SharedPreferences (`mizu_user_prefs`).
- **`service/PlaybackService.kt`**: Media3 `MediaSessionService` that owns the playback engine (`PlayerEngine` via `VlcPlayerEngine`), system media notification card, audio focus, becoming-noisy handling, dynamic queue management, and per-video track selection logic.
- **`engine/PlayerEngine.kt` & `engine/VlcPlayerEngine.kt`**: Clean player abstraction interface and libVLC implementation. Manages hardware acceleration with software decoding fallback, styled ASS/SSA subtitle rendering via libass, audio/subtitle delay, aspect ratio scaling, and volume boost.
- **`service/VlcMediaSessionAdapter.kt`**: Media3 `SimpleBasePlayer` bridge exposing libVLC playback state to Android's `MediaSession` and system controls.
- **`ui/LibraryScreen.kt`**: Fluid Jetpack Compose library UI containing Folders, Videos, and Playlists tabs, search bar, sort options menu, grid/list layout toggles, and folder drilldown.
- **`ui/PlayerScreen.kt`**: Video player surface embedding Media3 `PlayerView`, overlay controls, custom audio/subtitle pickers, and resume banner.
- **`ui/SettingsScreen.kt`**: Minimal preferences screen for audio and subtitle language priorities and default subtitle behavior.
- **`ui/PermissionScreen.kt` & `PermissionHelper.kt`**: Storage permission onboarding and settings deep-linking.
- **`util/AndroidThumbnailFetcher.kt`**: Coil fetcher utilizing Android hardware-accelerated thumbnail generation and disk caching.

---

## 2. Playback Architecture: Service-Owned Playback & Engine Interface

- **Engine Behind an Interface (`PlayerEngine`)**: All playback operations (`play`, `pause`, `seekTo`, `setPlaybackSpeed`, `setAspectRatio`, `setAudioDelay`, `setSubtitleDelay`, `setVolumeBoost`, track queries/selection, callbacks) are abstracted behind the `PlayerEngine` interface. UI, Service, and PiP communicate solely with this interface.
- **Engine Implementation**: Powered by `VlcPlayerEngine` utilizing the official `libvlc-all` library.
- **Decoupled Playback Engine**: Playback strictly belongs to `PlaybackService` (`MediaSessionService`). It runs independently of whether `PlayerScreen` is open or destroyed.
- **UI as a Pure Client**: `PlayerScreen`, Android's Picture-in-Picture window, and system media notifications/lock screen controls all act as clients controlling this single shared player engine.
- **MediaSession Integration**: `VlcMediaSessionAdapter` wraps `PlayerEngine` into Media3's `SimpleBasePlayer` so Android's media notification card, lock screen, and HyperOS control centre receive accurate timeline, title, artwork, and playback states.
- **PiP Stability Rules**:
  - `MainActivity` must declare all relevant `configChanges` (`orientation|screenSize|smallestScreenSize|screenLayout|uiMode|keyboard|keyboardHidden|navigation`) and `android:resizeableActivity="true"`.
  - PiP exit callbacks (`onPictureInPictureModeChanged`) must NEVER call `finish()` or stop playback. Stopping playback on PiP close is only handled when the user explicitly dismisses PiP with the cross button (`isFinishing == true`).
  - `buildPipParams` must set aspect ratio and `setAutoEnterEnabled(true)` on API 31+.

---

## 3. Failsafe Playback Chain

A subtitle or audio track issue must never cause playback to abort. On playback errors, the engine attempts an automated 4-step fallback sequence:
1. **Step 1 - Without Subtitles**: Disables subtitles and retries playback at the exact timestamp.
2. **Step 2 - Without External Subtitle Files**: Ignores companion/external subtitle files and retries.
3. **Step 3 - Reset Audio Track**: Discards custom/saved audio track selection and reverts to default stream.
4. **Step 4 - Software Decoding**: Disables hardware decoder (`media.setHWDecoderEnabled(false, false)`) and relies entirely on software decoding.
5. If and only if all 4 steps fail, the user is presented with the clear error screen and Back button.
6. The failing track/stream is blacklisted in `UserPreferences` so it is not picked again for that media file.

---

## 3. Preferences Storage

All user settings and playback states are stored in SharedPreferences via `UserPreferences`:
- **Library Preferences**: View mode (`is_grid_view`), sort option (`sort_option`), active tab (`selected_tab`).
- **Playback Resume**: Video resume positions stored under `pos_<video_key>`.
- **Per-Video Track Memory**:
  - Audio track preference stored under `audio_track_<video_key>`.
  - Subtitle track preference stored under `sub_track_<video_key>` (with `"OFF"` representing disabled subtitles).
- **Language Priorities**:
  - Preferred audio language sequence (`pref_audio_langs`).
  - Preferred subtitle language sequence (`pref_sub_langs`).
  - "No subtitles by default" toggle (`pref_subs_off_by_default`).

---

## 4. Library State & Scroll Management

- **Flicker-Free Sort & Search**:
  - Lazy list and grid states must be recreated using `key(sortOption, searchQuery) { val gridState = rememberLazyGridState(); ... }`. This ensures new sorted orders are measured and drawn immediately at item index 0 without drawing at an old scroll offset or requiring a delayed `scrollToItem(0)` effect.
  - Placement animations (`animateItem` / `animateItemPlacement`) must be omitted from library grids to prevent visual sliding/glitching during reorders.
  - Thumbnails must use stable keys (`video.id` or `video.path`) in Coil's `ImageRequest` to guarantee instant memory-cache hits when items reorder.
  - Scroll state must be preserved across orientation changes, grid/list toggles, and returning from the player screen.

---

## 5. Track Selection & Priority Hierarchy

When a video loads:
1. **Per-Video Saved Track**: If the user previously selected an audio track or subtitle track (including "Off") for this specific file, apply it.
2. **Language Priorities**: If no per-video choice is saved:
   - For subtitles: check if "No subtitles by default" is active (disable subtitles if so). Otherwise, match tracks against preferred subtitle languages in priority order.
   - For audio: match tracks against preferred audio languages in priority order.
3. **File Default**: If no priority matches or no preferences exist, fall back to the media container's default tracks.

---

## 7. Development Rules & Legal Constraints

- **Official libVLC Dependency (LGPL v2.1)**:
  - Mizu uses `org.videolan.android:libvlc-all` as a dynamically linked binary Gradle dependency under the LGPL v2.1 license.
  - The required open-source attribution notice is included in the Settings screen ("About Mizu").
- **Strict Prohibition Against Copying GPL Code**:
  - Reference open-source projects like VLC for Android strictly for conceptual understanding, lifecycle flow analysis, and architectural comparison.
  - **NEVER** copy, port, or adapt GPL code from the VLC for Android application into Mizu. All application logic, player interfaces, Compose screens, and service wrappers must be clean, original code.
- **Root-Cause Investigation First**:
  - Always identify and explain the root cause of bugs (lifecycle conflicts, configuration changes, race conditions) before writing patches. Never apply speculative surface fixes without diagnosing the underlying mechanism.
