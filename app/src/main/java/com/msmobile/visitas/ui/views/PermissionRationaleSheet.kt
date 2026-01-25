package com.msmobile.visitas.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.verticalFieldPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRationaleSheet(
    icon: ImageVector,
    message: String,
    isVisible: Boolean,
    contentPaddingValues: PaddingValues = PermissionRationaleSheetDefaults.contentPaddingValues,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(visible = isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = borderPadding * 2, vertical = verticalFieldPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(contentPaddingValues)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Button(onClick = onConfirm) {
                        Text(stringResource(id = R.string.grant_permissions))
                    }
                }
            }
        }
    }
}

object PermissionRationaleSheetDefaults {
    val contentPaddingValues = PaddingValues(vertical = 50.dp)
}