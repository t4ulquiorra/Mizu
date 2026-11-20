# The Crate — Next Player Design System Spec & Architecture

## 1. Design Thesis & Visual Identity
"The Crate" transforms Next Player from a standard Android Material UI media player into an editorial, tactile, high-depth experience inspired by digging through physical crates of vinyl records, paper archive files, and analog studio consoles.

- **Primary Metaphor:** Cardboard crates, manila archive folders, stamped library index cards, tactile mechanical keycaps, and precision analog ruler dials.
- **Palette Identity:** Warm manila paper tones layered over deep soot-black (`#0D0C0A`), highlighted with International Orange (`#FF5A2E`) and OFL typography (Bricolage Grotesque & JetBrains Mono).
- **Physical Depth:** Real 3D depth rendered natively in Jetpack Compose via `graphicsLayer` (rotationX, rotationY, translationY, scale, cameraDistance, and dynamic zIndex sorting).

---

## 2. Strict Design Tokens

### Background Surfaces
| Token | Hex | Role |
|---|---|---|
| `CrateBgBase` | `#0D0C0A` | Deepest root background canvas |
| `CrateBgDeep` | `#14120D` | Recessed containers, letterbox framing |
| `CrateBgSurface` | `#17150F` | Standard card and list item containers |
| `CrateBgSurfaceKey` | `#1B1812` | Elevated tactile keycaps, toolbars, and docks |

### Borders & Dividers
| Token | Hex | Role |
|---|---|---|
| `CrateBorder` | `#26221A` | Subtle dividers and default outlines |
| `CrateBorderStrong` | `#2E2819` | Keycap rims, container frames |
| `CrateBorderInput` | `#3A3220` | Focus outlines, active edges |

### Ink & Contrast
| Token | Hex | Role |
|---|---|---|
| `CrateInkPrimary` | `#ECE4D2` | High-contrast headers, active labels |
| `CrateInkSecondary` | `#B9B09B` | Body copy, secondary metadata |
| `CrateInkMuted` | `#8F8774` | Subtitles, disabled indicators |
| `CrateInkDim` | `#6F6858` | Inactive tick marks, subtle details |
| `CrateInkPaper` | `#1A150C` | Dense ink printed directly on manila paper |

### Manila Paper Series (Cycled by Folder Index)
| Token | Hex | Shade |
|---|---|---|
| `CratePaper0` | `#D9CBA6` | Warm Manila Cream |
| `CratePaper1` | `#CDBE96` | Aged Tan Manila |
| `CratePaper2` | `#E2D6B8` | Light Parchment |
| `CratePaper3` | `#C4B48A` | Dark Kraft Cardstock |
| `CrateStampRed`| `#C23A12` | Official Archive Rubber Stamp Red |

### Accent Palette (User-Selectable)
- **International Orange (Default):** `#FF5A2E` (Shadow: `#7A2A12`)
- **Acid Lime:** `#C8F23D` (Shadow: `#4B660B`)
- **Seafoam:** `#7FE7D0` (Shadow: `#1B6E5B`)
- **Amber:** `#FFC247` (Shadow: `#7A5212`)

---

## 3. Typography & OFL Local Resources

Both font families are bundled locally as OFL-licensed `.ttf` assets in `core/ui/src/main/res/font/`:
1. **Bricolage Grotesque** (`bricolage_grotesque.ttf`):
   - Used for screen titles, folder names, and display hero headers.
   - Distinctive brutalist proportions with expressive geometric curves.
2. **JetBrains Mono** (`jetbrains_mono.ttf`):
   - Used for metadata, file extensions, duration, resolution, timecodes, breadcrumbs, and badges.
   - Small caps formatting (`LIBRARY · 18 FOLDERS`) with letter-spacing (+1.5sp to +2.0sp).

---

## 4. 3D Coordinate Depth & Motion Math

### Crate Folder Stack (Home Screen)
For folder at relative distance $d = index - scrollPosition$:
- **Front folder ($d = 0$):** `scale = 1.0`, `translationY = 0dp`, `rotationX = 0°`, `alpha = 1.0`.
- **Folders behind ($d > 0$):**
  $$\text{scale}(d) = 1.0 - (d \times 0.045)$$
  $$\text{translationY}(d) = -(d \times 48\text{dp})$$
  $$\text{rotationX}(d) = 5.0^\circ \times d$$
  $$\text{organicOffsetX}(d, i) = \text{hashScatter}(i) \times \min(d, 1.0)$$
  $$\text{organicRotationZ}(d, i) = \text{hashTilt}(i) \times \min(d, 1.0)$$
  $$\text{alpha}(d) = (1.0 - (d - 2.5) \times 0.8)\text{.coerceIn}(0.0, 1.0)$$
  $$\text{zIndex}(d) = 1000f - d$$
- **Folders passed ($d < 0$):**
  Fly forward and up with negative X rotation:
  $$\text{translationY}(d) = d \times 320\text{dp}$$
  $$\text{rotationX}(d) = d \times 12^\circ$$
  $$\text{alpha}(d) = 1.0 + d$$

### Arc Carousel (Folder Interior)
For video item at relative index offset $d = index - scrollPosition$:
- **Radius of Curvature:** $R \approx 1400\text{px}$
- **Angular Offset:** $\theta = d \times 0.26\text{ radians}$
- **Lateral Translation:** $x = R \sin(\theta)$
- **Depth Scale & Rotation:**
  $$\text{rotationY} = -\theta \times \frac{180}{\pi} \times 0.65$$
  $$\text{scale} = 1.0 - |d| \times 0.08$$
  $$\text{alpha} = (1.0 - |d| \times 0.22)\text{.coerceIn}(0.2, 1.0)$$

---

## 5. Tactile UI Components

1. **`CrateTactileButton`:**
   - 3D tactile button with a hard 6dp offset bottom shadow in `#7A2A12`.
   - On press, the button face shifts down by 4dp, compressing the shadow to 2dp.
2. **`CratePaperCard`:**
   - Cycles through the 4 manila paper colors by item index.
   - Renders manila folder tabs in one of 4 staggered slots with index badge.
3. **`CrateStampBadge`:**
   - Rotated $-6^\circ$ rubber-stamp badge in `#C23A12` with dashed border and uppercase mono label.
4. **`CrateHandOfCards`:**
   - Fanned 4-card history shelf with continuous rotation ($-6^\circ, -2^\circ, +2^\circ, +6^\circ$) and staggered elevation.
5. **`CrateToggle`:**
   - Tactile hardware toggle switch matching `.toggle-sw` in the HTML spec.

---

## 6. Architecture & Non-Negotiables Compliance

- **Zero Regression:** All existing features remain fully reachable (Folders, Videos, Tree View, Sorting, Filtering, Multi-Selection Dock, Subtitles, Audio Tracks, PiP, Vault, Playlists, History, Network SMB/WebDAV, Settings).
- **Zero Backend Touched:** Database Room models, DataStore preferences repositories, media scanners, and Media3 player engines remain strictly unmodified.
- **Adaptive Screen Density:**
  - 96dp persistent Left Rail on landscape/tablet/TV.
  - Keycap dock navigation bar on mobile portrait.
  - Accessible TalkBack stepper buttons and explicit semantics properties preserved across all components.
