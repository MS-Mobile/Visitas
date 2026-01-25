package com.msmobile.visitas.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R

@Composable
fun SimpleSearchBar(
    modifier: Modifier = Modifier,
    search: String,
    onValueChange: (String) -> Unit,
    isSearchEmpty: Boolean,
    onFilterCleared: () -> Unit
) {
    TextField(
        modifier = modifier,
        value = search,
        onValueChange = onValueChange,
        placeholder = { Text(text = stringResource(id = R.string.search)) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        trailingIcon = {
            Box {
                AnimatedVisibility(visible = isSearchEmpty) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(visible = !isSearchEmpty) {
                    IconButton(onClick = onFilterCleared) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(id = R.string.clear_text)
                        )
                    }
                }
            }
        }
    )
}