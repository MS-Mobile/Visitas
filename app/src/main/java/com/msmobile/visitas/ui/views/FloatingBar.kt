package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingBar(
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit,
    buttonsHorizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(FloatingBarDefaults.ButtonsPadding),
    buttonsModifier: Modifier = Modifier
        .padding(horizontal = FloatingBarDefaults.ButtonsPadding),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = buttonsModifier.padding(8.dp),
                horizontalArrangement = buttonsHorizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
        floatingActionButton()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object FloatingBarDefaults {
    val ButtonsPadding: Dp
        get() {
            return FloatingToolbarDefaults.ScreenOffset * 2
        }
}

@PreviewPhone
@PreviewFoldable
@Composable
fun FloatingBarPreview() {
    VisitasTheme {
        FloatingBar(
            floatingActionButton = {
                FloatingAddButton(onFabClickedEvent = { })
            },
            content = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }
        )
    }
}
