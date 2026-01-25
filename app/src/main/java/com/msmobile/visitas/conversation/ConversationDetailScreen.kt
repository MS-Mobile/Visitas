package com.msmobile.visitas.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.OnScaffoldConfigurationChanged
import com.msmobile.visitas.R
import com.msmobile.visitas.conversation.ConversationDetailViewModel.ConversationState
import com.msmobile.visitas.extension.EditableTextFieldColors
import com.msmobile.visitas.extension.OnBackPressed
import com.msmobile.visitas.extension.ReadOnlyTextFieldColors
import com.msmobile.visitas.extension.removeBottomCorner
import com.msmobile.visitas.extension.removeTopCorner
import com.msmobile.visitas.extension.textField
import com.msmobile.visitas.ui.views.DetailFooter
import com.msmobile.visitas.ui.views.LazyColumnWithScrollbar
import com.msmobile.visitas.ui.views.TextFieldClearButton
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.verticalFieldPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.UUID

@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun ConversationDetailScreen(
    navigator: DestinationsNavigator,
    viewModel: ConversationDetailViewModel,
    firstConversationId: UUID? = null,
    scaffoldConfigurationChanged: OnScaffoldConfigurationChanged
) {
    val uiState: ConversationDetailViewModel.UiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    val onNavigateUp = {
        navigator.navigateUp()
        Unit
    }

    LaunchedEffect(key1 = null) {
        scaffoldConfigurationChanged(
            MainActivityViewModel.ScaffoldState(
                showBottomBar = true,
                showFAB = false
            )
        )
        onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(firstConversationId))
    }
    OnBackPressed {
        onEvent(ConversationDetailViewModel.UiEvent.CancelClicked)
    }
    ConversationDetailScreenContent(
        uiState = uiState,
        showDeleteButton = firstConversationId != null,
        onEvent = onEvent,
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun ConversationDetailScreenContent(
    uiState: ConversationDetailViewModel.UiState,
    showDeleteButton: Boolean,
    onEvent: (ConversationDetailViewModel.UiEvent) -> Unit,
    onNavigateUp: () -> Unit = {}
) {
    Scaffold(
        bottomBar = {
            DetailFooter(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
                    .padding(borderPadding),
                showDeleteButton = showDeleteButton,
                onSaveClickedEvent = { onEvent(ConversationDetailViewModel.UiEvent.SaveClicked) },
                onCancelClickedEvent = { onEvent(ConversationDetailViewModel.UiEvent.CancelClicked) },
                onDeleteClicked = { onEvent(ConversationDetailViewModel.UiEvent.DeleteClicked) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(text = stringResource(id = R.string.add_conversation))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.add_conversation)
                    )
                }, onClick = {
                    onEvent(ConversationDetailViewModel.UiEvent.AddClicked)
                }
            )
        }
    ) { paddingValues ->
        ConversationItems(
            bottomPadding = paddingValues.calculateBottomPadding(),
            uiState = uiState,
            onEvent = onEvent
        )
        StateHandler(uiState, onEvent, onNavigateUp)
    }
}

@Composable
private fun ConversationItems(
    bottomPadding: Dp,
    uiState: ConversationDetailViewModel.UiState,
    onEvent: (ConversationDetailViewModel.UiEvent) -> Unit,
) {
    val conversationList = uiState.conversationList.filter { !it.wasRemoved }
    val listState = rememberLazyListState()
    LazyColumnWithScrollbar(listState = listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .padding(borderPadding),
            verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
        ) {
            items(conversationList, key = { it.id }) { conversation ->
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.animateItem()
                ) {
                    ConversationItem(
                        conversation = conversation,
                        onEvent = onEvent
                    )
                }
            }
            item {
                Spacer(
                    modifier = Modifier
                        .imePadding()
                        .padding(bottom = bottomPadding)
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationState,
    onEvent: (ConversationDetailViewModel.UiEvent) -> Unit
) {
    val bottomTextFieldShape = if (conversation.canBeDeleted) {
        MaterialTheme.shapes.textField.removeTopCorner().removeBottomCorner()
    } else {
        MaterialTheme.shapes.textField.removeTopCorner()
    }
    Column {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    onEvent(
                        ConversationDetailViewModel.UiEvent.QuestionFocusChanged(
                            hasFocus = focusState.hasFocus,
                            conversation = conversation
                        )
                    )
                },
            value = conversation.question,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            label = {
                Text(text = stringResource(id = R.string.question))
            },
            trailingIcon = {
                TextFieldClearButton(show = conversation.showQuestionClear, onClear = {
                    onEvent(
                        ConversationDetailViewModel.UiEvent.ClearQuestionClicked(
                            conversation = conversation
                        )
                    )
                })
            },
            colors = EditableTextFieldColors,
            shape = MaterialTheme.shapes.textField.removeBottomCorner(),
            onValueChange = { value ->
                onEvent(
                    ConversationDetailViewModel.UiEvent.QuestionChanged(
                        conversation = conversation,
                        value = value
                    )
                )
            })
        HorizontalDivider()
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    onEvent(
                        ConversationDetailViewModel.UiEvent.ResponseFocusChanged(
                            hasFocus = focusState.hasFocus,
                            conversation = conversation
                        )
                    )
                },
            value = conversation.response,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            label = {
                Text(text = stringResource(id = R.string.response))
            },
            trailingIcon = {
                TextFieldClearButton(show = conversation.showResponseClear, onClear = {
                    onEvent(
                        ConversationDetailViewModel.UiEvent.ClearResponseClicked(
                            conversation = conversation
                        )
                    )
                })
            },
            colors = EditableTextFieldColors,
            shape = bottomTextFieldShape,
            onValueChange = { value ->
                onEvent(
                    ConversationDetailViewModel.UiEvent.ResponseChanged(
                        conversation = conversation,
                        value = value
                    )
                )
            })
        if (conversation.canBeDeleted) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ReadOnlyTextFieldColors.unfocusedContainerColor,
                        shape = MaterialTheme.shapes.textField.removeTopCorner(),
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        onEvent(
                            ConversationDetailViewModel.UiEvent.RemoveConversationClicked(
                                conversation = conversation
                            )
                        )
                    }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.remove_visit)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(onEvent: (ConversationDetailViewModel.UiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onEvent(ConversationDetailViewModel.UiEvent.DeleteDismissed)
        },
        title = {
            Text(text = stringResource(id = R.string.delete_title))
        },
        text = {
            Text(text = stringResource(id = R.string.would_you_like_to_delete_this))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onEvent(ConversationDetailViewModel.UiEvent.DeleteDismissed)
                }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun StateHandler(
    uiState: ConversationDetailViewModel.UiState,
    onEvent: (ConversationDetailViewModel.UiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    when (uiState.eventState) {
        is ConversationDetailViewModel.UiEventState.Idle,
        is ConversationDetailViewModel.UiEventState.Saving,
        is ConversationDetailViewModel.UiEventState.Deleting -> {

        }

        is ConversationDetailViewModel.UiEventState.SaveComplete,
        is ConversationDetailViewModel.UiEventState.Canceled,
        is ConversationDetailViewModel.UiEventState.Deleted -> {
            onNavigateUp()
        }

        is ConversationDetailViewModel.UiEventState.DeleteConfirmation -> {
            DeleteConfirmationDialog(onEvent)
        }
    }
}

@Preview
@Composable
private fun ConversationDetailScreenPreview(
    @PreviewParameter(ConversationDetailPreviewConfigProvider::class) config: ConversationDetailPreviewConfig
) {
    AppScaffold(
        uiState = config.mainActivityUiState,
        currentDestination = VisitDetailScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) {
        ConversationDetailScreenContent(
            uiState = config.uiState,
            showDeleteButton = config.showDeleteButton,
            onEvent = {},
            onNavigateUp = {}
        )
    }
}