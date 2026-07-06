package com.msmobile.visitas.util.scaffold

import androidx.compose.ui.graphics.vector.ImageVector

data class FloatingActionButtonAction(
    val contentDescription: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
