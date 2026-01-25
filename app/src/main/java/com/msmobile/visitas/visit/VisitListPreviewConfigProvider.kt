package com.msmobile.visitas.visit

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.summary.SummaryViewModel
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.IntentState
import java.time.LocalDateTime
import java.util.UUID

class VisitListPreviewConfigProvider : PreviewParameterProvider<VisitListPreviewConfig> {
    override val values: Sequence<VisitListPreviewConfig> = sequenceOf(
        VisitListPreviewConfig(
            configName = "Summary details collapsed",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState,
        ),
        VisitListPreviewConfig(
            configName = "Summary details expanded",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState.copy(shouldShowSummaryDetails = true),
            visitListUiState = previewVisitListUiState
        ),
        VisitListPreviewConfig(
            configName = "Loading visits",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(isLoadingVisits = true)
        ),
        VisitListPreviewConfig(
            configName = "Filtering visits",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(
                visitList = emptyList(),
                filter = previewVisitListUiState.filter.copy(search = "John")
            )
        ),
        VisitListPreviewConfig(
            configName = "Location rationale",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(
                showLocationRationale = true
            )
        )
    )

    override fun getDisplayName(index: Int): String? {
        return values.elementAt(index).configName
    }
}

data class VisitListPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val summaryUiState: SummaryViewModel.UiState,
    val visitListUiState: VisitListViewModel.UiState,
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    scaffoldState = MainActivityViewModel.ScaffoldState(
        showBottomBar = true,
        showFAB = true
    ),
    eventState = MainActivityViewModel.UiEventState.Idle,
    isTimerRunning = false,
    intentState = IntentState.None
)

private val previewSummaryUiState = SummaryViewModel.UiState(
    returnVisitCount = "0",
    bibleStudyCount = "0",
    totalFieldServiceSeconds = "0",
    selectedMonth = LocalDateTime.now(),
    shouldShowSummaryDetails = false,
    isSummaryMenuExpanded = false,
    summaryFilterOptions = listOf()
)

private val previewVisitListUiState = VisitListViewModel.UiState(
    visitList = listOf(
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "John Doe",
            householderAddress = "542 Ninth Boulevard, NY Center - Next to the train station",
            householderAddressDistance = AddressProvider.AddressDistance.Nearby(100f),
            date = LocalDateTime.now(),
            isDone = false,
            hasToBeRescheduled = false,
            isPendingVisitMenuExpanded = false,
            subject = "Subject",
            subjectPreview = "Subject preview",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0
        ),
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "Jane Doe",
            householderAddress = "123 Main St",
            date = LocalDateTime.now(),
            isDone = false,
            hasToBeRescheduled = true,
            isPendingVisitMenuExpanded = false,
            subjectPreview = "Subject preview",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0,
            householderAddressDistance = AddressProvider.AddressDistance.FarAway(600f),
            subject = "Subject"
        ),
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "Peter Doe",
            householderAddress = "123 Main St",
            date = LocalDateTime.now(),
            isDone = false,
            hasToBeRescheduled = false,
            isPendingVisitMenuExpanded = false,
            subjectPreview = "Subject preview",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0,
            householderAddressDistance = AddressProvider.AddressDistance.NoData,
            subject = "Subject"
        ),
    ),
    selectedTabIndex = 0,
    filter = VisitListViewModel.VisitFilter(
        search = "",
        dateFilter = VisitListViewModel.VisitDateFilter.All,
        distanceFilter = VisitListViewModel.VisitDistanceFilter.All
    ),
    visitsFilterOptions = VisitListDateFilterOption.entries,
    selectedVisitFilterOption = VisitListDateFilterOption.ScheduledForNextDays,
    isVisitsFilterMenuExpanded = false,
    selectedDate = LocalDateTime.now(),
    showLocationRationale = false,
    showLocationPermissionDialog = false,
    isLoadingVisits = false,
    showNearbyVisits = true,
    showBackupSheet = false,
    showVisitMapSheet = false,
    currentCoordinates = Pair(0.0, 0.0),
    visitMapState = VisitMapState.Empty,
    previewBackupFileState = VisitListViewModel.PreviewBackupFileState.None
)

