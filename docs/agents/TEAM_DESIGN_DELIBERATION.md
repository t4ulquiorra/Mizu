# Mizu Architecture & Design Team Deliberation
**Project:** Mizu (`com.mizu.player`)  
**Context:** Engineering & Design Overhaul using Unified Layout & UI Skill Systems  
**Date:** 2026-11-18  

---

## 1. Team Roster & Role Assignments

| Role / Handle | Specialist Focus | Skill Sources Leveraged |
| :--- | :--- | :--- |
| **`@Coordinator`** (Lead Architect) | System orchestration, GPL-3.0 compliance, CI/CD verification, and cross-team execution. | `antigravity-guide`, upstream Next Player specs. |
| **`@LayoutSpecialist`** (Mobile & Spatial UX) | Layout structure, proximity laws, responsive breakpoints, tablet adaptivity, touch ergonomics. | `better-layout`, `mobile-design`, `cursor-designer/ui-layout-and-density`. |
| **`@VisualDesigner`** (Neubrutalism & Visual Craft) | Surface textures, zero-blur shadows, color tokens, contrast (WCAG AA), optical alignment, anti-slop. | `better-ui`, `better-colors`, `unslop`, `taste-design`, `charles-unreasonable-craft`. |
| **`@PerformanceEngineer`** (Compose & System Insets) | Recomposition optimization, 144Hz high-refresh rendering, modifier order, system bar insets. | `jetpack-compose-performance`, `edge-to-edge`, Android Compose guidelines. |

---

## 2. Team Deliberation Log

### Entry 1: Session Kickoff & Scope Definition
**`@Coordinator`**:
> Team, we have cloned and indexed the newly acquired skill sets:
> - `jakubkrehel-skills` (`better-layout`, `better-ui`, `better-colors`, `better-accessibility`)
> - `spencergoldade/cursor-designer` (`ui-layout-and-density`, `design-core`, `accessibility-frontend`)
> - `mengto-skills` (layout grids and typography hierarchy)
> - Our native skills: `mobile-design`, `unslop`, `jetpack-compose-performance`, `edge-to-edge`.
> 
> Our previous release (`v0.21.8`) proved that our compilation, signing, and base Media3 playback pipelines pass CI cleanly on Google ATD API 35. However, user feedback and our code audits confirm that the UI is currently in an awkward transitional state: stock Material 3 Next Player screens with a few brutalist borders slapped on list items.
>
> We need a rigorous, coordinated overhaul of layout, aesthetics, and interaction mechanics. Let's hear from each specialist on what the code looks like right now and how the downloaded rules solve it.

---

### Entry 2: Spatial Layout & Ergonomics Audit
**`@LayoutSpecialist`**:
> I reviewed the layout rules from `better-layout` and `mobile-design` against our current screens:
> 
> 1. **Violation of the 2× Proximity Law (`better-layout` §1):**
>    - In `FolderItem.kt` and `MediaItem.kt`, the spacing between internal title text and metadata subtitle is `4dp`, but the vertical card spacing in `LazyColumn` is only `6dp`. 
>    - Because `6dp < 2 × 4dp`, the user's eye struggles to segment individual list items when scanning rapidly. The law states: *The gap between distinct groups must be at least double the gap inside a group.* We must elevate item spacing to `12dp` or `16dp`.
>
> 2. **Divider Overuse vs. Negative Space:**
>    - Stock Next Player relies on hairline horizontal divider lines (`HorizontalDivider`) between rows. Under `better-layout`, dividers are the *last resort*. We should separate items with negative space first and distinct brutalist container shapes second, dropping the thin gray dividers entirely.
>
> 3. **Tablet Adaptivity for Xiaomi Pad 6 (`mobile-design` §Window Size Classes):**
>    - In landscape on the Pad 6 (expanded width >840dp), having a bottom navigation bar stretches tabs across the entire bottom edge where thumbs can't reach them comfortably.
>    - Furthermore, the video grid currently defaults to fixed columns. We need an adaptive grid (`GridCells.Adaptive(minSize = 220.dp)`) so a 144Hz 11-inch tablet displays a comfortable 3-to-4 column cards grid, while phones stay at 2 columns.
>
> 4. **Touch Target Floor (`accessibility-touch.md`):**
>    - Several quick-action icons (e.g. overflow three-dots, filter chips) have visual sizes of 20–24dp without sufficient content padding to reach the **48×48dp** touch target floor. We must enforce `minimumInteractiveComponentSize()` or explicit `48.dp` constraints across all buttons.

---

### Entry 3: Visual Polish, Colors & The Neubrutalism Identity
**`@VisualDesigner`**:
> Building on `@LayoutSpecialist`'s structural points, let's address the aesthetic identity through `better-ui`, `better-colors`, and `unslop`:
> 
> 1. **Concentric Radii Formula (`better-ui` §Outer radius = inner radius + padding):**
>    - When we render thumbnail containers inside a brutalist card, our card has `cornerRadius = 10.dp` and inner items have arbitrary `8.dp`. 
>    - Geometrically, if a card has padding `P` and inner element radius `R_in`, the outer card radius must equal `R_in + P` for corners to remain concentric and visually pleasing.
>
> 2. **Neubrutalism Token Harmonization (`MizuStyle.kt`):**
>    - Right now, some screens still use stock `MaterialTheme.colorScheme.primaryContainer`. 
>    - Every surface in Mizu Neubrutalism should use our calibrated palette:
>      - Background: Paper White `#FFF8EE` (Light) / Pure Ink `#111111` (Dark)
>      - Outlines: Solid Ink 2dp (`#111111` in Light, `#333333` in Dark)
>      - Shadows: Hard offset 3dp with zero blur
>      - Accents: Sun Yellow `#FFD84D` (Folders), Aqua `#5CE1E6` (Videos), Lavender `#B9A4FF` (Audio), Lime `#B8F36B` (Playlists), Coral `#FF6B57` (Actions/Danger).
>
> 3. **The Scrubber & Player Controls HUD (`charles-unreasonable-craft`):**
>    - The current video player uses the standard Android Material slider. It feels flimsy and stock.
>    - We should design a bespoke brutalist scrubber: a solid 6dp track with an ink border, vibrant Sun-Yellow progress fill, and a chunky rectangular 16×24dp thumb indicator that grows tactilely on touch.
>    - Overlay buttons (Play/Pause, Seek forward/back) should use our `neoClickable` modifier with physical downward displacement (3dp) on press.

---

### Entry 4: Compose Performance & Inset Safety
**`@PerformanceEngineer`**:
> Let's look at the implementation realities in Jetpack Compose:
> 
> 1. **Modifier Ordering Law (`.cursorrules` & `better-layout`):**
>    - In Compose, modifier sequence is critical. The rule is:
>      ```
>      Modifier
>          .fillMaxWidth()                  // 1. Structural constraints
>          .neoShadow(...)                  // 2. Outer drop shadow
>          .border(...)                     // 3. Structural boundary
>          .clip(shape)                     // 4. Shape clipping
>          .background(color)               // 5. Fill
>          .clickable(...)                  // 6. Interaction (covers whole boundary)
>          .padding(internalPadding)        // 7. Internal spacing
>      ```
>    - Placing `.clickable()` *after* `.padding()` shrinks the touch area and drops below 48dp! Placing `.clickable()` before internal padding ensures the entire 48dp+ box responds to touch.
>
> 2. **144Hz Recomposition Prevention during Playback:**
>    - During video playback, the current playback position updates every 500ms to 1s.
>    - If we read `currentPosition` in a parent Composable, it forces the entire player overlay (all buttons, titles, track selectors) to recompose every second!
>    - We must defer state reads: pass lambda suppliers (e.g. `{ currentPosition }`) or use `derivedStateOf` directly inside the custom scrubber canvas draw phase (`drawWithCache` / `drawBehind`). The timer text should be isolated into a tiny leaf Composable.
>
> 3. **Edge-to-Edge System Bar Insets (`edge-to-edge`):**
>    - On Xiaomi Pad 6 (Android 14), we must consume `WindowInsets.safeDrawing` or `WindowInsets.systemBars`.
>    - During video playback, when the HUD appears, controls must be padded with `windowInsetsPadding(WindowInsets.safeDrawing)` so top action icons don't sit under the front camera hole and bottom controls don't conflict with the Android 14 gesture bar.

---

### Entry 5: Action Plan & Consensus
**`@Coordinator`**:
> Excellent. We have clear consensus across Layout, Visual Craft, and Performance. Here is our master execution roadmap:
> 
> 1. **Phase 1: Foundation Tokens & Spacing Scale**
>    - Update `MizuStyle.kt` to export explicit layout spacing tokens (`SpaceGroup = 16.dp`, `SpaceInternal = 8.dp`, `SpaceMicro = 4.dp`, `MinTouchTarget = 48.dp`).
>    - Codify the Concentric Corner Radius rule and Modifier ordering standard.
> 
> 2. **Phase 2: Video Library & Folder Item Spatial Polish**
>    - Enforce the 2x proximity law in `FolderItem.kt` and `MediaItem.kt`.
>    - Eliminate thin hairline dividers; replace with spaced brutalist cards.
>    - Configure adaptive grid columns for tablet vs phone.
> 
> 3. **Phase 3: Bespoke Player HUD & Scrubber**
>    - Replace the stock slider with the custom brutalist track & thumb.
>    - Isolate playback position reads to prevent 144Hz recomposition stutter.
>    - Wire `neoClickable` tactile press physics to transport controls (Play/Pause, Skip).
> 
> 4. **Phase 4: CI Verification & Instrumented Testing**
>    - Verify zero ktlint regressions.
>    - Run full emulator test suite on Google ATD API 35 to guarantee playback, subtitles, and navigation remain 100% stable.
>
> Let's initiate Phase 1 immediately.
