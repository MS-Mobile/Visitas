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
