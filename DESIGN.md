# MIZU — DESIGN SYSTEM & VISUAL ARCHITECTURE (DESIGN.md)

## 1. Visual Philosophy: Neubrutalism
Mizu embraces a contemporary **Neubrutalism** aesthetic: bold, tactile, legible, and unapologetically physical. Unlike sterile flat designs or generic soft-glow interfaces, Mizu uses high-contrast ink boundaries, confident color blocking, and sharp geometric shadows.

### Core Visual Tenets:
- **Defined Ink Contours**: Interactive elements and cards are bounded by solid 1.5.dp - 2.5.dp ink borders (`#111111`).
- **Tactile Hard Shadows**: Elevations use crisp offset shadows (e.g., `4.dp` offset X and Y with zero blur) rather than diffuse blurs.
- **Vibrant Accent Geometry**: Signature **Sun-Yellow** (`#FFD84D`) serves as the hero identity color, balanced with crisp ink tones.
- **Instant Readability**: Heavy typography, high-contrast badges, and unmistakable states.

---

## 2. Color System & Contrast Mathematics

### Primary Palette
| Token | Light Theme | Dark Theme | Purpose |
| :--- | :--- | :--- | :--- |
| `MizuPrimary` | `#FFD84D` (Sun-Yellow) | `#FFD84D` (Sun-Yellow) | Hero accent, primary actions, FAB |
| `MizuOnPrimary` | `#111111` (Deep Ink) | `#111111` (Deep Ink) | Text/icons on primary surfaces (Ratio: **13.2:1** — WCAG AAA) |
| `MizuBackground` | `#FBFBF9` (Warm Paper) | `#121212` (Midnight Ink) | Root screen canvas |
| `MizuSurface` | `#FFFFFF` (Pure White) | `#1E1E1E` (Card Surface) | Cards, sheets, dialog containers |
| `MizuOutline` | `#111111` (Deep Ink) | `#444444` (Steel Outline) | Borders, dividers, frame definitions |
| `MizuTextPrimary` | `#111111` (Deep Ink) | `#F5F5F5` (Off-White) | Titles, headings, main labels (Ratio: **18.5:1** — WCAG AAA) |
| `MizuTextSecondary`| `#555555` (Muted Ink) | `#A3A3A3` (Muted Silver) | Subtitles, duration badges, metadata |

### Semantic Colors
- **Success**: `#22C55E` (Emerald Green)
- **Warning**: `#F59E0B` (Amber Orange)
- **Danger**: `#EF4444` (Ruby Red)
- **Secondary Accent**: `#2DE2E6` (Electric Teal)

---

## 3. Elevation, Borders & Shapes
- **Border Stroke**:
  - Cards: `BorderStroke(2.dp, MizuOutline)`
  - Buttons / Inputs: `BorderStroke(2.dp, MizuOutline)`
  - Chips / Badges: `BorderStroke(1.5.dp, MizuOutline)`
- **Corner Radii**:
  - Buttons & Dialogs: `12.dp`
  - Cards & Video Tiles: `16.dp`
  - Small Badges & Chips: `8.dp`
  - FAB & Circular Actions: `CircleShape`
- **Offset Drop Shadows**:
  - Standard Card: `shadowOffset = 4.dp`, color = `#111111`
  - Hover / Focus: `shadowOffset = 6.dp`
  - Pressed State: `shadowOffset = 0.dp` (tactile button press simulation)

---

## 4. Typography Hierarchy
- **Display / Title**: Extra-bold sans-serif with tight tracking (`LetterSpacing = -0.5.sp`).
- **Body**: Medium-weight sans-serif engineered for effortless scanning.
- **Numbers & Timecodes**: Monospace / tabular numbers (`FontFeatureSettings = "tnum"`) to eliminate jitter during video scrubbing.

---

## 5. Skin System Architecture & Roadmap
Mizu is built to support hot-swappable visual themes while keeping business logic and playback decoupled:
1. **Neubrutalism (Flagship Default)**: Full ink borders, offset shadows, bold geometry, Sun-Yellow accents.
2. **Material You**: Dynamic wallpaper-derived colors via Material3 palette extraction.
3. **Minimalist Dark**: Borderless, deep OLED blacks (`#000000`), subtle micro-dividers.
4. **Glass / Frosted**: Translucent panels with background blur and gradient highlights.
