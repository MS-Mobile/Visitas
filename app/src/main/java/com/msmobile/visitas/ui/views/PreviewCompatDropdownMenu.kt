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
