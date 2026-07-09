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
    // Read outside SubcomposeLayout's measure lambda: that lambda is a plain MeasureScope callback,
    // not a composable context, so a composable getter like MaterialTheme.colorScheme cannot be
    // called from within it.
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val contentPlaceables = subcompose(PreviewOverlaySlot.Content) {
            CompositionLocalProvider(LocalPreviewOverlayHost provides host) {
                content()
            }
        }.map { it.measure(constraints) }

        val entries = host.entries.values.toList()

        val scrimPlaceables = if (entries.any { it.scrim }) {
            subcompose(PreviewOverlaySlot.Scrim) {
                Box(Modifier.fillMaxSize().background(scrimColor))
            }.map { it.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight)) }
        } else {
            emptyList()
        }

        // Wrap each entry in a single-node Box so exactly one measurable is produced per entry,
        // keeping overlayPlaceables aligned index-for-index with entries regardless of what an
        // entry's content emits.
        val overlayPlaceables = subcompose(PreviewOverlaySlot.Overlays) {
            entries.forEach { entry -> Box { entry.content() } }
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
        // Placement is per-kind, not per-entry: entries sharing a placement land at the same
        // coordinates and overlap. Real previews register at most one overlay per placement (a
        // single open menu / a single open sheet), so this is sufficient; revisit if a preview
        // ever needs two simultaneous overlays of the same kind.
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
