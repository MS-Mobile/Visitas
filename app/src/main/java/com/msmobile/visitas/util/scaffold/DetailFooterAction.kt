package com.msmobile.visitas.util.scaffold

import androidx.compose.ui.graphics.vector.ImageVector

data class DetailFooterAction(
    val contentDescription: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
