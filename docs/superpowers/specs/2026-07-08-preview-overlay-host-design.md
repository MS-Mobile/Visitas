# Shared Preview Overlay Host — Design

**Date:** 2026-07-08
**Status:** Approved (pending spec review)

## Problem

`ModalBottomSheet` renders its content into a separate platform window
(`ModalBottomSheetWindow`, attached to the `WindowManager`) — architecturally
the same as the `Popup` that `DropdownMenu` uses. layoutlib, which backs both
interactive Compose previews and Compose Preview Screenshot tests
(`@PreviewTest`), does not composite those detached windows. Consequences:

- **Interactive previews:** sheets show only intermittently — Studio's preview
  loop occasionally catches a frame after the sheet window has painted; it is a
  race.
- **Screenshot tests:** sheets never appear — `@PreviewTest` captures a single
  settled layoutlib frame with no window compositing.

This is the identical limitation `PreviewCompatDropdownMenu` was built to escape.
Its `HostPreview` overlay solves it for menus, but the overlay-escape mechanism
lives inside a dropdown-specific object, so sheets cannot reuse it.

## Goal

Promote the reusable overlay-escape mechanism into a neutral primitive that both
the dropdown menu and modal sheet compat wrappers consume, and migrate all modal
sheet call sites onto it so previews and screenshots render sheets at **full
modal fidelity** (scrim over the whole screen + sheet docked to the bottom edge,
escaping any parent clipping).

Non-goals: no change to production behavior (wrappers delegate to the real
Material components outside inspection mode); no open-ended overlay framework
(only the two placements we actually have).

## Architecture

### New file: `ui/views/PreviewOverlayHost.kt`

The promoted primitive (renamed from `PreviewCompatDropdownMenu.HostPreview`):

- **`PreviewOverlayHost(content: @Composable () -> Unit)`** — outside
  `LocalInspectionMode`, a transparent pass-through (must not appear in
  production chrome). Inside inspection mode, a single-pass `SubcomposeLayout`
  that measures the app content first (settling Scaffold's lazily-composed
  chrome so overlays register themselves during composition), then subcomposes
  the registered overlays on top — in one layout pass, with no reliance on a
  follow-up recomposition that the screenshot renderer never settles.
- **`PreviewOverlayHostState`** — the registry. Deliberately a plain
  (non-snapshot) `LinkedHashMap<Any, Entry>`: the screenshot renderer does not
  propagate snapshot writes made during composition to sibling reads, so entries
  are collected and read back in the same pass. Remembered fresh per preview with
  stable keys, so no clearing is needed.
- **`LocalPreviewOverlayHost`** — `staticCompositionLocalOf<PreviewOverlayHostState?>`.
- **`OverlayPlacement`** — a sealed type with exactly two cases:
  - `TopBarAnchored` — below the top app bar, inset from the end edge (today's
    menu placement; `PREVIEW_MENU_TOP` / `PREVIEW_MENU_END_MARGIN` move here).
  - `BottomDocked` — full screen width, bottom-aligned.
- **`Entry(content, placement, scrim: Boolean)`** — one registered overlay.

### Layout pass (inside `PreviewOverlayHost`)

1. Subcompose + measure app content (drives chrome/sheet registration).
2. If **any** entry has `scrim = true`, subcompose + measure one full-screen
   scrim layer (Material scrim color/alpha).
3. Subcompose + measure overlays with **per-placement constraints**:
   `TopBarAnchored` → wrap (unbounded height, as today); `BottomDocked` →
   full width, wrap height.
4. Place in z-order **content → scrim → overlays**:
   - `TopBarAnchored`: `x = maxWidth − placeable.width − endMargin`, `y = top`.
   - `BottomDocked`: `x = 0`, `y = maxHeight − placeable.height`.

Multiple overlays coexist: a screen with both a menu and a sheet open renders
both, with one shared scrim behind the sheet.

### `ui/views/PreviewCompatDropdownMenu.kt` (refactor)

Keeps its public `invoke` and production/inspection renderer split. Its
inspection renderer now registers `Entry(placement = TopBarAnchored,
scrim = false)` with the shared `PreviewOverlayHost` instead of owning the
overlay. `HostPreview` and the private overlay/registry/placement code are
deleted from this file.

### New file: `ui/views/PreviewCompatModalSheet.kt`

A drop-in replacement for `androidx.compose.material3.ModalBottomSheet`, same
call syntax via `invoke`. Params mirror the subset in use:
`onDismissRequest`, `sheetState`, `content` (no visibility param — it shows
whenever composed, exactly like the real sheet, so callers keep their existing
`AnimatedVisibility` / `if` gating and migration is a one-line swap).

- Production renderer: delegates to the real `ModalBottomSheet`.
- Inspection renderer: registers `Entry(placement = BottomDocked, scrim = true)`.
  Its inline surface mirrors `BottomSheetDefaults` — expanded shape, container
  color, a `DragHandle` on top. Falls back to an inline surface when no host is
  present (parallel to the dropdown's no-host fallback).

Both wrappers share a renderer-interface pattern so the production and inspection
implementations expose identical params and cannot drift.

## Call-site migration

All four `ModalBottomSheet` sites migrate; the BackupSheet content-extraction
workaround is retired for a single consistent pattern.

| Site | File | Change |
|------|------|--------|
| PhoneOptionsSheet | `visit/VisitDetailScreen.kt:303` | `ModalBottomSheet(` → `PreviewCompatModalSheet(` |
| BibleStudentsSheet | `visit/VisitListScreen.kt:547` | `ModalBottomSheet(` → `PreviewCompatModalSheet(` |
| PermissionRationaleSheet | `ui/views/PermissionRationaleSheet.kt:41` | `ModalBottomSheet(` → `PreviewCompatModalSheet(` |
| BackupSheet | `backup/BackupSheet.kt:48` | `ModalBottomSheet(` → `PreviewCompatModalSheet(` |

Host wrapping / previews:

- Rename `PreviewCompatDropdownMenu.HostPreview` → `PreviewOverlayHost` at its two
  existing call sites (`VisitDetailScreenPreview`, `VisitListScreenPreview`) and
  update the stale comment in `VisitListScreen.kt:1128`. These previews already
  wrap correctly, so PhoneOptions and BibleStudents sheets render through the
  overlay automatically (their configs already open them:
  `showPhoneOptionsSheet = true`, `isBibleStudentsSheetVisible = true`).
- **Retire BackupSheet workaround:** change `BackupScreenPreview`
  (`backup/BackupSheet.kt:162`) to render the real `BackupSheet(isVisible = true,
  …)` inside `PreviewOverlayHost` + `AppScaffold`, instead of calling
  `BackupScreenContent` directly. `BackupScreenContent` stays as the sheet's
  private body.
- **New coverage for PermissionRationaleSheet:** it is a reusable `ui/views`
  component with no existing preview. Add a standalone
  `PermissionRationaleSheetPreview` (open state, wrapped in `PreviewOverlayHost`),
  mirroring the `DateTimePicker` / `RestoreBackupDialog` preview pattern.

## Testing & verification

- **Screenshot tests** (`@PreviewTest`, layoutlib-backed):
  - `VisitDetailScreenshotTest` and `VisitListScreenshotTest` now capture their
    sheets open; reference PNGs regenerate.
  - `BackupSheetScreenshotTest` keeps pointing at `BackupScreenPreview`, now
    routed through the real sheet.
  - Add `PermissionRationaleSheetScreenshotTest`.
  - Reference images regenerated via `./gradlew updateDebugScreenshotTest` and
    committed; validated with `./gradlew validateDebugScreenshotTest`.
- **Production unchanged:** wrappers delegate to the real components outside
  inspection mode; a release build plus existing unit/instrumentation tests
  confirm no behavior change.
- **Manual preview check:** open each migrated preview in the IDE and confirm the
  sheet paints reliably (no intermittency) before regenerating references.

## Out of scope

- `VisitListScreen`'s `showVisitMapSheet` (line 278) is not a `ModalBottomSheet`
  and is unaffected.
- No new overlay placements beyond `TopBarAnchored` and `BottomDocked`.
