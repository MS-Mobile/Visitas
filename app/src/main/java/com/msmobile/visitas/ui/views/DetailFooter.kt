package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.util.borderPadding

@Composable
fun DetailFooter(
    modifier: Modifier = Modifier,
    showDeleteButton: Boolean,
    onSaveClickedEvent: () -> Unit,
    onCancelClickedEvent: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(borderPadding, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDeleteButton) {
            IconButton(onClick = onDeleteClicked) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(id = R.string.delete)
                )
            }
        }
        OutlinedButton(
            onClick = onCancelClickedEvent
        ) {
            Text(text = stringResource(id = R.string.cancel))
        }
        Button(
            onClick = onSaveClickedEvent
        ) {
            Text(text = stringResource(id = R.string.save))
        }
    }
}