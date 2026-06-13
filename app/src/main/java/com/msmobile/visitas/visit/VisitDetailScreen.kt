package com.msmobile.visitas.visit

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.AppScaffoldState
import com.msmobile.visitas.DetailFooterActions
import com.msmobile.visitas.R
import com.msmobile.visitas.TopBarAction
import com.msmobile.visitas.extension.EditableTextFieldColors
import com.msmobile.visitas.extension.OnBackPressed
import com.msmobile.visitas.extension.ReadOnlyTextFieldColors
import com.msmobile.visitas.extension.RequestCalendarPermission
import com.msmobile.visitas.extension.RequestLocationPermission
import com.msmobile.visitas.extension.removeBottomCorner
import com.msmobile.visitas.extension.removeTopCorner
import com.msmobile.visitas.extension.sharp
import com.msmobile.visitas.extension.stringResource
import com.msmobile.visitas.extension.textField
import com.msmobile.visitas.extension.toString
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.DateTimePicker
import com.msmobile.visitas.ui.views.LazyColumnWithScrollbar
import com.msmobile.visitas.ui.views.PermissionRationaleSheet
import com.msmobile.visitas.ui.views.TextFieldClearButton
import com.msmobile.visitas.ui.views.TextFieldExpandButton
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.floatingBarBottomPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.snackbarPadding
import com.msmobile.visitas.util.verticalFieldPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.UUID

@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun VisitDetailScreen(
    navigator: DestinationsNavigator,
    viewModel: VisitDetailViewModel,
    appScaffoldState: AppScaffoldState,
    householderId: UUID? = null
) {
    val uiState: VisitDetailViewModel.UiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    val onNavigateUp = {
        navigator.navigateUp()
        Unit
    }
    VisitDetailScreenContent(
        householderId = householderId,
        uiState = uiState,
        appScaffoldState = appScaffoldState,
        onEvent = onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun VisitDetailScreenContent(
    householderId: UUID?,
    uiState: VisitDetailViewModel.UiState,
    appScaffoldState: AppScaffoldState,
    onNavigateUp: () -> Unit,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit,
) {
    LaunchedEffect(key1 = null) {
        onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId))
    }

    OnBackPressed {
        onEvent(VisitDetailViewModel.UiEvent.CancelClicked)
    }

    val copyDataDescription = stringResource(id = R.string.copy_data_content_description)
    val deleteDescription = stringResource(id = R.string.delete)
    val chromeOwner = remember { Any() }
    DisposableEffect(Unit) {
        appScaffoldState.setUiState(
            owner = chromeOwner,
            uiState = AppScaffoldState.UiState(
                topBarActions = listOf(
                    TopBarAction(
                        contentDescription = copyDataDescription,
                        icon = Icons.Rounded.ContentCopy,
                        onClick = { onEvent(VisitDetailViewModel.UiEvent.CopyVisitDataClicked) }
                    ),
                    TopBarAction(
                        contentDescription = deleteDescription,
                        icon = Icons.Rounded.Delete,
                        onClick = { onEvent(VisitDetailViewModel.UiEvent.DeleteClicked) }
                    )
                ),
                detailFooterActions = DetailFooterActions(
                    onBack = { onEvent(VisitDetailViewModel.UiEvent.CancelClicked) },
                    onSave = { onEvent(VisitDetailViewModel.UiEvent.SaveClicked) },
                    onAdd = { onEvent(VisitDetailViewModel.UiEvent.AddVisitClicked) }
                )
            )
        )
        onDispose { appScaffoldState.clearUiState(chromeOwner) }
    }
    VisitDetail(
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
    StateHandler(uiState = uiState, onNavigateUp = onNavigateUp, onEvent = onEvent)
}

@Composable
private fun VisitDetail(
    uiState: VisitDetailViewModel.UiState,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    val visitList = uiState.visitList.filter { !it.wasRemoved }
    val listState = rememberLazyListState()
    LazyColumnWithScrollbar(listState = listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = borderPadding),
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
                        .padding(bottom = verticalFieldPadding + floatingBarBottomPadding)
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
    Row {
        PreferredDayDropdown(
            modifier = Modifier.weight(.5f),
            householder = householder,
            onEvent = onEvent
        )
        VerticalDivider()
        PreferredTimeDropdown(
            modifier = Modifier.weight(.5f),
            householder = householder,
            onEvent = onEvent
        )
    }
    HorizontalDivider()
    val textStyle = LocalTextStyle.current
    val textMeasurer = rememberTextMeasurer()
    var notesTextValue by remember { mutableStateOf(TextFieldValue(householder.notes ?: "")) }
    LaunchedEffect(householder.notes) {
        if (notesTextValue.text != (householder.notes ?: "")) {
            notesTextValue = TextFieldValue(householder.notes ?: "")
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val notesHasOverflow = remember(householder.notes, constraints.maxWidth, textStyle) {
            if (householder.notes.isNullOrEmpty()) false
            else {
                val result = textMeasurer.measure(
                    text = householder.notes,
                    style = textStyle,
                    constraints = constraints,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                result.hasVisualOverflow
            }
        }
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.hasFocus) {
                        notesTextValue = notesTextValue.copy(selection = TextRange(0))
                    }
                    onEvent(VisitDetailViewModel.UiEvent.NotesFocusChanged(focusState.hasFocus))
                },
            value = notesTextValue,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true
            ),
            trailingIcon = {
                TextFieldClearButton(show = householder.showClearNotes, onClear = {
                    onEvent(VisitDetailViewModel.UiEvent.ClearNotesClicked)
                })
                TextFieldExpandButton(
                    show = !householder.showClearNotes && notesHasOverflow,
                    isExpanded = householder.isNotesExpanded,
                    onExpand = {
                        onEvent(VisitDetailViewModel.UiEvent.ExpandNotesClicked)
                    }
                )
            },
            label = {
                Text(text = stringResource(id = R.string.householder_notes))
            },
            colors = EditableTextFieldColors,
            shape = MaterialTheme.shapes.textField.removeTopCorner(),
            singleLine = !householder.isNotesExpanded,
            onValueChange = { value ->
                notesTextValue = value
                onEvent(VisitDetailViewModel.UiEvent.HouseholderNotesChanged(value.text))
            }
        )
    }
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
            contentDescription = stringResource(id = R.string.loading_address_content_description)
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
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = stringResource(id = R.string.load_address_content_description)
                    )
                }
            }

            VisitDetailViewModel.HouseholderAddressState.LookUpAddressFromLatLong -> {
                IconButton(onClick = {
                    onEvent(VisitDetailViewModel.UiEvent.LookUpAddressFromLatLongClicked)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.TravelExplore,
                        contentDescription = stringResource(id = R.string.lookup_address_content_description)
                    )
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
    modifier: Modifier = Modifier,
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

    Column(modifier = modifier) {
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
                    contentDescription = stringResource(id = R.string.expand_preferred_day_list_content_description)
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
    modifier: Modifier = Modifier,
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

    Column(modifier = modifier) {
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
                    contentDescription = stringResource(id = R.string.expand_preferred_time_list_content_description)
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
            value = visit.date.toString(LocalLocale.current.platformLocale),
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
                onEvent(VisitDetailViewModel.UiEvent.VisitTypeListDismissed(visit))
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
    onNavigateUp: () -> Unit,
    onEvent: (VisitDetailViewModel.UiEvent) -> Unit
) {
    when (val eventState = uiState.eventState) {
        is VisitDetailViewModel.UiEventState.Canceled,
        is VisitDetailViewModel.UiEventState.SaveSucceeded,
        is VisitDetailViewModel.UiEventState.Deleted -> {
            LaunchedEffect(eventState) {
                onNavigateUp()
            }
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
            NoAddressFoundSnackbar(
                modifier = Modifier.snackbarPadding(),
                onSnackbarDismissed = {
                    onEvent(VisitDetailViewModel.UiEvent.SnackbarDismissed)
                }
            )
        }

        is VisitDetailViewModel.UiEventState.CopiedToClipboard -> {
            CopiedToClipboardSnackbar(
                modifier = Modifier.snackbarPadding(),
                onSnackbarDismissed = {
                    onEvent(VisitDetailViewModel.UiEvent.SnackbarDismissed)
                }
            )
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
private fun NoAddressFoundSnackbar(
    modifier: Modifier,
    onSnackbarDismissed: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Snackbar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            dismissAction = {
                IconButton(onClick = onSnackbarDismissed) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_icon_content_description),
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

@Composable
fun CopiedToClipboardSnackbar(
    modifier: Modifier,
    onSnackbarDismissed: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Snackbar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            dismissAction = {
                IconButton(onClick = onSnackbarDismissed) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_icon_content_description),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }) {
            Text(
                text = stringResource(R.string.copied_to_clipboard),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
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

@VisibleForTesting
@PreviewPhone
@PreviewFoldable
@Composable
internal fun VisitDetailScreenPreview(
    @PreviewParameter(VisitDetailPreviewConfigProvider::class) config: VisitDetailPreviewConfig
) {
    VisitasTheme {
        AppScaffold(
            uiState = config.mainActivityUiState,
            currentDestination = VisitDetailScreenDestination,
            onEvent = {},
            onNavigateToTab = {},
            onNavigate = {},
            topBarActions = listOf(
                TopBarAction(
                    contentDescription = stringResource(id = R.string.copy_data_content_description),
                    icon = Icons.Rounded.ContentCopy,
                    onClick = { }
                ),
                TopBarAction(
                    contentDescription = stringResource(id = R.string.delete),
                    icon = Icons.Rounded.Delete,
                    onClick = {}
                )
            ),
            detailFooterActions = DetailFooterActions(
                onBack = {},
                onSave = {},
                onAdd = {}
            )
        ) {
            VisitDetailScreenContent(
                householderId = config.householderId,
                uiState = config.uiState,
                appScaffoldState = remember { AppScaffoldState() }, // TODO: config.appScaffoldState
                onEvent = {},
                onNavigateUp = {},
            )
        }
    }
}
