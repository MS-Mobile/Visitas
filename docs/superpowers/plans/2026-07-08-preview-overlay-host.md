# Shared Preview Overlay Host Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the preview overlay-escape mechanism from `PreviewCompatDropdownMenu` into a neutral `PreviewOverlayHost` primitive and route all modal bottom sheets through a new `PreviewCompatModalSheet` so previews and screenshot tests render sheets at full modal fidelity (scrim + bottom dock).

**Architecture:** A single `SubcomposeLayout`-based host, active only under `LocalInspectionMode`, measures app content first (settling Scaffold's lazily-composed chrome so overlays self-register), then paints an optional scrim and the registered overlays on top in one layout pass. Two thin compat wrappers (dropdown menu, modal sheet) register entries carrying a placement (`TopBarAnchored` | `BottomDocked`) and a scrim flag; in production they delegate to the real Material components.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Compose Preview Screenshot Testing (`@PreviewTest`, `updateDebugScreenshotTest` / `validateDebugScreenshotTest`).

**Spec:** `docs/superpowers/specs/2026-07-08-preview-overlay-host-design.md`

**Verification note:** These changes render layoutlib previews, not asserted values. The "test" for each rendering change is its `@PreviewTest`: generate the reference PNG with `./gradlew updateDebugScreenshotTest`, **visually inspect the generated image**, then commit it. `validateDebugScreenshotTest` fails when no reference exists yet — that is the expected "red" state before generating.

---

## File Structure

- **Create** `app/src/main/java/com/msmobile/visitas/ui/views/PreviewOverlayHost.kt` — the shared primitive: `PreviewOverlayHost`, `PreviewOverlayHostState`, `LocalPreviewOverlayHost`, `OverlayPlacement`, slot enum, placement constants, scrim layer.
- **Modify** `app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatDropdownMenu.kt` — delete its private host/registry/placement; register with the shared host.
- **Create** `app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatModalSheet.kt` — drop-in for `ModalBottomSheet`.
- **Modify** `visit/VisitDetailScreen.kt`, `visit/VisitListScreen.kt`, `ui/views/PermissionRationaleSheet.kt`, `backup/BackupSheet.kt` — swap sheets onto the wrapper; wrap previews in `PreviewOverlayHost`.
- **Create** `app/src/screenshotTest/kotlin/com/msmobile/visitas/ui/views/PermissionRationaleSheetScreenshotTest.kt`.

---

## Task 1: Create the `PreviewOverlayHost` primitive

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/ui/views/PreviewOverlayHost.kt`

- [ ] **Step 1: Create the file with the full primitive**

```kotlin
package com.msmobile.visitas.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

// Where a top-bar-anchored overlay (e.g. a dropdown opened from a top-bar action) paints: below the
// top app bar, inset from the end edge. Placement is fixed rather than anchored to the trigger
// because the screenshot renderer does not settle the post-layout measurement a real anchor needs.
private val TOP_BAR_ANCHORED_TOP = 112.dp
private val TOP_BAR_ANCHORED_END_MARGIN = 8.dp

// Material's default modal scrim opacity over the app content behind a bottom sheet.
private const val SCRIM_ALPHA = 0.32f

/** Placement strategies for [PreviewOverlayHost] entries. Closed to the two cases actually in use. */
internal sealed interface OverlayPlacement {
    /** Below the top app bar, inset from the end edge. Used for dropdown menus. */
    data object TopBarAnchored : OverlayPlacement

    /** Full width, docked to the bottom edge. Used for modal bottom sheets. */
    data object BottomDocked : OverlayPlacement
}

/**
 * Full-screen overlay that paints preview-only compat popups (dropdown menus, modal sheets) above
 * everything else so previews/screenshots show them complete, escaping the app bar / card / window
 * boundary that would otherwise clip or drop them. Wrap a preview's root with it.
 *
 * Outside [LocalInspectionMode] it is a transparent pass-through, so it must not appear in
 * production chrome.
 *
 * It is a [SubcomposeLayout] rather than a plain overlay for ordering: the app content is measured
 * first, which drives Scaffold's lazily-composed chrome so overlays register themselves, and only
 * then are the overlays subcomposed from the now-populated registry — all in one layout pass, with
 * no reliance on a follow-up recomposition (which the screenshot renderer does not settle).
 */
@Composable
fun PreviewOverlayHost(content: @Composable () -> Unit) {
    if (!LocalInspectionMode.current) {
        content()
        return
    }
    val host = remember { PreviewOverlayHostState() }
    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val contentPlaceables = subcompose(PreviewOverlaySlot.Content) {
            CompositionLocalProvider(LocalPreviewOverlayHost provides host) {
                content()
            }
        }.map { it.measure(constraints) }

        val entries = host.entries.values.toList()

        val scrimPlaceables = if (entries.any { it.scrim }) {
            val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
            subcompose(PreviewOverlaySlot.Scrim) {
                Box(Modifier.fillMaxSize().background(scrimColor))
            }.map { it.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight)) }
        } else {
            emptyList()
        }

        val overlayPlaceables = subcompose(PreviewOverlaySlot.Overlays) {
            entries.forEach { it.content() }
        }.mapIndexed { index, measurable ->
            val entry = entries[index]
            val overlayConstraints = when (entry.placement) {
                OverlayPlacement.TopBarAnchored ->
                    constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Int.MAX_VALUE)
                OverlayPlacement.BottomDocked ->
                    constraints.copy(minWidth = constraints.maxWidth, minHeight = 0)
            }
            entry.placement to measurable.measure(overlayConstraints)
        }

        val top = TOP_BAR_ANCHORED_TOP.roundToPx()
        val endMargin = TOP_BAR_ANCHORED_END_MARGIN.roundToPx()
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            scrimPlaceables.forEach { it.place(0, 0) }
            overlayPlaceables.forEach { (placement, placeable) ->
                when (placement) {
                    OverlayPlacement.TopBarAnchored -> {
                        val x = (constraints.maxWidth - placeable.width - endMargin).coerceAtLeast(0)
                        placeable.place(x, top)
                    }
                    OverlayPlacement.BottomDocked -> {
                        val y = (constraints.maxHeight - placeable.height).coerceAtLeast(0)
                        placeable.place(0, y)
                    }
                }
            }
        }
    }
}

/**
 * Registry of currently-shown preview overlays to paint. Deliberately a plain (non-snapshot) map:
 * the screenshot renderer does not propagate snapshot writes made during composition to sibling
 * reads, so entries are collected during the single composition pass and read back in that same
 * pass. The host is remembered fresh per preview and keys are stable, so no clearing is needed.
 */
internal class PreviewOverlayHostState {
    val entries = LinkedHashMap<Any, Entry>()

    class Entry(
        val placement: OverlayPlacement,
        val scrim: Boolean,
        val content: @Composable () -> Unit,
    )
}

internal val LocalPreviewOverlayHost = staticCompositionLocalOf<PreviewOverlayHostState?> { null }

private enum class PreviewOverlaySlot { Content, Scrim, Overlays }
```

- [ ] **Step 2: Compile to verify it builds**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`PreviewOverlayHost` is not yet consumed; the old `PreviewCompatDropdownMenu.HostPreview` still exists and is untouched.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/ui/views/PreviewOverlayHost.kt
git commit -m "Add shared PreviewOverlayHost primitive"
```

---

## Task 2: Route `PreviewCompatDropdownMenu` through the shared host

Delete the dropdown's private host/registry/placement and register with `PreviewOverlayHost` instead. Migrate the two existing `HostPreview` call sites.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatDropdownMenu.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt` (preview only)
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt` (preview only)

- [ ] **Step 1: Rewrite `PreviewCompatDropdownMenu.kt`**

Replace the whole file with the version below. It keeps `invoke`, the renderer interface, both renderers, and `DropdownMenuSurface`; it removes `HostPreview`, `PreviewMenuHostState`, `LocalPreviewMenuHost`, `PreviewMenuSlot`, the menu-placement constants, and the `SubcomposeLayout`.

```kotlin
package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

// Mirrors the private DropdownMenuVerticalPadding the real menu popup applies around its content.
private val DROPDOWN_MENU_VERTICAL_PADDING = 8.dp

/**
 * Drop-in replacement for [androidx.compose.material3.DropdownMenu], invoked with the same call
 * syntax via [invoke].
 *
 * In production it delegates to the real [DropdownMenu], so behaviour is unchanged for users. Under
 * [LocalInspectionMode] the real menu can't be used because layoutlib does not paint
 * [androidx.compose.ui.window.Popup] content, so the same content is registered with the nearest
 * [PreviewOverlayHost] and painted there — above all other content, below the top app bar — so
 * previews/screenshots show the full menu instead of a slice clipped by the app bar or a card.
 */
object PreviewCompatDropdownMenu {

    /**
     * Renders the dropdown. Only shows content when [expanded] is true, matching [DropdownMenu];
     * drive it from the same state.
     */
    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        expanded: Boolean,
        properties: PopupProperties = PopupProperties(),
        onDismissRequest: () -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val renderer = if (LocalInspectionMode.current) PreviewDropdownMenuRenderer else MaterialDropdownMenuRenderer
        renderer.Render(
            modifier = modifier,
            expanded = expanded,
            properties = properties,
            onDismissRequest = onDismissRequest,
            content = content,
        )
    }
}

/**
 * Contract shared by the production and preview dropdown renderers. Because both implementations
 * override the same [Render] signature, they are forced to expose identical params and can't drift
 * apart. [PreviewCompatDropdownMenu] picks the implementation based on [LocalInspectionMode].
 */
private interface DropdownMenuRenderer {
    @Composable
    fun Render(
        modifier: Modifier,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        properties: PopupProperties,
        content: @Composable ColumnScope.() -> Unit,
    )
}

/** Production renderer: the real Material 3 [DropdownMenu], shown in a [androidx.compose.ui.window.Popup]. */
private object MaterialDropdownMenuRenderer : DropdownMenuRenderer {
    @Composable
    override fun Render(
        modifier: Modifier,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        properties: PopupProperties,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            content = content,
        )
    }
}

/**
 * Preview renderer: registers the menu with the enclosing [PreviewOverlayHost] instead of drawing
 * inline, so the host can paint it above the app bar / card that would otherwise clip it. Falls
 * back to an inline [Surface] when no host is present.
 */
private object PreviewDropdownMenuRenderer : DropdownMenuRenderer {
    @Composable
    override fun Render(
        modifier: Modifier,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        properties: PopupProperties,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val host = LocalPreviewOverlayHost.current

        if (host == null) {
            // No overlay in the tree: best-effort inline render so the menu isn't missing entirely.
            if (expanded) {
                DropdownMenuSurface(modifier = modifier, content = content)
            }
            return
        }

        if (!expanded) return

        // Register during composition. The host measures the app content before subcomposing the
        // overlay, so by the time it reads this map the (lazily-composed) app bar has registered.
        val id = remember { Any() }
        if (id !in host.entries) {
            host.entries[id] = PreviewOverlayHostState.Entry(
                placement = OverlayPlacement.TopBarAnchored,
                scrim = false,
                content = { DropdownMenuSurface(content = content) },
            )
        }
    }
}

/** Content of a preview dropdown, styled to mirror the real [DropdownMenu] surface. */
@Composable
private fun DropdownMenuSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MenuDefaults.shape,
        color = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .width(IntrinsicSize.Max)
                .padding(vertical = DROPDOWN_MENU_VERTICAL_PADDING),
            content = content,
        )
    }
}
```

- [ ] **Step 2: Update the two `HostPreview` call sites**

In `VisitDetailScreen.kt`, change `PreviewCompatDropdownMenu.HostPreview {` (around line 1327) to `PreviewOverlayHost {` and add the import `import com.msmobile.visitas.ui.views.PreviewOverlayHost` (the file already imports `PreviewCompatDropdownMenu` from that package).

In `VisitListScreen.kt`, change `PreviewCompatDropdownMenu.HostPreview {` (around line 1130) to `PreviewOverlayHost {`, add the same import, and replace the stale comment above it:

```kotlin
        // Preview-only: hosts the expanded overlays (filter menu) above the app bar, which would
        // otherwise clip them in screenshots. No-op / absent in production (see PreviewOverlayHost).
        PreviewOverlayHost {
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Regenerate dropdown screenshots and confirm unchanged**

Run: `./gradlew updateDebugScreenshotTest`
Then inspect `git diff --stat app/src/screenshotTest/**/reference*` (or the `reference` image dir). The existing dropdown-bearing references (VisitList filter menu, VisitDetail overflow menu) should be byte-identical or visually identical — the refactor is behavior-preserving for menus. Visually confirm no menu regressed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatDropdownMenu.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt \
        app/src/screenshotTestDebug/reference
git commit -m "Route PreviewCompatDropdownMenu through shared PreviewOverlayHost"
```

(Adjust the reference-image path to this project's actual screenshot output dir if different.)

---

## Task 3: Create `PreviewCompatModalSheet`

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatModalSheet.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Drop-in replacement for [androidx.compose.material3.ModalBottomSheet], invoked with the same call
 * syntax via [invoke]. It shows whenever it is composed (like the real sheet), so callers keep their
 * existing visibility gating around it.
 *
 * In production it delegates to the real [ModalBottomSheet]. Under [LocalInspectionMode] the real
 * sheet can't be used because layoutlib does not paint its detached window, so the content is
 * registered with the nearest [PreviewOverlayHost] and painted there — docked to the bottom, over a
 * scrim — so previews/screenshots show the sheet reliably instead of intermittently or not at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
object PreviewCompatModalSheet {

    @Composable
    operator fun invoke(
        onDismissRequest: () -> Unit,
        sheetState: SheetState = rememberModalBottomSheetState(),
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val renderer = if (LocalInspectionMode.current) PreviewModalSheetRenderer else MaterialModalSheetRenderer
        renderer.Render(onDismissRequest = onDismissRequest, sheetState = sheetState, content = content)
    }
}

/**
 * Contract shared by the production and preview sheet renderers so their signatures can't drift.
 * [PreviewCompatModalSheet] picks the implementation based on [LocalInspectionMode].
 */
@OptIn(ExperimentalMaterial3Api::class)
private interface ModalSheetRenderer {
    @Composable
    fun Render(
        onDismissRequest: () -> Unit,
        sheetState: SheetState,
        content: @Composable ColumnScope.() -> Unit,
    )
}

/** Production renderer: the real Material 3 [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
private object MaterialModalSheetRenderer : ModalSheetRenderer {
    @Composable
    override fun Render(
        onDismissRequest: () -> Unit,
        sheetState: SheetState,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            content = content,
        )
    }
}

/**
 * Preview renderer: registers the sheet with the enclosing [PreviewOverlayHost] (bottom-docked, over
 * a scrim) instead of drawing a window. Falls back to an inline surface when no host is present.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object PreviewModalSheetRenderer : ModalSheetRenderer {
    @Composable
    override fun Render(
        onDismissRequest: () -> Unit,
        sheetState: SheetState,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val host = LocalPreviewOverlayHost.current

        if (host == null) {
            ModalSheetSurface(content = content)
            return
        }

        val id = remember { Any() }
        if (id !in host.entries) {
            host.entries[id] = PreviewOverlayHostState.Entry(
                placement = OverlayPlacement.BottomDocked,
                scrim = true,
                content = { ModalSheetSurface(content = content) },
            )
        }
    }
}

/** Content of a preview sheet, styled to mirror the real [ModalBottomSheet] surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalSheetSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BottomSheetDefaults.DragHandle()
            }
            content()
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (Not yet consumed by any call site.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/ui/views/PreviewCompatModalSheet.kt
git commit -m "Add PreviewCompatModalSheet drop-in for ModalBottomSheet"
```

---

## Task 4: Migrate `PhoneOptionsSheet` (VisitDetailScreen)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt`

- [ ] **Step 1: Swap the sheet**

In `PhoneOptionsSheet` (around line 303) change:

```kotlin
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
```

to:

```kotlin
    PreviewCompatModalSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
```

Add `import com.msmobile.visitas.ui.views.PreviewCompatModalSheet`. Remove `import androidx.compose.material3.ModalBottomSheet` (now unused; `rememberModalBottomSheetState` import stays). The `PreviewOverlayHost` wrap in `VisitDetailScreenPreview` and the `showPhoneOptionsSheet = true` config already exist from Task 2 / the preview config, so no further wiring is needed.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Regenerate and inspect the screenshot**

Run: `./gradlew updateDebugScreenshotTest`
Inspect the `VisitDetailScreenshotTest` reference image for the config with `showPhoneOptionsSheet = true`: the phone-options sheet must appear docked to the bottom, full width, with a drag handle and a dimmed scrim over the screen behind it.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt app/src/screenshotTestDebug/reference
git commit -m "Render PhoneOptionsSheet via PreviewCompatModalSheet in previews"
```

---

## Task 5: Migrate `BibleStudentsSheet` (VisitListScreen)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt`

- [ ] **Step 1: Swap the sheet**

In `BibleStudentsSheet` (around line 547), inside the existing `AnimatedVisibility(visible = isVisible)`, change:

```kotlin
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
```

to:

```kotlin
        PreviewCompatModalSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
```

Add `import com.msmobile.visitas.ui.views.PreviewCompatModalSheet`. Remove `import androidx.compose.material3.ModalBottomSheet` (unused now). The preview host wrap and the `isBibleStudentsSheetVisible = true` config already exist.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Regenerate and inspect the screenshot**

Run: `./gradlew updateDebugScreenshotTest`
Inspect the `VisitListScreenshotTest` reference for the config with `isBibleStudentsSheetVisible = true`: the Bible-students sheet must appear bottom-docked over a scrim, full width, with a drag handle.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt app/src/screenshotTestDebug/reference
git commit -m "Render BibleStudentsSheet via PreviewCompatModalSheet in previews"
```

---

## Task 6: Migrate `PermissionRationaleSheet` and add its preview + screenshot test

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/ui/views/PermissionRationaleSheet.kt`
- Create: `app/src/screenshotTest/kotlin/com/msmobile/visitas/ui/views/PermissionRationaleSheetScreenshotTest.kt`

- [ ] **Step 1: Swap the sheet**

In `PermissionRationaleSheet` (around line 41), inside the existing `AnimatedVisibility`, change `ModalBottomSheet(` to `PreviewCompatModalSheet(` (same `onDismissRequest` / `sheetState` args). No import needed — same package. Remove `import androidx.compose.material3.ModalBottomSheet` (unused; `rememberModalBottomSheetState` stays).

- [ ] **Step 2: Add a preview (open state) at the bottom of the file**

Add these imports:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.ui.tooling.preview.Preview
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
```

Add the preview composable (mirrors the args the VisitList call site passes — `VisitListScreen.kt:294`):

```kotlin
@PreviewPhone
@Composable
internal fun PermissionRationaleSheetPreview() {
    VisitasTheme {
        PreviewOverlayHost {
            PermissionRationaleSheet(
                icon = Icons.Rounded.LocationOn,
                message = stringResource(R.string.location_permission_message),
                isVisible = true,
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
```

(`stringResource` and `R` are already imported in this file.)

- [ ] **Step 3: Create the screenshot test**

```kotlin
package com.msmobile.visitas.ui.views

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class PermissionRationaleSheetScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun PermissionRationaleSheetPreviewTest() {
        PermissionRationaleSheetPreview()
    }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugScreenshotTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Generate and inspect the new reference**

Run: `./gradlew updateDebugScreenshotTest`
Inspect the new `PermissionRationaleSheetScreenshotTest` reference: the rationale sheet (location icon, message, Cancel / Grant buttons) must appear bottom-docked over a scrim with a drag handle.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/ui/views/PermissionRationaleSheet.kt \
        app/src/screenshotTest/kotlin/com/msmobile/visitas/ui/views/PermissionRationaleSheetScreenshotTest.kt \
        app/src/screenshotTestDebug/reference
git commit -m "Render PermissionRationaleSheet via PreviewCompatModalSheet; add screenshot test"
```

---

## Task 7: Migrate `BackupSheet` and retire its content-extraction workaround

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/backup/BackupSheet.kt`

- [ ] **Step 1: Swap the sheet**

In `BackupSheet` (around line 48), inside the existing `AnimatedVisibility`, change `ModalBottomSheet(` to `PreviewCompatModalSheet(` (same args). Add `import com.msmobile.visitas.ui.views.PreviewCompatModalSheet`. Remove `import androidx.compose.material3.ModalBottomSheet` (unused; `rememberModalBottomSheetState` stays).

- [ ] **Step 2: Rewrite `BackupScreenPreview` to render the real sheet**

Replace the existing `BackupScreenPreview` (around line 162) so it drives the actual `BackupSheet` through the overlay host, instead of calling `BackupScreenContent` directly:

```kotlin
@VisibleForTesting
@PreviewPhone
@PreviewFoldable
@Composable
internal fun BackupScreenPreview(
    @PreviewParameter(BackupSheetPreviewConfigProvider::class) config: BackupSheetPreviewConfig
) {
    VisitasTheme(config.isDarkMode) {
        PreviewOverlayHost {
            AppScaffold(
                uiState = config.mainActivityUiState,
                currentDestination = VisitListScreenDestination,
                onEvent = {},
                onNavigateToTab = {},
                onNavigate = {}
            ) {
                BackupSheet(
                    isVisible = true,
                    uiState = config.backupUiState,
                    onBackupSheetEvent = {},
                    onDismiss = {},
                )
            }
        }
    }
}
```

Add `import com.msmobile.visitas.ui.views.PreviewOverlayHost`. `BackupScreenContent` stays as the sheet's private body (still called by `BackupSheet`).

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugScreenshotTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Regenerate and inspect**

Run: `./gradlew updateDebugScreenshotTest`
Inspect the `BackupSheetScreenshotTest` reference: it now shows the real backup sheet docked to the bottom over a scrim (previously it showed the content inline in the scaffold body). Confirm the buttons and any snackbar state still render.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/backup/BackupSheet.kt app/src/screenshotTestDebug/reference
git commit -m "Render BackupSheet via PreviewCompatModalSheet; retire content-extraction workaround"
```

---

## Task 8: Full verification pass

**Files:** none (verification + reference lock-in)

- [ ] **Step 1: Validate all screenshot tests against committed references**

Run: `./gradlew validateDebugScreenshotTest`
Expected: PASS for every test, including the four migrated sheets and the new `PermissionRationaleSheetScreenshotTest`. If any reference is stale, run `./gradlew updateDebugScreenshotTest`, re-inspect, and commit.

- [ ] **Step 2: Confirm production is unchanged (release build + unit tests)**

Run: `./gradlew :app:compileReleaseKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL / tests pass. The wrappers delegate to the real `DropdownMenu` / `ModalBottomSheet` outside inspection mode, so runtime behavior is unchanged.

- [ ] **Step 3: Confirm no lingering references to the old API**

Run: `git grep -n "HostPreview"` and `git grep -n "ModalBottomSheet(" -- app/src/main`
Expected: no `HostPreview` matches anywhere; no `ModalBottomSheet(` matches in `app/src/main` except inside `PreviewCompatModalSheet.kt`'s `MaterialModalSheetRenderer`.

- [ ] **Step 4: Final commit if anything changed**

```bash
git add -A
git commit -m "Lock in preview overlay host screenshot references"
```

---

## Self-Review

**Spec coverage:**
- New `PreviewOverlayHost` primitive (host, state, local, `OverlayPlacement`, scrim, single-pass layout) → Task 1. ✓
- `PreviewCompatDropdownMenu` refactor to register `TopBarAnchored`, `HostPreview` deleted → Task 2. ✓
- `PreviewCompatModalSheet` drop-in, `BottomDocked` + scrim, `BottomSheetDefaults` surface, no-host fallback → Task 3. ✓
- Four call-site migrations → Tasks 4 (VisitDetail), 5 (VisitList), 6 (PermissionRationale), 7 (Backup). ✓
- Host wrapping / stale comment / existing open-state configs → Tasks 2, 4, 5. ✓
- Retire BackupSheet workaround → Task 7. ✓
- New PermissionRationaleSheet preview + screenshot test → Task 6. ✓
- Screenshot regeneration + production-unchanged verification → each task + Task 8. ✓

**Type consistency:** `PreviewOverlayHostState.Entry(placement, scrim, content)` is constructed identically in Tasks 2 and 3; `content` is `@Composable () -> Unit` in the registry and both wrappers wrap their `ColumnScope` content into a single-root surface before registering, matching the host's 1-measurable-per-entry assumption. `OverlayPlacement.TopBarAnchored` / `BottomDocked` names are consistent across Tasks 1–3. `PreviewOverlayHost` (public) vs `PreviewOverlayHostState` / `LocalPreviewOverlayHost` / `OverlayPlacement` (internal, same module) — all consumers are in-module.

**Placeholder scan:** no TBD/TODO/"handle edge cases"; every code step shows full code.

**Note for executor:** reference-image paths in `git add` lines assume the AGP screenshot output under `app/src/screenshotTestDebug/reference`. Confirm this project's actual reference directory (check where existing `*ScreenshotTest` PNGs live) and adjust the `git add` paths accordingly.
