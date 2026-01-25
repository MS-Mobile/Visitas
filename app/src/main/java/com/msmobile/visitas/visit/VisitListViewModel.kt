package com.msmobile.visitas.visit

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.extension.containsAllWords
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.UserLocationProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.next
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class VisitListViewModel
@Inject
constructor(
    private val moshi: Moshi,
    private val dispatchers: DispatcherProvider,
    private val visitRepository: VisitRepository,
    private val visitHouseholderRepository: VisitHouseholderRepository,
    private val preferenceRepository: PreferenceRepository,
    private val addressProvider: AddressProvider,
    private val userLocationProvider: UserLocationProvider,
    private val permissionChecker: PermissionChecker,
    private val osrmRoutingProvider: com.msmobile.visitas.routing.OsrmRoutingProvider,
    private val calendarEventManager: CalendarEventManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            visitList = listOf(),
            filter = VisitFilter(
                search = "",
                dateFilter = VisitDateFilter.All,
                distanceFilter = VisitDistanceFilter.All
            ),
            selectedDate = LocalDateTime.now(),
            isVisitsFilterMenuExpanded = false,
            selectedTabIndex = 0,
            visitsFilterOptions = listOf(),
            selectedVisitFilterOption = VisitListDateFilterOption.All,
            showLocationRationale = false,
            showLocationPermissionDialog = false,
            isLoadingVisits = false,
            showNearbyVisits = false,
            showBackupSheet = false,
            showVisitMapSheet = false,
            currentCoordinates = Pair(
                userLocationProvider.location.value.latitudeOrDefault,
                userLocationProvider.location.value.longitudeOrDefault
            ),
            visitMapState = VisitMapState.Empty,
            previewBackupFileState = PreviewBackupFileState.None
        )
    )
    private val visitMapAdapter by lazy(::createVisitMapAdapter)

    private var nextRouteCalcJob: Job? = null
    private var nextRouteCalcInterval: Duration = INITIAL_ROUTE_CALC_INTERNAL
    private var enableNearbyVisitsAfterPermission = false

    val uiState: StateFlow<UiState> = _uiState

    init {
        userLocationProvider.location
            .onCompletion { stopTrackingLocation() }
            .onEach(::onLocationChanged)
            .launchIn(viewModelScope)
        uiState
            .map(::mapVisibleItems)
            .distinctUntilChanged()
            .onEach(::updateVisitMapState)
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
        if (hasLocationPermission()) {
            startTrackingLocation()
        }
    }

    fun onEvent(uiEvent: UiEvent) {
        when (uiEvent) {
            is UiEvent.ViewCreated -> viewCreated()
            is UiEvent.TabSelected -> tabSelected(uiEvent.tabIndex)
            is UiEvent.SearchChanged -> searchChanged(uiEvent.filter)
            is UiEvent.FilterCleared -> filterCleared()
            is UiEvent.PendingVisitMenuClicked -> pendingVisitMenuClicked(uiEvent.visit)
            is UiEvent.RescheduleVisitToday -> rescheduleVisitTodaySelected(uiEvent.visit)
            is UiEvent.RescheduleVisitTomorrow -> rescheduleVisitTomorrowSelected(uiEvent.visit)
            is UiEvent.RescheduleVisitSelected -> rescheduleVisitSelected(
                uiEvent.visit,
                uiEvent.dayOfWeek
            )

            is UiEvent.VisitsFilterButtonClicked -> visitsFilterButtonClicked()
            is UiEvent.VisitsFilterMenuDismissed -> visitsFilterMenuDismissed()
            is UiEvent.VisitsFilterOptionSelected -> visitsFilterOptionSelected(uiEvent.option)

            is UiEvent.VisitMapEventTriggered -> visitMapEventTriggered(uiEvent.visitMapEvent)

            is UiEvent.LocationPermissionGranted -> handleLocationPermissionGranted()
            is UiEvent.LocationRationaleAccepted -> handleDistanceBottomSheetConfirmed()
            is UiEvent.LocationRationaleDismissed -> handleDistanceBottomSheetDismissed()
            is UiEvent.LocationPermissionDialogShown -> handleLocationPermissionDialogShown()
            is UiEvent.ShowNearbyVisitsToggled -> handleShowNearbyVisitsToggled(uiEvent.show)
            is UiEvent.BackupButtonClicked -> handleBackupButtonClicked()
            is UiEvent.BackupSheetDismissed -> handleBackupSheetDismissed()
            is UiEvent.BackupRestoredSuccessfully -> refreshVisits()
            is UiEvent.BackupFilePreviewed -> handleBackupFilePreviewed(uiEvent.fileUri)
            is UiEvent.RestorePreviewedBackupDialogDismissed -> handleRestorePreviewedBackupDialogDismissed()
            is UiEvent.VisitMapSheetClicked -> handleVisitMapSheetClicked()
            is UiEvent.VisitMapSheetDismissed -> handleVisitMapSheetDismissed()
        }
    }

    private fun handleLocationPermissionGranted() {
        if (enableNearbyVisitsAfterPermission) {
            enableNearbyVisitsAfterPermission = false
            handleShowNearbyVisitsToggled(showNearbyVisits = true)
        }
        startTrackingLocation()
    }

    private fun handleDistanceBottomSheetConfirmed() {
        newState {
            copy(
                showLocationRationale = false,
                showLocationPermissionDialog = true
            )
        }
    }

    private fun handleDistanceBottomSheetDismissed() {
        newState {
            copy(
                showLocationRationale = false,
                showLocationPermissionDialog = false
            )
        }
    }

    private fun handleLocationPermissionDialogShown() {
        newState {
            copy(showLocationPermissionDialog = false)
        }
    }


    private fun visitsFilterOptionSelected(option: VisitListDateFilterOption) {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get().copy(visitListDateFilterOption = option)
            preferenceRepository.save(preference = preference)
        }
        newState {
            copy(selectedVisitFilterOption = option).applyFilters()
        }
    }


    private fun visitMapEventTriggered(visitMapEvent: VisitsMapEvent) {
        when (visitMapEvent) {
            is VisitsMapEvent.ErrorLoadingMap -> newState {
                copy(visitMapState = VisitMapState.Error)
            }
        }
    }

    private fun handleShowNearbyVisitsToggled(showNearbyVisits: Boolean) {
        enableNearbyVisitsAfterPermission = false

        if (!hasLocationPermission() && showNearbyVisits) {
            enableNearbyVisitsAfterPermission = true
            newState {
                copy(showLocationRationale = true)
            }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val option = if (showNearbyVisits) {
                VisitListDistanceFilterOption.Nearby
            } else {
                VisitListDistanceFilterOption.All
            }
            val preference = preferenceRepository.get().copy(visitListDistanceFilterOption = option)
            preferenceRepository.save(preference = preference)
        }
        newState {
            copy(showNearbyVisits = showNearbyVisits).applyFilters()
        }
    }

    private fun visitsFilterMenuDismissed() {
        newState {
            val isVisitsFilterMenuExpanded = false
            copy(isVisitsFilterMenuExpanded = isVisitsFilterMenuExpanded)
        }
    }

    private fun visitsFilterButtonClicked() {
        newState {
            val isVisitsFilterMenuExpanded = true
            copy(isVisitsFilterMenuExpanded = isVisitsFilterMenuExpanded)
        }
    }

    private fun rescheduleVisit(visit: VisitHouseholderState, date: LocalDateTime) {
        newState {
            val visitList = visitList.toMutableList().apply {
                set(
                    indexOf(visit),
                    visit.copy(
                        date = date,
                        hasToBeRescheduled = hasToBeRescheduled(date, visit.isDone),
                        isPendingVisitMenuExpanded = false
                    )
                )
            }
            copy(
                visitList = visitList
            )
        }
        viewModelScope.launch(dispatchers.io) {
            val visitModel = visitRepository.getById(visit.visitId).copy(date = date)
            val calendarEventId = syncCalendarEvent(
                visitModel = visitModel,
                householderName = visit.householderName
            )
            val updatedVisitModel = visitModel.copy(calendarEventId = calendarEventId)
            visitRepository.save(updatedVisitModel)
            refreshVisits()
        }
    }

    private fun pendingVisitMenuClicked(visit: VisitHouseholderState) {
        newState {
            val visitList = visitList.toMutableList().apply {
                set(
                    indexOf(visit),
                    visit.copy(isPendingVisitMenuExpanded = !visit.isPendingVisitMenuExpanded)
                )
            }
            copy(
                visitList = visitList
            )
        }
    }

    private fun tabSelected(tabIndex: Int) {
        newState {
            copy(
                selectedTabIndex = tabIndex,
                visitList = visitList
            )
        }
    }

    private fun filterCleared() {
        searchChanged("")
    }

    private fun viewCreated() {
        val uiState = _uiState.value
        if (!uiState.isLoadingVisits) {
            newState {
                copy(isLoadingVisits = true)
            }
            refreshVisits()
        }
    }

    private fun refreshVisits() {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get()
            val selectedVisitFilterOption = preference.visitListDateFilterOption
            val selectedVisitDistanceFilterOption = preference.visitListDistanceFilterOption
            val visitList = visitHouseholderRepository.getAll().map { visitHouseholder ->
                visitHouseholder.asState
            }.filterBy(_uiState.value.filter)
            val visitsFilterOptions = VisitListDateFilterOption.entries
            val showNearbyVisits =
                selectedVisitDistanceFilterOption == VisitListDistanceFilterOption.Nearby
            val userLocation = userLocationProvider.location.value
            newState {
                copy(
                    visitList = visitList,
                    visitsFilterOptions = visitsFilterOptions,
                    selectedVisitFilterOption = selectedVisitFilterOption,
                    isLoadingVisits = false,
                    showNearbyVisits = showNearbyVisits
                ).applyFilters()
                    .calculateDistanceBetweenUserAndHouseholders(userLocation)
                    .applyFilters()
            }
        }
    }

    private fun searchChanged(value: String) {
        newState {
            val filter = filter.copy(search = value)
            val visitList = visitList.filterBy(filter)
            copy(
                visitList = visitList,
                filter = filter
            )
        }
    }

    private fun rescheduleVisitTodaySelected(visit: VisitHouseholderState) {
        val date = LocalDate.now()
            .atStartOfDay()
            .withHour(visit.date.hour)
            .withMinute(visit.date.minute)
        rescheduleVisit(visit, date)
    }

    private fun rescheduleVisitTomorrowSelected(visit: VisitHouseholderState) {
        val date = LocalDate.now()
            .plusDays(1)
            .atStartOfDay()
            .withHour(visit.date.hour)
            .withMinute(visit.date.minute)
        rescheduleVisit(visit, date)
    }

    private fun rescheduleVisitSelected(visit: VisitHouseholderState, dayOfWeek: DayOfWeek) {
        val date = LocalDate.now()
            .with(next(dayOfWeek))
            .atStartOfDay()
            .withHour(visit.date.hour)
            .withMinute(visit.date.minute)
        rescheduleVisit(visit, date)
    }

    private fun handleBackupButtonClicked() {
        newState {
            copy(
                showBackupSheet = true
            )
        }
    }

    private fun handleBackupSheetDismissed() {
        newState {
            copy(
                showBackupSheet = false
            )
        }
    }

    private fun handleBackupFilePreviewed(fileUri: Uri) {
        newState {
            copy(
                previewBackupFileState = PreviewBackupFileState.Previewing(fileUri)
            )
        }
    }

    private fun handleRestorePreviewedBackupDialogDismissed() {
        newState {
            copy(
                previewBackupFileState = PreviewBackupFileState.None
            )
        }
    }

    private fun handleVisitMapSheetClicked() {
        newState {
            copy(
                showVisitMapSheet = true
            )
        }
    }

    private fun handleVisitMapSheetDismissed() {
        newState {
            copy(
                showVisitMapSheet = false
            )
        }
    }

    private fun hasToBeRescheduled(date: LocalDateTime, isDone: Boolean): Boolean {
        return !isDone && date.toLocalDate().isBefore(LocalDate.now())
    }

    private suspend fun syncCalendarEvent(visitModel: Visit, householderName: String): Long? {
        if (!calendarEventManager.hasCalendarPermission()) {
            return visitModel.calendarEventId
        }

        val title = buildCalendarEventTitle(householderName, visitModel.subject)

        return calendarEventManager.saveEvent(
            eventId = visitModel.calendarEventId,
            title = title,
            description = visitModel.subject,
            startTime = visitModel.date,
            isDone = visitModel.isDone
        )
    }

    private fun buildCalendarEventTitle(householderName: String, subject: String): String {
        return if (subject.isNotBlank()) {
            val subjectPreview = subject.lines().firstOrNull() ?: ""
            "$householderName - ${subjectPreview}"
        } else {
            householderName
        }
    }

    private fun onLocationChanged(location: UserLocationProvider.UserLocation) {
        if (location !is UserLocationProvider.UserLocation.Available) return
        newState {
            calculateDistanceBetweenUserAndHouseholders(location)
                .copy(
                    currentCoordinates = Pair(
                        location.latitude,
                        location.longitude
                    )
                )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingLocation() {
        userLocationProvider.startLocationUpdates()
    }

    private fun stopTrackingLocation() {
        userLocationProvider.stopLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        return permissionChecker.hasPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun mapVisibleItems(state: UiState): List<VisitHouseholderState> {
        return state.visitList.filter { visit -> !visit.hide }
    }

    private fun updateVisitMapState(visitList: List<VisitHouseholderState>) {
        val visitMapState = uiState.value.visitMapState

        if (visitList.isEmpty()) {
            newState {
                copy(visitMapState = VisitMapState.Empty)
            }
            return
        }

        if (visitMapState is VisitMapState.Loading) {
            return
        }

        newState {
            // Don't disturb users showing loading state during preview
            if (showVisitMapSheet && this.visitMapState is VisitMapState.Visits) {
                return@newState this
            }
            copy(visitMapState = VisitMapState.Loading)
        }

        scheduleNextRouteCalculation(visitList)
    }

    private fun scheduleNextRouteCalculation(visitList: List<VisitHouseholderState>) {
        nextRouteCalcJob?.cancel()
        nextRouteCalcJob = viewModelScope.launch(dispatchers.io) {
            val routeCalcInterval = nextRouteCalcInterval
            nextRouteCalcInterval = SUBSEQUENT_ROUTE_CALC_INTERVAL

            delay(routeCalcInterval)

            val newMapState = generateVisitMapState(visitList)
            newState {
                copy(visitMapState = newMapState)
            }
        }
    }

    private fun UiState.calculateDistanceBetweenUserAndHouseholders(
        location: UserLocationProvider.UserLocation
    ): UiState {
        if (location !is UserLocationProvider.UserLocation.Available) {
            return this
        }
        val (latitude, longitude) = location
        val visitList = visitList.map { state ->
            state.copy(
                householderAddressDistance = state.calculateDistance(
                    latitude,
                    longitude
                )
            )
        }
        return copy(
            visitList = visitList
        )
    }

    private suspend fun generateVisitMapState(
        visitList: List<VisitHouseholderState>
    ): VisitMapState {
        val visitMapData = visitList.mapNotNull { visit ->
            val latitude = visit.householderLatitude ?: return@mapNotNull null
            val longitude = visit.householderLongitude ?: return@mapNotNull null
            val householderDistance = visit.householderAddressDistance.distanceOrNull?.toInt()

            VisitMapData(
                householderName = visit.householderName,
                visitSubject = visit.subjectPreview,
                householderAddress = visit.householderAddress,
                householderLatitude = latitude,
                householderLongitude = longitude,
                householderDistance = householderDistance,
                visitOrder = 0
            )
        }

        if (visitMapData.isEmpty()) {
            return VisitMapState.Empty
        }

        // Launch route optimization in background
        val mapData = fetchOptimizedRoute(visitMapData)
        val serializedMapData = visitMapAdapter.toJson(mapData)
        val newMapState = VisitMapState.Visits(serialized = serializedMapData)

        return newMapState
    }

    private suspend fun fetchOptimizedRoute(visitMapData: List<VisitMapData>): List<VisitMapData> {
        return try {
            val currentCoordinates = _uiState.value.currentCoordinates
            val visitLocations = visitMapData.map { mapData ->
                mapData.householderLatitude to mapData.householderLongitude
            }

            val routeResult = osrmRoutingProvider.optimizeVisitRoute(
                currentLocation = currentCoordinates,
                visitLocations = visitLocations
            )

            // Update visit map data with optimized order and route geometry
            val optimizedVisitMapDataList = visitMapData.mapIndexedNotNull { visitIndex, mapData ->
                val optimizedVisit = routeResult.orderedVisits.find { optimizedVisit ->
                    optimizedVisit.originalIndex == visitIndex
                }
                val optimizedOrder = optimizedVisit?.optimizedOrder ?: return@mapIndexedNotNull null
                mapData.copy(
                    visitOrder = optimizedOrder,
                    routeGeometry = routeResult.routeGeometry
                )
            }.sortedBy { it.visitOrder }

            optimizedVisitMapDataList
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            visitMapData
        }
    }

    private fun createVisitMapAdapter(): JsonAdapter<List<VisitMapData>?> {
        return moshi.adapter<List<VisitMapData>>(
            Types.newParameterizedType(
                List::class.java,
                VisitMapData::class.java
            )
        )
    }

    private fun newState(block: UiState.() -> UiState) {
        _uiState.update(block)
    }

    private fun VisitHouseholderState.calculateDistance(
        latitude: Double,
        longitude: Double
    ): AddressProvider.AddressDistance {
        val householderLatitude =
            householderLatitude ?: return AddressProvider.AddressDistance.NoData
        val householderLongitude =
            householderLongitude ?: return AddressProvider.AddressDistance.NoData
        return addressProvider.calculateDistance(
            startLatitude = latitude,
            startLongitude = longitude,
            endLatitude = householderLatitude,
            endLongitude = householderLongitude
        )
    }

    private fun UiState.applyFilters(): UiState {
        val distanceFilter = if (showNearbyVisits) {
            VisitDistanceFilter.Nearby
        } else {
            VisitDistanceFilter.All
        }
        val filter = filter.copy(
            dateFilter = selectedVisitFilterOption.asDateFilter,
            distanceFilter = distanceFilter
        )
        val visitList = visitList.filterBy(filter)
        return copy(
            visitList = visitList,
            filter = filter
        )
    }

    private fun List<VisitHouseholderState>.filterBy(filter: VisitFilter): List<VisitHouseholderState> {
        val today = LocalDate.now()
        return map { visit ->
            val (search, dateFilter, distanceFilter) = filter
            val householderDistance = visit.householderAddressDistance
            val householderDistanceAsFilter = householderDistance.asDistanceFilter
            val visitDate = visit.date.toLocalDate()
            val isVisitDone = visit.isDone
            val matchesName = visit.householderName.containsAllWords(filter.search)
            val isSearchEmpty = search.isEmpty()
            val isFilteringByDistance = distanceFilter is VisitDistanceFilter.Nearby
            val matchesDate = dateFilter.matchesDate(
                isVisitDone = isVisitDone,
                visitDate = visitDate,
                today = today
            )
            val matchesDistance = distanceFilter.matchesDistance(
                isFilteringByDistance = isFilteringByDistance,
                householderDistanceAsFilter = householderDistanceAsFilter
            )
            val show = isSearchEmpty && (matchesDate || matchesDistance)
                    || matchesName
            visit.copy(hide = !show) to matchesDistance
        }.sortedWith(
            compareByDescending<Pair<VisitHouseholderState, Boolean>> { (_, matchesDistance) -> matchesDistance }
                .thenBy { (visit, _) -> visit.date }
        ).map { (visit, _) -> visit }
    }

    private val AddressProvider.AddressDistance.asDistanceFilter: VisitDistanceFilter
        get() {
            return when (this) {
                is AddressProvider.AddressDistance.Nearby -> VisitDistanceFilter.Nearby
                is AddressProvider.AddressDistance.NoData,
                is AddressProvider.AddressDistance.Medium,
                is AddressProvider.AddressDistance.FarAway -> VisitDistanceFilter.All
            }
        }

    private val VisitListDateFilterOption.asDateFilter: VisitDateFilter
        get() {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val afterTomorrow = tomorrow.plusDays(1)
            val dateFilter = when (this) {
                VisitListDateFilterOption.All -> VisitDateFilter.All
                VisitListDateFilterOption.PastDue -> VisitDateFilter.Missed
                VisitListDateFilterOption.ScheduledForToday -> VisitDateFilter.Specific(date = today)
                VisitListDateFilterOption.ScheduledForTomorrow -> VisitDateFilter.Specific(date = tomorrow)
                VisitListDateFilterOption.ScheduledForNextDays -> VisitDateFilter.AtLeast(date = afterTomorrow)
                VisitListDateFilterOption.Done -> VisitDateFilter.Done
            }
            return dateFilter
        }


    private val VisitHouseholder.asState: VisitHouseholderState
        get() {
            return VisitHouseholderState(
                visitId = visitId,
                subject = subject,
                subjectPreview = subject.split("\n").firstOrNull() ?: "",
                date = date,
                isDone = isDone,
                householderId = householderId,
                householderName = householderName,
                householderAddress = householderAddress,
                isPendingVisitMenuExpanded = false,
                hasToBeRescheduled = hasToBeRescheduled(date, isDone),
                type = type,
                householderLatitude = householderLatitude,
                householderLongitude = householderLongitude,
                householderAddressDistance = AddressProvider.AddressDistance.NoData,
                hide = false
            )
        }

    private fun VisitDateFilter.matchesDate(
        isVisitDone: Boolean,
        visitDate: LocalDate,
        today: LocalDate
    ): Boolean {
        return when (this) {
            is VisitDateFilter.All -> true
            is VisitDateFilter.Missed -> !isVisitDone && visitDate.isBefore(today)
            is VisitDateFilter.Specific -> !isVisitDone && this.date == visitDate
            is VisitDateFilter.AtLeast -> !isVisitDone && visitDate >= this.date
            is VisitDateFilter.Done -> isVisitDone
        }
    }

    private fun VisitDistanceFilter.matchesDistance(
        isFilteringByDistance: Boolean,
        householderDistanceAsFilter: VisitDistanceFilter
    ): Boolean {
        return isFilteringByDistance
                && this == householderDistanceAsFilter
    }

    private val UserLocationProvider.UserLocation.latitudeOrDefault: Double
        get() = when (this) {
            is UserLocationProvider.UserLocation.Available -> latitude
            UserLocationProvider.UserLocation.NotAvailable -> 0.0
        }

    private val UserLocationProvider.UserLocation.longitudeOrDefault: Double
        get() = when (this) {
            is UserLocationProvider.UserLocation.Available -> longitude
            UserLocationProvider.UserLocation.NotAvailable -> 0.0
        }

    private val AddressProvider.AddressDistance.distanceOrNull: Float?
        get() = when (this) {
            is AddressProvider.AddressDistance.NoData -> null
            is AddressProvider.AddressDistance.Nearby -> distance
            is AddressProvider.AddressDistance.Medium -> distance
            is AddressProvider.AddressDistance.FarAway -> distance
        }

    sealed class UiEvent {
        data object ViewCreated : UiEvent()
        data object FilterCleared : UiEvent()
        data object VisitsFilterButtonClicked : UiEvent()
        data object VisitsFilterMenuDismissed : UiEvent()
        data object LocationPermissionGranted : UiEvent()
        data object LocationRationaleAccepted : UiEvent()
        data object LocationRationaleDismissed : UiEvent()
        data object LocationPermissionDialogShown : UiEvent()
        data object BackupButtonClicked : UiEvent()
        data object BackupSheetDismissed : UiEvent()
        data object BackupRestoredSuccessfully : UiEvent()
        data object VisitMapSheetClicked : UiEvent()
        data object VisitMapSheetDismissed : UiEvent()

        data class VisitsFilterOptionSelected(val option: VisitListDateFilterOption) : UiEvent()

        data class VisitMapEventTriggered(val visitMapEvent: VisitsMapEvent) : UiEvent()

        data class TabSelected(val tabIndex: Int) : UiEvent()
        data class SearchChanged(val filter: String) : UiEvent()
        data class PendingVisitMenuClicked(val visit: VisitHouseholderState) : UiEvent()
        data class RescheduleVisitToday(val visit: VisitHouseholderState) : UiEvent()
        data class RescheduleVisitTomorrow(val visit: VisitHouseholderState) : UiEvent()
        data class RescheduleVisitSelected(
            val visit: VisitHouseholderState, val dayOfWeek: DayOfWeek
        ) : UiEvent()

        data class ShowNearbyVisitsToggled(val show: Boolean) : UiEvent()
        data class BackupFilePreviewed(val fileUri: Uri) : UiEvent()
        data object RestorePreviewedBackupDialogDismissed : UiEvent()
    }

    data class VisitFilter(
        val search: String,
        val dateFilter: VisitDateFilter,
        val distanceFilter: VisitDistanceFilter
    )

    sealed class VisitDateFilter {
        data object All : VisitDateFilter()
        data object Missed : VisitDateFilter()
        data class Specific(val date: LocalDate) : VisitDateFilter()
        data class AtLeast(val date: LocalDate) : VisitDateFilter()
        data object Done : VisitDateFilter()
    }

    sealed class VisitDistanceFilter {
        data object All : VisitDistanceFilter()
        data object Nearby : VisitDistanceFilter()
    }

    data class VisitHouseholderState(
        val visitId: UUID,
        val subject: String,
        val subjectPreview: String,
        val date: LocalDateTime,
        val isDone: Boolean,
        val householderId: UUID,
        val householderName: String,
        val householderAddress: String,
        val householderLatitude: Double?,
        val householderLongitude: Double?,
        val householderAddressDistance: AddressProvider.AddressDistance,
        val hide: Boolean,
        val isPendingVisitMenuExpanded: Boolean,
        val hasToBeRescheduled: Boolean,
        val type: VisitType,
    )

    sealed interface PreviewBackupFileState {
        data object None : PreviewBackupFileState
        data class Previewing(val fileUri: Uri) : PreviewBackupFileState
    }

    data class UiState(
        val visitList: List<VisitHouseholderState>,
        val filter: VisitFilter,
        val selectedDate: LocalDateTime,
        val isVisitsFilterMenuExpanded: Boolean,
        val selectedTabIndex: Int,
        val visitsFilterOptions: List<VisitListDateFilterOption>,
        val selectedVisitFilterOption: VisitListDateFilterOption,
        val showLocationRationale: Boolean,
        val showLocationPermissionDialog: Boolean,
        val isLoadingVisits: Boolean,
        val showNearbyVisits: Boolean,
        val showBackupSheet: Boolean,
        val showVisitMapSheet: Boolean,
        val currentCoordinates: Pair<Double, Double>,
        val visitMapState: VisitMapState,
        val previewBackupFileState: PreviewBackupFileState
    )

    companion object {
        private val INITIAL_ROUTE_CALC_INTERNAL = 0.seconds
        private val SUBSEQUENT_ROUTE_CALC_INTERVAL = 10.seconds
    }
}