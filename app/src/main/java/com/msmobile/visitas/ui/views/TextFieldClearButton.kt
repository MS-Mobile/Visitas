package com.msmobile.visitas.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun TextFieldClearButton(show: Boolean, onClear: () -> Unit) {
    AnimatedVisibility(visible = show) {
        IconButton(
            onClick = onClear
        ) {
            Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = null
            )
        }
    }
}