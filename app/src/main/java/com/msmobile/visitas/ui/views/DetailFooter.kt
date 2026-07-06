package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.borderPadding

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailFooter(
    modifier: Modifier = Modifier,
    onDiscardClicked: () -> Unit,
    onSaveClickedEvent: () -> Unit,
    onFabClickedEvent: () -> Unit
) {
    Row(modifier = modifier) {
        FloatingBar(
            modifier = Modifier.weight(weight = .5f, fill = false),
            floatingActionButton = {
                FloatingAddButton(
                    modifier = Modifier.padding(end = borderPadding),
                    onFabClickedEvent = onFabClickedEvent
                )
            },
            content = {
                IconButton(onClick = onDiscardClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(id = R.string.discard_draft)
                    )
                }
                IconButton(onClick = onSaveClickedEvent) {
                    Icon(
                        imageVector = Icons.Rounded.DoneOutline,
                        contentDescription = stringResource(id = R.string.save)
                    )
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
                onDiscardClicked = {},
                onSaveClickedEvent = {},
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
        DetailFooter(
            onDiscardClicked = {},
            onSaveClickedEvent = {},
            onFabClickedEvent = {}
        )
    }
}
