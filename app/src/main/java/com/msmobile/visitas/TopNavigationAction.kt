package com.msmobile.visitas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource

data class TopNavigationAction(
    val contentDescription: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Standard "up" navigation action published by a screen so the app bar renders a back arrow that
 * invokes the screen's own [onNavigateUp]. Shared so every screen wires the same icon and content
 * description while owning the navigation behaviour.
 */
@Composable
fun upNavigationActions(onNavigateUp: () -> Unit): List<TopNavigationAction> = listOf(
    TopNavigationAction(
        contentDescription = stringResource(id = R.string.navigate_back_content_description),
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        onClick = onNavigateUp
    )
)
