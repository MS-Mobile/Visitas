package com.msmobile.visitas.visit

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.summary.SummaryViewModel
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.IntentState
import java.time.LocalDateTime
import java.util.UUID

private val previewDate1 = LocalDateTime.of(2024, 1, 15, 10, 12)
private val previewDate2 = previewDate1.plusWeeks(1)
private val previewDate3 = previewDate1.plusWeeks(2)

@VisibleForTesting
internal class VisitListPreviewConfigProvider : PreviewParameterProvider<VisitListPreviewConfig> {
    override val values: Sequence<VisitListPreviewConfig> = sequenceOf(
        VisitListPreviewConfig(
            configName = "Summary details collapsed",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState
        ),
        VisitListPreviewConfig(
            configName = "Summary details expanded",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState.copy(shouldShowSummaryDetails = true),
            visitListUiState = previewVisitListUiState
        ),
        VisitListPreviewConfig(
            configName = "Bible students sheet expanded",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState.copy(
                shouldShowSummaryDetails = true,
                bibleStudyCount = "2",
                bibleStudentNames = listOf("Mary Magdalene", "Nicodemus"),
                isBibleStudentsSheetVisible = true
            ),
            visitListUiState = previewVisitListUiState
        ),
        VisitListPreviewConfig(
            configName = "Summary menu expanded",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState.copy(isSummaryMenuExpanded = true),
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
                filter = previewVisitListUiState.filter.copy(search = "Mary")
            )
        ),
        VisitListPreviewConfig(
            configName = "Location rationale",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(
                showLocationRationale = true
            )
        ),
        VisitListPreviewConfig(
            configName = "With draft visit",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(
                visitList = listOf(
                    previewVisitListUiState.visitList[0].copy(isDraft = true),
                    previewVisitListUiState.visitList[1],
                    previewVisitListUiState.visitList[2],
                )
            )
        ),
        VisitListPreviewConfig(
            configName = "Filter options expanded",
            mainActivityUiState = previewMainActivityUiState,
            summaryUiState = previewSummaryUiState,
            visitListUiState = previewVisitListUiState.copy(
                visitList = listOf(
                    previewVisitListUiState.visitList[0],
                    previewVisitListUiState.visitList[1],
                    previewVisitListUiState.visitList[2],
                ),
                isVisitsFilterMenuExpanded = true
            )
        )
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class VisitListPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val summaryUiState: SummaryViewModel.UiState,
    val visitListUiState: VisitListViewModel.UiState
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

private val previewSummaryUiState = SummaryViewModel.UiState(
    returnVisitCount = "0",
    bibleStudyCount = "0",
    bibleStudentNames = listOf(),
    isBibleStudentsSheetVisible = false,
    selectedMonth = previewDate1,
    shouldShowSummaryDetails = false,
    isSummaryMenuExpanded = false,
    summaryFilterOptions = listOf()
)

private val previewVisitListUiState = VisitListViewModel.UiState(
    visitList = listOf(
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "Mary Magdalene",
            householderAddress = "12 Olive Tree Street - Near the Garden of Gethsemane",
            householderAddressDistance = AddressProvider.AddressDistance.Nearby(100f),
            date = previewDate1,
            isDone = false,
            isDraft = false,
            hasToBeRescheduled = false,
            isPendingVisitMenuExpanded = false,
            subject = "What is God's Kingdom?",
            subjectPreview = "What is God's Kingdom? — Daniel 2:44",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0
        ),
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "Joseph of Arimathea",
            householderAddress = "45 Cedar Avenue",
            date = previewDate2,
            isDone = false,
            isDraft = false,
            hasToBeRescheduled = true,
            isPendingVisitMenuExpanded = false,
            subjectPreview = "The resurrection of the dead — John 5:28, 29",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0,
            householderAddressDistance = AddressProvider.AddressDistance.FarAway(600f),
            subject = "The resurrection of the dead"
        ),
        VisitListViewModel.VisitHouseholderState(
            householderId = UUID.randomUUID(),
            householderName = "Nicodemus",
            householderAddress = "7 Pharisee Street",
            date = previewDate3,
            isDone = false,
            isDraft = false,
            hasToBeRescheduled = false,
            isPendingVisitMenuExpanded = false,
            subjectPreview = "Who is Jesus Christ? — Luke 1:31-33",
            hide = false,
            visitId = UUID.randomUUID(),
            type = VisitType.FIRST_VISIT,
            householderLatitude = 0.0,
            householderLongitude = 0.0,
            householderAddressDistance = AddressProvider.AddressDistance.NoData,
            subject = "Who is Jesus Christ?"
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
    selectedDate = previewDate1,
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

