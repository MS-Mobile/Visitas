package com.msmobile.visitas.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R

@Composable
fun TextFieldClearButton(show: Boolean, onClear: () -> Unit) {
    AnimatedVisibility(visible = show) {
        IconButton(
            onClick = onClear
        ) {
            Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = stringResource(id = R.string.clear_text_content_description)
            )
        }
    }
}