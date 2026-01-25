package com.msmobile.visitas.visit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.OnScaffoldConfigurationChanged
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.EditableTextFieldColors
import com.msmobile.visitas.extension.OnBackPressed
import com.msmobile.visitas.extension.ReadOnlyTextFieldColors
import com.msmobile.visitas.extension.RequestCalendarPermission
import com.msmobile.visitas.extension.RequestLocationPermission
import com.msmobile.visitas.extension.isKeyboardOpen
import com.msmobile.visitas.extension.removeBottomCorner
import com.msmobile.visitas.extension.removeTopCorner
import com.msmobile.visitas.extension.sharp
import com.msmobile.visitas.extension.stringResource
import com.msmobile.visitas.extension.textField
import com.msmobile.visitas.extension.toString
import com.msmobile.visitas.ui.views.DateTimePicker
import com.msmobile.visitas.ui.views.DetailFooter
import com.msmobile.visitas.ui.views.LazyColumnWithScrollbar
import com.msmobile.visitas.ui.views.PermissionRationaleSheet
import com.msmobile.visitas.ui.views.TextFieldClearButton
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.verticalFieldPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import java.util.Locale
import java.util.UUID

@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun VisitDetailScreen(
    navigator: DestinationsNavigator,
    viewModel: VisitDetailViewModel,
    householderId: UUID? = null,
    scaffoldConfigurationChanged: OnScaffoldConfigurationChanged
) {
    val uiState: VisitDetailViewModel.UiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    LaunchedEffect(key1 = null) {
        scaffoldConfigurationChanged(
            MainActivityViewModel.ScaffoldState(
                showBottomBar = true,
                showFAB = false
            )
        )
    }
    VisitDetailScreenContent(navigator, householderId, uiState, onEvent)
}

@Composable
private fun VisitDetailScreenContent(
    navigator: DestinationsNavigator,
    householderId: UUID?,
    uiState: VisitDetailViewModel.UiState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    val showDeleteButton = uiState.showDeleteButton
    val isKeyboardOpen by isKeyboardOpen()

    LaunchedEffect(key1 = null) {
        onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId))

    }

    OnBackPressed {
        onEvent(VisitDetailViewModel.UiEvent.CancelClicked)
    }
    Scaffold(
        bottomBar = {
            DetailFooter(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
                    .padding(borderPadding),
                showDeleteButton = showDeleteButton,
                onSaveClickedEvent = { onEvent(VisitDetailViewModel.UiEvent.SaveClicked) },
                onCancelClickedEvent = { onEvent(VisitDetailViewModel.UiEvent.CancelClicked) },
                onDeleteClicked = { onEvent(VisitDetailViewModel.UiEvent.DeleteClicked) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = !isKeyboardOpen,
                text = {
                    Text(text = stringResource(id = R.string.add_visit))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.add_visit)
                    )
                }, onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.AddVisitClicked)
                }
            )
        }
    ) { paddingValues ->
        val bottomPadding by remember { mutableStateOf(paddingValues.calculateBottomPadding()) }
        VisitDetail(
            bottomPadding = bottomPadding,
            uiState = uiState,
            onEvent = onEvent
        )
        if (uiState.showLocationPermissionDialog) {
            RequestLocationPermission {
                onEvent(VisitDetailViewModel.UiEvent.LocationPermissionDialogShown)
                onEvent(VisitDetailViewModel.UiEvent.LocationPermissionGranted)
            }
        }
        if (uiState.showCalendarPermissionDialog) {
            RequestCalendarPermission {
                onEvent(VisitDetailViewModel.UiEvent.CalendarPermissionDialogShown)
                onEvent(VisitDetailViewModel.UiEvent.CalendarPermissionGranted)
            }
        }
        PermissionRationaleSheet(
            isVisible = uiState.showLocationRationale,
            message = stringResource(R.string.location_permission_message),
            icon = Icons.Rounded.LocationOn,
            onDismiss = {
                onEvent(VisitDetailViewModel.UiEvent.LocationRationaleDismissed)
            },
            onConfirm = {
                onEvent(VisitDetailViewModel.UiEvent.LocationRationaleAccepted)
            }
        )
        PermissionRationaleSheet(
            isVisible = uiState.showCalendarRationale,
            message = stringResource(R.string.calendar_permission_message),
            icon = Icons.Rounded.DateRange,
            onDismiss = {
                onEvent(VisitDetailViewModel.UiEvent.CalendarRationaleDismissed)
            },
            onConfirm = {
                onEvent(VisitDetailViewModel.UiEvent.CalendarRationaleAccepted)
            }
        )
        StateHandler(uiState = uiState, navigator = navigator, onEvent = onEvent)
    }
}

@Composable
private fun VisitDetail(
    bottomPadding: Dp,
    uiState: VisitDetailViewModel.UiState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    val visitList = uiState.visitList.filter { !it.wasRemoved }
    val listState = rememberLazyListState()
    LazyColumnWithScrollbar(listState = listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .padding(borderPadding),
            verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
        ) {
            item {
                HouseholderDetail(
                    householder = uiState.householder,
                    onEvent = onEvent
                )
            }
            items(visitList, key = { it.id }) { visit ->
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.animateItem()
                ) {
                    VisitItem(
                        visit = visit,
                        conversationList = uiState.conversationList,
                        visitTypeList = uiState.visitTypeList,
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
private fun HouseholderDetail(
    householder: VisitDetailViewModel.HouseholderState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                onEvent(VisitDetailViewModel.UiEvent.NameFocusChanged(focusState.hasFocus))
            },
        value = householder.name,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        trailingIcon = {
            TextFieldClearButton(householder.showClearName, onClear = {
                onEvent(VisitDetailViewModel.UiEvent.ClearNameClicked)
            })
        },
        label = {
            Text(text = stringResource(id = R.string.householder_name))
        },
        colors = EditableTextFieldColors,
        shape = MaterialTheme.shapes.textField.removeBottomCorner(),
        onValueChange = { value ->
            onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged(value))
        }
    )
    HorizontalDivider()
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                onEvent(VisitDetailViewModel.UiEvent.AddressFocusChanged(focusState.hasFocus))
            },
        value = householder.address,
        readOnly = householder.isLoadingAddress,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        trailingIcon = {
            HouseholderAddressActionButton(
                householder.addressState,
                householder.isLoadingAddress,
                onEvent
            )
        },
        label = {
            Text(text = stringResource(id = R.string.householder_address))
        },
        colors = EditableTextFieldColors,
        shape = MaterialTheme.shapes.sharp,
        onValueChange = { value ->
            onEvent(VisitDetailViewModel.UiEvent.HouseholderAddressChanged(value))
        }
    )
    HorizontalDivider()
    PreferredDayDropdown(
        householder = householder,
        onEvent = onEvent
    )
    HorizontalDivider()
    PreferredTimeDropdown(
        householder = householder,
        onEvent = onEvent
    )
    HorizontalDivider()
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                onEvent(VisitDetailViewModel.UiEvent.NotesFocusChanged(focusState.hasFocus))
            },
        value = householder.notes ?: "",
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true
        ),
        trailingIcon = {
            TextFieldClearButton(show = householder.showClearNotes, onClear = {
                onEvent(VisitDetailViewModel.UiEvent.ClearNotesClicked)
            })
        },
        label = {
            Text(text = stringResource(id = R.string.householder_notes))
        },
        colors = EditableTextFieldColors,
        shape = MaterialTheme.shapes.textField.removeTopCorner(),
        onValueChange = { value ->
            onEvent(VisitDetailViewModel.UiEvent.HouseholderNotesChanged(value))
        }
    )
}

@Composable
private fun HouseholderAddressActionButton(
    addressState: VisitDetailViewModel.HouseholderAddressState,
    isLoadingAddress: Boolean,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    val animatedRotationValue by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        ), label = ""
    )
    if (isLoadingAddress) {
        Icon(
            modifier = Modifier.rotate(animatedRotationValue),
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null
        )
    } else {
        when (addressState) {
            VisitDetailViewModel.HouseholderAddressState.ShowClearAddress -> {
                TextFieldClearButton(true, onClear = {
                    onEvent(VisitDetailViewModel.UiEvent.ClearAddressClicked)
                })
            }

            VisitDetailViewModel.HouseholderAddressState.LoadLocation -> {
                IconButton(onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.LoadAddressClicked)
                }) {
                    Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null)
                }
            }

            VisitDetailViewModel.HouseholderAddressState.LookUpAddressFromLatLong -> {
                IconButton(onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.LookUpAddressFromLatLongClicked)
                }) {
                    Icon(imageVector = Icons.Rounded.TravelExplore, contentDescription = null)
                }
            }

            VisitDetailViewModel.HouseholderAddressState.None -> {
                // No button
            }
        }
    }
}

@Composable
private fun PreferredDayDropdown(
    householder: VisitDetailViewModel.HouseholderState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDay = householder.preferredDay
    val displayText = when (selectedDay) {
        VisitPreferredDay.ANY -> stringResource(R.string.preferred_day_any)
        VisitPreferredDay.SUNDAY -> stringResource(R.string.preferred_day_sunday)
        VisitPreferredDay.MONDAY -> stringResource(R.string.preferred_day_monday)
        VisitPreferredDay.TUESDAY -> stringResource(R.string.preferred_day_tuesday)
        VisitPreferredDay.WEDNESDAY -> stringResource(R.string.preferred_day_wednesday)
        VisitPreferredDay.THURSDAY -> stringResource(R.string.preferred_day_thursday)
        VisitPreferredDay.FRIDAY -> stringResource(R.string.preferred_day_friday)
        VisitPreferredDay.SATURDAY -> stringResource(R.string.preferred_day_saturday)
        VisitPreferredDay.WEEKDAYS -> stringResource(R.string.preferred_day_weekdays)
        VisitPreferredDay.WEEKENDS -> stringResource(R.string.preferred_day_weekends)
    }

    Column {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(text = stringResource(id = R.string.preferred_day)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null
                )
            },
            colors = ReadOnlyTextFieldColors,
            shape = MaterialTheme.shapes.sharp
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VisitPreferredDay.entries.forEach { day ->
                val itemText = when (day) {
                    VisitPreferredDay.ANY -> stringResource(R.string.preferred_day_any)
                    VisitPreferredDay.SUNDAY -> stringResource(R.string.preferred_day_sunday)
                    VisitPreferredDay.MONDAY -> stringResource(R.string.preferred_day_monday)
                    VisitPreferredDay.TUESDAY -> stringResource(R.string.preferred_day_tuesday)
                    VisitPreferredDay.WEDNESDAY -> stringResource(R.string.preferred_day_wednesday)
                    VisitPreferredDay.THURSDAY -> stringResource(R.string.preferred_day_thursday)
                    VisitPreferredDay.FRIDAY -> stringResource(R.string.preferred_day_friday)
                    VisitPreferredDay.SATURDAY -> stringResource(R.string.preferred_day_saturday)
                    VisitPreferredDay.WEEKDAYS -> stringResource(R.string.preferred_day_weekdays)
                    VisitPreferredDay.WEEKENDS -> stringResource(R.string.preferred_day_weekends)
                }
                DropdownMenuItem(
                    text = { Text(text = itemText) },
                    onClick = {
                        onEvent(VisitDetailViewModel.UiEvent.PreferredDayChanged(day))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PreferredTimeDropdown(
    householder: VisitDetailViewModel.HouseholderState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTime = householder.preferredTime
    val displayText = when (selectedTime) {
        VisitPreferredTime.ANY -> stringResource(R.string.preferred_time_any)
        VisitPreferredTime.MORNING -> stringResource(R.string.preferred_time_morning)
        VisitPreferredTime.AFTERNOON -> stringResource(R.string.preferred_time_afternoon)
        VisitPreferredTime.EVENING -> stringResource(R.string.preferred_time_evening)
    }

    Column {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(text = stringResource(id = R.string.preferred_time)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null
                )
            },
            colors = ReadOnlyTextFieldColors,
            shape = MaterialTheme.shapes.sharp
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VisitPreferredTime.entries.forEach { time ->
                val itemText = when (time) {
                    VisitPreferredTime.ANY -> stringResource(R.string.preferred_time_any)
                    VisitPreferredTime.MORNING -> stringResource(R.string.preferred_time_morning)
                    VisitPreferredTime.AFTERNOON -> stringResource(R.string.preferred_time_afternoon)
                    VisitPreferredTime.EVENING -> stringResource(R.string.preferred_time_evening)
                }
                DropdownMenuItem(
                    text = { Text(text = itemText) },
                    onClick = {
                        onEvent(VisitDetailViewModel.UiEvent.PreferredTimeChanged(time))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.VisitItem(
    visit: VisitDetailViewModel.VisitState,
    conversationList: List<VisitDetailViewModel.ConversationState>,
    visitTypeList: List<VisitDetailViewModel.VisitTypeState>,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    val dateFocusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }

    LaunchedEffect(visit.hasVisitTimeError) {
        if (visit.hasVisitTimeError && !hasRequestedFocus) {
            dateFocusRequester.requestFocus()
            hasRequestedFocus = true
        }
    }

    Column {
        VisitSubjectDropdownList(
            modifier = Modifier.fillMaxWidth(),
            visit = visit,
            conversationList = conversationList,
            onEvent = onEvent
        )
        HorizontalDivider()
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(dateFocusRequester)
                .clickable {
                    onEvent(VisitDetailViewModel.UiEvent.VisitDateClicked(visit))
                },
            value = visit.date.toString(Locale.getDefault()),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            isError = visit.hasVisitTimeError,
            shape = MaterialTheme.shapes.sharp,
            colors = ReadOnlyTextFieldColors,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = stringResource(
                        id = R.string.visit_date
                    )
                )
            }
        )
        if (visit.hasVisitTimeError) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = horizontalFieldPadding),
                text = stringResource(id = R.string.visit_time_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider()
        VisitTypeDropdownList(
            modifier = Modifier.fillMaxWidth(),
            visit = visit,
            visitTypeList = visitTypeList,
            onEvent = onEvent
        )
        HorizontalDivider()
        Row(
            modifier = Modifier
                .heightIn(min = TextFieldDefaults.MinHeight)
                .background(
                    color = ReadOnlyTextFieldColors.unfocusedContainerColor,
                    shape = MaterialTheme.shapes.textField.removeTopCorner(),
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = visit.isDone, onCheckedChange = { value ->
                onEvent(VisitDetailViewModel.UiEvent.VisitDoneChanged(visit, value))
            })
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onEvent(
                            VisitDetailViewModel.UiEvent.VisitDoneChanged(
                                visit,
                                !visit.isDone
                            )
                        )
                    }, text = stringResource(id = R.string.visit_is_done)
            )

            if (visit.canBeRemoved) {
                IconButton(onClick = {
                    onEvent(
                        VisitDetailViewModel.UiEvent.RemoveVisitClicked(
                            visit
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
private fun LazyItemScope.VisitSubjectDropdownList(
    modifier: Modifier,
    visit: VisitDetailViewModel.VisitState,
    conversationList: List<VisitDetailViewModel.ConversationState>,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    // TextFieldValue state maintained locally for IME compatibility
    var textFieldValueState by remember(visit.id) {
        mutableStateOf(
            TextFieldValue(
                text = visit.subject,
                selection = TextRange(visit.caretPosition)
            )
        )
    }

    // Track external changes to visit.subject (from conversation selection, clear button, etc)
    // and update TextFieldValue only when the change didn't originate from user typing
    LaunchedEffect(visit.subject) {
        if (textFieldValueState.text != visit.subject) {
            textFieldValueState = TextFieldValue(
                text = visit.subject,
                selection = TextRange(visit.caretPosition)
            )
        }
    }

    Column(modifier = modifier) {
        TextField(
            modifier = modifier.onFocusChanged { focusState ->
                onEvent(
                    VisitDetailViewModel.UiEvent.VisitSubjectFocusChanged(
                        focusState.hasFocus,
                        visit
                    )
                )
            },
            value = textFieldValueState,
            shape = MaterialTheme.shapes.textField.removeBottomCorner(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true
            ),
            trailingIcon = {
                TextFieldClearButton(show = visit.showClearSubject) {
                    onEvent(
                        VisitDetailViewModel.UiEvent.ClearSubjectClicked(visit = visit)
                    )
                }
                NextVisitSuggestionButton(
                    show = visit.showNextVisitSuggestion,
                    onClick = {
                        onEvent(
                            VisitDetailViewModel.UiEvent.NextVisitSuggestionClicked(
                                visit
                            )
                        )
                    }
                )
            },
            label = {
                Text(text = stringResource(id = R.string.visit_subject))
            },
            colors = EditableTextFieldColors,
            onValueChange = { value ->
                // Update local state immediately for smooth typing
                textFieldValueState = value

                // Send to ViewModel
                onEvent(
                    VisitDetailViewModel.UiEvent.VisitSubjectChanged(
                        visit = visit,
                        value = value.text,
                        caretPosition = value.selection.start
                    )
                )
            })
        DropdownMenu(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .fillParentMaxWidth(),
            expanded = visit.isConversationListExpanded,
            properties = PopupProperties(focusable = false),
            onDismissRequest = {
                onEvent(VisitDetailViewModel.UiEvent.ConversationListDismissed(visit))
            }) {
            Text(
                modifier = Modifier.padding(horizontalFieldPadding),
                text = stringResource(id = R.string.conversations)
            )
            HorizontalDivider()
            conversationList.map { conversation ->
                if (conversation.show) {
                    DropdownMenuItem(
                        contentPadding = PaddingValues(
                            vertical = verticalFieldPadding,
                            horizontal = horizontalFieldPadding
                        ),
                        text = { Text(text = conversation.question) },
                        onClick = {
                            onEvent(
                                VisitDetailViewModel.UiEvent.ConversationSelected(
                                    visit = visit,
                                    conversation = conversation,
                                    caretPosition = textFieldValueState.selection.start
                                )
                            )
                        })
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.VisitTypeDropdownList(
    modifier: Modifier,
    visit: VisitDetailViewModel.VisitState,
    visitTypeList: List<VisitDetailViewModel.VisitTypeState>,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    Column(modifier = modifier) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(VisitDetailViewModel.UiEvent.VisitTypeClicked(visit))
                },
            value = stringResource(resource = visit.visitType.description),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            colors = ReadOnlyTextFieldColors,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(
                        id = R.string.expand_visit_type_list
                    )
                )
            }
        )
        DropdownMenu(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .fillParentMaxWidth(),
            expanded = visit.isVisitTypeListExpanded,
            properties = PopupProperties(focusable = false),
            onDismissRequest = {
                onEvent(VisitDetailViewModel.UiEvent.ConversationListDismissed(visit))
            }) {
            Text(
                modifier = Modifier.padding(horizontalFieldPadding),
                text = stringResource(id = R.string.visit_type)
            )
            HorizontalDivider()
            visitTypeList.map { visitType ->
                DropdownMenuItem(
                    contentPadding = PaddingValues(
                        vertical = verticalFieldPadding,
                        horizontal = horizontalFieldPadding
                    ),
                    text = { Text(text = stringResource(resource = visitType.description)) },
                    onClick = {
                        onEvent(
                            VisitDetailViewModel.UiEvent.VisitTypeSelected(
                                visit,
                                visitType
                            )
                        )
                    })
            }
        }
    }
}

@Composable
private fun StateHandler(
    uiState: VisitDetailViewModel.UiState,
    navigator: DestinationsNavigator,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    when (val eventState = uiState.eventState) {
        is VisitDetailViewModel.UiEventState.Canceled,
        is VisitDetailViewModel.UiEventState.SaveSucceeded,
        is VisitDetailViewModel.UiEventState.Deleted -> {
            navigator.navigateUp()
        }

        is VisitDetailViewModel.UiEventState.Idle,
        is VisitDetailViewModel.UiEventState.Saving,
        is VisitDetailViewModel.UiEventState.Deleting,
        is VisitDetailViewModel.UiEventState.ValidationError -> {
        }

        is VisitDetailViewModel.UiEventState.VisitDateExpanded -> {
            DateTimePicker(
                dateTime = eventState.visit.date,
                onDateSelected = { dateTime ->
                    onEvent(
                        VisitDetailViewModel.UiEvent.VisitDateAccepted(
                            eventState.visit,
                            dateTime
                        )
                    )
                }, onDismiss = {
                    onEvent(VisitDetailViewModel.UiEvent.VisitDateDismissed)
                }
            )
        }

        is VisitDetailViewModel.UiEventState.NextVisitSuggestionShowing -> {
            AlertDialog(
                onDismissRequest = {
                    onEvent(VisitDetailViewModel.UiEvent.NextVisitSuggestionDismissed)
                },
                title = {
                    Text(text = stringResource(id = R.string.next_visit_suggestion))
                },
                text = {
                    Text(text = eventState.visit.nextConversationSuggestion?.question ?: "")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(
                                VisitDetailViewModel.UiEvent.NextVisitSuggestionAccepted(
                                    eventState.visit
                                )
                            )
                        }
                    ) {
                        Text(stringResource(id = R.string.add_visit))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(VisitDetailViewModel.UiEvent.NextVisitSuggestionDismissed)
                        }
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }

        is VisitDetailViewModel.UiEventState.DeleteConfirmation -> {
            DeleteMessage(onEvent)
        }

        is VisitDetailViewModel.UiEventState.DiscardChangesConfirmation -> {
            DiscardChangesMessage(onEvent)
        }

        is VisitDetailViewModel.UiEventState.NoAddressFound -> {
            Snackbar(
                modifier = Modifier.padding(borderPadding),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                dismissAction = {
                    IconButton(onClick = {
                        onEvent(VisitDetailViewModel.UiEvent.SnackbarDismissed)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }) {
                Text(
                    text = stringResource(R.string.houlseholder_no_address_found),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DeleteMessage(onEvent: (VisitDetailViewModel.UiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onEvent(VisitDetailViewModel.UiEvent.DeleteDismissed)
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
                    onEvent(VisitDetailViewModel.UiEvent.DeleteAccepted)
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.DeleteDismissed)
                }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun DiscardChangesMessage(onEvent: (VisitDetailViewModel.UiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onEvent(VisitDetailViewModel.UiEvent.DiscardChangesDismissed)
        },
        title = {
            Text(text = stringResource(id = R.string.discard_changes_title))
        },
        text = {
            Text(text = stringResource(id = R.string.should_discard_changes))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.DiscardChangesAccepted)
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.DiscardChangesDismissed)
                }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun NextVisitSuggestionButton(show: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(show) {
        IconButton(
            onClick = onClick
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(id = R.string.conversation_suggestion)
            )
        }
    }
}

@Preview
@Composable
private fun VisitDetailScreenPreview(
    @PreviewParameter(VisitDetailPreviewConfigProvider::class) config: VisitDetailPreviewConfig
) {
    AppScaffold(
        uiState = config.mainActivityUiState,
        currentDestination = VisitDetailScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) {
        VisitDetailScreenContent(
            navigator = EmptyDestinationsNavigator,
            householderId = config.householderId,
            uiState = config.uiState,
            onEvent = {}
        )
    }
}
