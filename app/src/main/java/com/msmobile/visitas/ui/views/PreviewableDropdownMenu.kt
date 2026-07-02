package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

// Mirrors the private DropdownMenuVerticalPadding the real menu popup applies around its content.
private val DROPDOWN_MENU_VERTICAL_PADDING = 8.dp

/**
 * Drop-in replacement for [androidx.compose.material3.DropdownMenu].
 *
 * In production it delegates to the real [DropdownMenu], so behaviour is unchanged for users. Under
 * [LocalInspectionMode] it renders the same [content] inline. Only shows content when [expanded] is
 * true, matching [DropdownMenu]; drive it from the same state.
 */
@Composable
fun PreviewableDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    properties: PopupProperties = PopupProperties(),
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val renderer = if (LocalInspectionMode.current) PreviewDropdownMenu else RealDropdownMenu
    renderer.Render(
        modifier = modifier,
        expanded = expanded,
        properties = properties,
        onDismissRequest = onDismissRequest,
        content = content,
    )
}

/**
 * Contract shared by the production and preview dropdown renderers. Because both implementations
 * override the same [Render] signature, they are forced to expose identical params and can't drift
 * apart. [PreviewableDropdownMenu] picks the implementation based on [LocalInspectionMode].
 */
interface DropdownMenuRenderer {
    @Composable
    fun Render(
        modifier: Modifier,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        properties: PopupProperties,
        content: @Composable ColumnScope.() -> Unit,
    )
}

/**
 * Production renderer: the real Material 3 [DropdownMenu], shown in a [androidx.compose.ui.window.Popup].
 */
private object RealDropdownMenu : DropdownMenuRenderer {
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
            content = content,
        )
    }
}

/**
 * Preview renderer: layoutlib does not paint [androidx.compose.ui.window.Popup] content, so the same
 * content is rendered inline inside a plain [Surface]. Styling mirrors the real menu so
 * previews/screenshots stay visually faithful.
 */
private object PreviewDropdownMenu : DropdownMenuRenderer {
    @Composable
    override fun Render(
        modifier: Modifier,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        properties: PopupProperties,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        if (!expanded) return
        Surface(
            modifier = modifier,
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(vertical = DROPDOWN_MENU_VERTICAL_PADDING),
                content = content,
            )
        }
    }
}
