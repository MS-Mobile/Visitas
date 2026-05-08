package com.msmobile.visitas.ui.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingAddButton(
    onFabClickedEvent: () -> Unit
) {
    FloatingActionButton(
        onClick = onFabClickedEvent,
        containerColor = vibrantFloatingToolbarColors().fabContainerColor,
        contentColor = vibrantFloatingToolbarColors().fabContentColor
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
    }
}

@PreviewPhone
@PreviewFoldable
@Composable
fun FloatingAddButtonPreview() {
    VisitasTheme {
        FloatingAddButton(onFabClickedEvent = {})
    }
}