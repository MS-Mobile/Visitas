package com.msmobile.visitas.visit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.OnIntentStateHandled
import com.msmobile.visitas.OnScaffoldConfigurationChanged
import com.msmobile.visitas.R
import com.msmobile.visitas.backup.BackupSheet
import com.msmobile.visitas.backup.BackupViewModel
import com.msmobile.visitas.extension.OnBackPressed
import com.msmobile.visitas.extension.RequestLocationPermission
import com.msmobile.visitas.extension.isKeyboardOpen
import com.msmobile.visitas.extension.launchGoogleMaps
import com.msmobile.visitas.extension.textShimmer
import com.msmobile.visitas.extension.toString
import com.msmobile.visitas.extension.tonalButtonColors
import com.msmobile.visitas.summary.SummaryViewModel
import com.msmobile.visitas.ui.views.MonthNavigator
import com.msmobile.visitas.ui.views.MonthNavigatorEvent
import com.msmobile.visitas.ui.views.PermissionRationaleSheet
import com.msmobile.visitas.ui.views.RestoreBackupDialog
import com.msmobile.visitas.ui.views.LazyColumnWithScrollbar
import com.msmobile.visitas.ui.views.SimpleSearchBar
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.ListScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.cardInnerPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.verticalFieldPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

private const val LOADING_VISITS_COUNT = 5
private const val VISIT_MAP_ANIMATION_DURATION = 300

@Destination<RootGraph>(style = ListScreenStyle::class, start = true)
@Composable
fun VisitListScreen(
    navigator: DestinationsNavigator,
    paddingValues: PaddingValues,
    summaryViewModel: SummaryViewModel,
    visitListViewModel: VisitListViewModel,
    backupViewModel: BackupViewModel,
    intentState: IntentState,
    scaffoldConfigurationChanged: OnScaffoldConfigurationChanged,
    onIntentStateHandled: OnIntentStateHandled,
) {
    val summaryUiState by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val visitListUiState by visitListViewModel.uiState.collectAsStateWithLifecycle()
    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val onSummaryEvent = summaryViewModel::onEvent
    val onVisitListEvent = visitListViewModel::onEvent
    val onBackupSheetEvent = backupViewModel::onEvent
    val onMonthPickerEvent = summaryViewModel::onMonthPickerEvent
    val onNavigate = { direction: Direction ->
        navigator.navigate(direction)
    }
    val isKeyboardOpen by isKeyboardOpen()
    val onVisitMapEvent = { visitMapEvent: VisitsMapEvent ->
        visitListViewModel.onEvent(
            VisitListViewModel.UiEvent.VisitMapEventTriggered(
                visitMapEvent = visitMapEvent
            )
        )
    }
    LaunchedEffect(key1 = isKeyboardOpen) {
        scaffoldConfigurationChanged(
            MainActivityViewModel.ScaffoldState(
                showBottomBar = !isKeyboardOpen,
                showFAB = true,
                showTimerFAB = false
            )
        )
    }
    OnBackPressed { }
    VisitListScreenContent(
        paddingValues = paddingValues,
        summaryUiState = summaryUiState,
        visitListUiState = visitListUiState,
        backupUiState = backupUiState,
        intentState = intentState,
        onSummaryEvent = onSummaryEvent,
        onVisitListEvent = onVisitListEvent,
        onBackupSheetEvent = onBackupSheetEvent,
        onMonthPickerEvent = onMonthPickerEvent,
        onNavigate = onNavigate,
        onIntentStateHandled = onIntentStateHandled,
        onVisitMapEvent = onVisitMapEvent
    )
}

@Composable
private fun VisitListScreenContent(
    paddingValues: PaddingValues,
    summaryUiState: SummaryViewModel.UiState,
    visitListUiState: VisitListViewModel.UiState,
    backupUiState: BackupViewModel.UiState,
    intentState: IntentState,
    onSummaryEvent: (SummaryViewModel.UiEvent) -> Unit,
    onVisitListEvent: (VisitListViewModel.UiEvent) -> Unit,
    onBackupSheetEvent: (BackupViewModel.UiEvent) -> Unit,
    onMonthPickerEvent: (MonthNavigatorEvent) -> Unit,
    onNavigate: (Direction) -> Unit,
    onIntentStateHandled: OnIntentStateHandled,
    onVisitMapEvent: (VisitsMapEvent) -> Unit
) {
    intentState
    Column(
        verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
    ) {
        SummaryCard(
            summaryUiState = summaryUiState,
            visitListUiState = visitListUiState,
            onSummaryEvent = onSummaryEvent,
            onVisitListEvent = onVisitListEvent,
            onBackupEvent = onBackupSheetEvent,
            onMonthPickerEvent = onMonthPickerEvent,
        )
        VisitsList(
            modifier = Modifier.padding(
                start = borderPadding,
                end = borderPadding
            ),
            paddingValues = paddingValues,
            visitListUiState = visitListUiState,
            onVisitListEvent = onVisitListEvent,
            onNavigate = onNavigate
        )
        VisitMapSheet(
            isVisible = visitListUiState.showVisitMapSheet,
            currentCoordinate = visitListUiState.currentCoordinates,
            visitMapState = visitListUiState.visitMapState,
            onDismiss = {
                onVisitListEvent(VisitListViewModel.UiEvent.VisitMapSheetDismissed)
            },
            onVisitMapEvent = onVisitMapEvent
        )
        PermissionRationaleSheet(
            isVisible = visitListUiState.showLocationRationale,
            message = stringResource(R.string.location_permission_message),
            icon = Icons.Rounded.LocationOn,
            onDismiss = {
                onVisitListEvent(VisitListViewModel.UiEvent.LocationRationaleDismissed)
            },
            onConfirm = {
                onVisitListEvent(VisitListViewModel.UiEvent.LocationRationaleAccepted)
            }
        )
        BackupSheet(
            isVisible = visitListUiState.showBackupSheet,
            uiState = backupUiState,
            onBackupSheetEvent = { backupEvent ->
                onBackupSheetEvent(backupEvent)
                onVisitListEvent(VisitListViewModel.UiEvent.BackupSheetDismissed)
            },
            onDismiss = {
                onVisitListEvent(VisitListViewModel.UiEvent.BackupSheetDismissed)
                onBackupSheetEvent(BackupViewModel.UiEvent.BackupCanceled)
            }
        )
        when (val previewBackupFileState = visitListUiState.previewBackupFileState) {
            VisitListViewModel.PreviewBackupFileState.None -> {
                // No preview state, do nothing
            }

            is VisitListViewModel.PreviewBackupFileState.Previewing -> {
                val confirmRestoreEvent = BackupViewModel.UiEvent.RestoreBackup(
                    previewBackupFileState.fileUri,
                    successMessage = stringResource(id = R.string.restore_backup_success),
                    errorMessage = stringResource(id = R.string.restore_backup_failure)
                )
                RestoreBackupDialog(
                    onDismiss = {
                        onVisitListEvent(VisitListViewModel.UiEvent.RestorePreviewedBackupDialogDismissed)
                    },
                    onConfirm = {
                        onBackupSheetEvent(confirmRestoreEvent)
                        onVisitListEvent(VisitListViewModel.UiEvent.RestorePreviewedBackupDialogDismissed)
                    }
                )
            }
        }
        if (visitListUiState.showLocationPermissionDialog) {
            RequestLocationPermission {
                onVisitListEvent(VisitListViewModel.UiEvent.LocationPermissionDialogShown)
                onVisitListEvent(VisitListViewModel.UiEvent.LocationPermissionGranted)
            }
        }
        when (backupUiState.backupResult) {
            is BackupViewModel.BackupResult.BackupCreationSuccess -> {
                onBackupSheetEvent(BackupViewModel.UiEvent.BackupResultAcknowledged)
            }

            is BackupViewModel.BackupResult.RestoreFailure -> {
                onBackupSheetEvent(BackupViewModel.UiEvent.BackupResultAcknowledged)
            }

            is BackupViewModel.BackupResult.RestoreSuccess -> {
                onBackupSheetEvent(BackupViewModel.UiEvent.BackupResultAcknowledged)
                onVisitListEvent(VisitListViewModel.UiEvent.BackupRestoredSuccessfully)
            }

            null -> {
                // No backup result, do nothing
            }
        }
        when (intentState) {
            IntentState.None -> {
                // No intent state, do nothing
            }

            is IntentState.PreviewBackupFile -> {
                onVisitListEvent(
                    VisitListViewModel.UiEvent.BackupFilePreviewed(intentState.uri)
                )
                onIntentStateHandled()
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summaryUiState: SummaryViewModel.UiState,
    visitListUiState: VisitListViewModel.UiState,
    onSummaryEvent: (SummaryViewModel.UiEvent) -> Unit,
    onVisitListEvent: (VisitListViewModel.UiEvent) -> Unit,
    onBackupEvent: (BackupViewModel.UiEvent) -> Unit,
    onMonthPickerEvent: (MonthNavigatorEvent) -> Unit
) {
    val searchValue = visitListUiState.filter.search
    val returnVisitCount = summaryUiState.returnVisitCount
    val bibleStudyCount = summaryUiState.bibleStudyCount
    val fieldServiceHours = summaryUiState.totalFieldServiceSeconds
    val currentMonth = summaryUiState.selectedMonth
    val shouldShowSummaryDetails = summaryUiState.shouldShowSummaryDetails
    val shouldShowSummaryHours = summaryUiState.shouldShowSummaryHours

    SummaryCardContent(
        searchValue = searchValue,
        returnVisitCount = returnVisitCount,
        bibleStudyCount = bibleStudyCount,
        fieldServiceHours = fieldServiceHours,
        currentMonth = currentMonth,
        shouldShowSummaryDetails = shouldShowSummaryDetails,
        shouldShowSummaryHours = shouldShowSummaryHours,
        onSummaryEvent = onSummaryEvent,
        onMonthPickerEvent = onMonthPickerEvent,
        onVisitListEvent = onVisitListEvent,
        onBackupEvent = onBackupEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryCardContent(
    searchValue: String,
    returnVisitCount: String,
    bibleStudyCount: String,
    fieldServiceHours: String,
    currentMonth: LocalDateTime,
    shouldShowSummaryDetails: Boolean,
    shouldShowSummaryHours: Boolean,
    onSummaryEvent: (event: SummaryViewModel.UiEvent) -> Unit,
    onMonthPickerEvent: (monthNavigatorEvent: MonthNavigatorEvent) -> Unit,
    onVisitListEvent: (uiEvent: VisitListViewModel.UiEvent) -> Unit,
    onBackupEvent: (BackupViewModel.UiEvent) -> Unit
) {
    val isSearchEmpty = searchValue.isEmpty()
    LaunchedEffect(key1 = null) {
        onSummaryEvent(SummaryViewModel.UiEvent.ViewCreated)
    }
    val showDetailsButtonIcon = if (shouldShowSummaryDetails) {
        Icons.Rounded.KeyboardArrowUp
    } else {
        Icons.Rounded.KeyboardArrowDown
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalFieldPadding,
                end = horizontalFieldPadding,
                top = verticalFieldPadding,
                bottom = borderPadding
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleSearchBar(
                modifier = Modifier.weight(1f, true),
                search = searchValue,
                onValueChange = { search ->
                    onVisitListEvent(VisitListViewModel.UiEvent.SearchChanged(search))
                },
                isSearchEmpty = isSearchEmpty,
                onFilterCleared = {
                    onVisitListEvent(VisitListViewModel.UiEvent.FilterCleared)
                }
            )
            IconButton(onClick = {
                onSummaryEvent(
                    SummaryViewModel.UiEvent.SummaryMenuSelected(
                        option = SummaryViewModel.SummaryMenuOption.ShowDetails
                    )
                )
            }) {
                Icon(imageVector = showDetailsButtonIcon, contentDescription = null)
            }
        }
        SummaryCardDetails(
            returnVisitCount = returnVisitCount,
            bibleStudyCount = bibleStudyCount,
            fieldServiceHours = fieldServiceHours,
            currentMonth = currentMonth,
            shouldShowSummaryDetails = shouldShowSummaryDetails,
            shouldShowSummaryHours = shouldShowSummaryHours,
            onVisitListEvent = onVisitListEvent,
            onBackupEvent = onBackupEvent,
            onMonthPickerEvent = onMonthPickerEvent
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColumnScope.SummaryCardDetails(
    returnVisitCount: String,
    bibleStudyCount: String,
    fieldServiceHours: String,
    currentMonth: LocalDateTime,
    shouldShowSummaryDetails: Boolean,
    shouldShowSummaryHours: Boolean,
    onVisitListEvent: (VisitListViewModel.UiEvent) -> Unit,
    onBackupEvent: (BackupViewModel.UiEvent) -> Unit,
    onMonthPickerEvent: (MonthNavigatorEvent) -> Unit
) {
    AnimatedVisibility(visible = shouldShowSummaryDetails) {
        Column {
            Spacer(modifier = Modifier.padding(vertical = verticalFieldPadding))
            MonthNavigator(
                modifier = Modifier.fillMaxWidth(),
                dateTime = currentMonth,
                onEvent = onMonthPickerEvent
            )
            Spacer(modifier = Modifier.padding(vertical = verticalFieldPadding))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = horizontalFieldPadding.times(2),
                        vertical = verticalFieldPadding
                    ),
                text = stringResource(id = R.string.monthly_summary),
                style = MaterialTheme.typography.bodyLargeEmphasized
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = horizontalFieldPadding.times(2))
                ) {
                    if (shouldShowSummaryHours) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.field_service_hours),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(text = fieldServiceHours, textAlign = TextAlign.End)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.return_visits),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(text = returnVisitCount, textAlign = TextAlign.End)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.bible_studies)
                                    + stringResource(id = R.string.colon),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(text = bibleStudyCount, textAlign = TextAlign.End)
                    }
                }
                IconButton(onClick = {
                    onBackupEvent(BackupViewModel.UiEvent.BackupCanceled)
                    onVisitListEvent(VisitListViewModel.UiEvent.BackupButtonClicked)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = stringResource(id = R.string.create_backup)
                    )
                }
            }
            Spacer(modifier = Modifier.padding(vertical = verticalFieldPadding))
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = horizontalFieldPadding.times(2))
            )
        }
    }
}

@Composable
private fun VisitsList(
    modifier: Modifier,
    paddingValues: PaddingValues,
    visitListUiState: VisitListViewModel.UiState,
    onVisitListEvent: (VisitListViewModel.UiEvent) -> Unit,
    onNavigate: (Direction) -> Unit,
) {
    val visitList = visitListUiState.visitList.filter { !it.hide }
    val isLoadingVisits = visitListUiState.isLoadingVisits
    val showNearbyVisits = visitListUiState.showNearbyVisits
    val listBottomPadding = paddingValues.calculateBottomPadding().let { dp ->
        if (dp > 0.dp) dp - verticalFieldPadding else verticalFieldPadding
    }.coerceAtLeast(0.dp)

    LaunchedEffect(key1 = null) {
        onVisitListEvent(VisitListViewModel.UiEvent.ViewCreated)
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = verticalFieldPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.visits),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = horizontalFieldPadding)
            )
            Row {
                IconButton(
                    onClick = {
                        onVisitListEvent(VisitListViewModel.UiEvent.VisitMapSheetClicked)
                    }) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                VisitListFilterMenu(uiState = visitListUiState, onEvent = onVisitListEvent)
            }
        }
        val listState = rememberLazyListState()
        LazyColumnWithScrollbar(listState = listState) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(verticalFieldPadding),
                contentPadding = PaddingValues(
                    top = verticalFieldPadding,
                    bottom = listBottomPadding
                )
            ) {
                if (isLoadingVisits && visitList.isEmpty()) {
                    items(LOADING_VISITS_COUNT) {
                        VisitCardSkeleton()
                    }
                } else if (visitList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxHeight()
                                .fillParentMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FindInPage,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.no_visits_found),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = borderPadding)
                            )
                        }
                    }
                } else {
                    items(
                        items = visitList,
                        key = { visit -> visit.visitId }) { visit ->
                        VisitCard(
                            visit = visit,
                            isLoading = false,
                            showNearbyVisits = showNearbyVisits,
                            onEvent = onVisitListEvent,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VisitCardSkeleton() {
    val householderName = stringResource(R.string.placeholder_medium_string)
    val householderAddress = stringResource(R.string.placeholder_long_string)
    val subjectPreview = stringResource(R.string.placeholder_very_long_string)
    val visit = remember {
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = householderName,
            householderAddress = householderAddress,
            date = LocalDateTime.now(),
            isDone = false,
            hasToBeRescheduled = false,
            isPendingVisitMenuExpanded = false,
            subjectPreview = subjectPreview,
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0,
            householderAddressDistance = AddressProvider.AddressDistance.Medium(300f),
            subject = ""
        )
    }
    VisitCard(
        visit = visit,
        isLoading = true,
        showNearbyVisits = true,
        onEvent = {},
        onNavigate = {}
    )
}

@Composable
private fun VisitListFilterMenu(
    uiState: VisitListViewModel.UiState,
    onEvent: (VisitListViewModel.UiEvent) -> Unit
) {
    IconButton(onClick = {
        onEvent(VisitListViewModel.UiEvent.VisitsFilterButtonClicked)
    }) {
        Icon(imageVector = Icons.Rounded.FilterList, contentDescription = null)
        DropdownMenu(
            expanded = uiState.isVisitsFilterMenuExpanded,
            onDismissRequest = {
                onEvent(VisitListViewModel.UiEvent.VisitsFilterMenuDismissed)
            }) {

            // Date filter
            Text(
                modifier = Modifier.padding(borderPadding),
                text = stringResource(id = R.string.filter_visits)
            )
            HorizontalDivider()
            uiState.visitsFilterOptions.map { option ->
                DropdownMenuItem(text = {
                    Text(text = stringResource(id = option.description.textResId))
                }, trailingIcon = {
                    if (uiState.selectedVisitFilterOption == option) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                    }
                }, onClick = {
                    onEvent(VisitListViewModel.UiEvent.VisitsFilterOptionSelected(option = option))
                })
            }

            // Distance filter
            HorizontalDivider()
            Text(
                modifier = Modifier.padding(borderPadding),
                text = stringResource(id = R.string.visit_distance)
            )
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(VisitListViewModel.UiEvent.ShowNearbyVisitsToggled(!uiState.showNearbyVisits))
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(borderPadding),
                    text = stringResource(id = R.string.nearby_visits)
                )
                Checkbox(checked = uiState.showNearbyVisits, onCheckedChange = {
                    onEvent(VisitListViewModel.UiEvent.ShowNearbyVisitsToggled(it))
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitCard(
    visit: VisitListViewModel.VisitHouseholderState,
    isLoading: Boolean,
    showNearbyVisits: Boolean,
    onEvent: (VisitListViewModel.UiEvent) -> Unit,
    onNavigate: (Direction) -> Unit,
) {
    val dateTextColor = MaterialTheme.colorScheme.primary
    val isHouseholderAddressNearby =
        visit.householderAddressDistance is AddressProvider.AddressDistance.Nearby
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors()
            .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = {
            onNavigate(VisitDetailScreenDestination(visit.householderId))
        }
    ) {
        Column(
            modifier = Modifier.padding(cardInnerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.textShimmer(isLoading),
                    style = MaterialTheme.typography.titleMedium,
                    text = visit.householderName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showNearbyVisits && isHouseholderAddressNearby) {
                        Text(
                            text = stringResource(id = R.string.nearby_visit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    }
                    VerticalDivider(color = Color.Transparent, thickness = horizontalFieldPadding)
                    if (!visit.isDone) {
                        AnimatedVisibility(!isLoading) {
                            IconButton(
                                modifier = Modifier.size(26.dp),
                                onClick = {
                                    onEvent(
                                        VisitListViewModel.UiEvent.PendingVisitMenuClicked(
                                            visit
                                        )
                                    )
                                }) {
                                Icon(
                                    imageVector = Icons.Rounded.Update,
                                    contentDescription = stringResource(
                                        id = R.string.more_options
                                    )
                                )
                                PendingVisitMenu(visit, onEvent)
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.textShimmer(isLoading),
                    text = visit.date.toString(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = dateTextColor
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    modifier = Modifier
                        .textShimmer(isLoading)
                        .weight(1f),
                    text = visit.subjectPreview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedVisibility(
                    visible = !isLoading
                ) {
                    if (!isLoading && visit.householderAddressDistance !is AddressProvider.AddressDistance.NoData) {
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                val latitude = visit.householderLatitude ?: return@IconButton
                                val longitude = visit.householderLongitude ?: return@IconButton
                                val householderAddress = visit.householderAddress
                                context.launchGoogleMaps(householderAddress, latitude, longitude)
                            }) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingVisitMenu(
    visit: VisitListViewModel.VisitHouseholderState,
    onEvent: (VisitListViewModel.UiEvent) -> Unit
) {
    DropdownMenu(
        expanded = visit.isPendingVisitMenuExpanded,
        onDismissRequest = {
            onEvent(VisitListViewModel.UiEvent.PendingVisitMenuClicked(visit))
        }) {
        Text(
            modifier = Modifier.padding(borderPadding),
            text = stringResource(id = R.string.visit_reschedule)
        )
        HorizontalDivider()
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_today))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitToday(visit))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_tomorrow))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitTomorrow(visit))
        })
        HorizontalDivider()
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_monday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.MONDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_tuesday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.TUESDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_wednesday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.WEDNESDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_thursday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.THURSDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_friday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.FRIDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_saturday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.SATURDAY))
        })
        DropdownMenuItem(text = {
            Text(text = stringResource(id = R.string.reschedule_visit_sunday))
        }, onClick = {
            onEvent(VisitListViewModel.UiEvent.RescheduleVisitSelected(visit, DayOfWeek.SUNDAY))
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.VisitMapSheet(
    isVisible: Boolean,
    currentCoordinate: Pair<Double, Double>,
    visitMapState: VisitMapState,
    onDismiss: () -> Unit,
    onVisitMapEvent: (VisitsMapEvent) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = tween(VISIT_MAP_ANIMATION_DURATION),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(VISIT_MAP_ANIMATION_DURATION)),
        exit = slideOutVertically(
            animationSpec = tween(VISIT_MAP_ANIMATION_DURATION),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(VISIT_MAP_ANIMATION_DURATION))
    ) {
        BasicAlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyLoadedVisitsMap(
                    currentCoordinate = currentCoordinate,
                    visitMapState = visitMapState,
                    onVisitMapEvent = onVisitMapEvent
                )

                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = tonalButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyLoadedVisitsMap(
    currentCoordinate: Pair<Double, Double>,
    visitMapState: VisitMapState,
    onVisitMapEvent: (VisitsMapEvent) -> Unit
) {
    var didLoadMap by remember { mutableStateOf(false) }
    val didLoadMapData = visitMapState is VisitMapState.Visits
    val isMapLoading = visitMapState is VisitMapState.Loading
    val didFailLoadingMap = visitMapState is VisitMapState.Error
    val noMapData = visitMapState is VisitMapState.Empty

    // Use LaunchedEffect to delay the map loading and prevent UI hanging
    LaunchedEffect(Unit) {
        // Give the UI a frame to render the loading state first
        delay(100)
        didLoadMap = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!didLoadMap || isMapLoading) {
            VisitMapLoadingState()
        } else if (didFailLoadingMap) {
            VisitMapErrorState()
        } else if (noMapData) {
            VisitMapEmptyState()
        } else if (didLoadMapData) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                VisitsMap(
                    currentLocation = currentCoordinate,
                    visitMapState = visitMapState,
                    onMapEvent = onVisitMapEvent
                )
            }
        }
    }
}

@Composable
private fun VisitMapLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.padding(16.dp))
        Text(
            text = stringResource(R.string.loading_map),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VisitMapEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_visits_to_show_on_the_map),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VisitMapErrorState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_loading_visits_map),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun VisitListScreenPreview(
    @PreviewParameter(VisitListPreviewConfigProvider::class) config: VisitListPreviewConfig
) {
    AppScaffold(
        uiState = config.mainActivityUiState,
        currentDestination = VisitListScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) { paddingValues ->
        VisitListScreenContent(
            paddingValues = paddingValues,
            summaryUiState = config.summaryUiState,
            visitListUiState = config.visitListUiState,
            backupUiState = BackupViewModel.UiState(),
            intentState = IntentState.None,
            onSummaryEvent = {},
            onVisitListEvent = {},
            onBackupSheetEvent = {},
            onMonthPickerEvent = {},
            onNavigate = {},
            onIntentStateHandled = {},
            onVisitMapEvent = {}
        )
    }
}
