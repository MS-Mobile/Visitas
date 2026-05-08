package com.msmobile.visitas.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.icons.CopyDataIcon

@Composable
fun CopyDataButton(show: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = show) {
        val label = stringResource(id = R.string.copy_data_content_description)
        IconButton(
            modifier = Modifier.semantics { contentDescription = label },
            onClick = onClick,
        ) {
            CopyDataIcon()
        }
    }
}