package com.msmobile.visitas.util.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class TopBarAction(
    val contentDescription: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val menu: (@Composable () -> Unit)? = null,
)