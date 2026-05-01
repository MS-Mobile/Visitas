package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.horizontalFieldPadding

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailFooter(
    modifier: Modifier = Modifier,
    showDeleteButton: Boolean,
    onSaveClickedEvent: () -> Unit,
    onCancelClickedEvent: () -> Unit,
    onDeleteClicked: () -> Unit,
    onFabClickedEvent: () -> Unit,
    extraButtons: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (showDeleteButton) {
            FloatingBar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = borderPadding),
                floatingActionButton = {},
                content = {
                    IconButton(onClick = onDeleteClicked) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(id = R.string.delete)
                        )
                    }
                }
            )
        }

        FloatingBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onFabClickedEvent
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            },
            content = {
                Row {
                    IconButton(onClick = onCancelClickedEvent) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(id = R.string.cancel)
                        )
                    }

                    extraButtons()

                    IconButton(onClick = onSaveClickedEvent) {
                        Icon(
                            imageVector = Icons.Rounded.DoneOutline,
                            contentDescription = stringResource(id = R.string.save)
                        )
                    }
                }
            }
        )
    }
}

@Composable
@PreviewPhone
@PreviewFoldable
private fun DetailFooterPreview() {
    VisitasTheme {
        Surface {
            DetailFooter(
                showDeleteButton = false,
                onSaveClickedEvent = {},
                onCancelClickedEvent = {},
                onDeleteClicked = {},
                onFabClickedEvent = {}
            )
        }
    }
}

@Composable
@PreviewPhone
@PreviewFoldable
private fun DetailFooterWithDeletePreview() {
    VisitasTheme {
        Surface {
            DetailFooter(
                showDeleteButton = true,
                onSaveClickedEvent = {},
                onCancelClickedEvent = {},
                onDeleteClicked = {},
                onFabClickedEvent = {}
            )
        }
    }
}
