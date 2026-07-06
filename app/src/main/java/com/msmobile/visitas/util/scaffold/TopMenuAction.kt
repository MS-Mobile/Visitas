package com.msmobile.visitas.util.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R

data class TopMenuAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Overflow menu option that navigates to the settings screen. Shared so every screen that surfaces
 * the settings menu wires the same label and icon while owning the navigation behaviour.
 */
@Composable
fun settingsTopMenuActions(onNavigateToSettings: () -> Unit): List<TopMenuAction> = listOf(
    TopMenuAction(
        text = stringResource(id = R.string.settings),
        icon = Icons.Rounded.Settings,
        onClick = onNavigateToSettings
    )
)
