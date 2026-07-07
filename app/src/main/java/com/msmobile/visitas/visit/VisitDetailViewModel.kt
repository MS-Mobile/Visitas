package com.msmobile.visitas.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.R
import com.msmobile.visitas.conversation.Conversation
import com.msmobile.visitas.conversation.ConversationRepository
import com.msmobile.visitas.extension.containsAllWords
import com.msmobile.visitas.extension.split
import com.msmobile.visitas.extension.subListInclusive
import com.msmobile.visitas.householder.Householder
import com.msmobile.visitas.householder.HouseholderRepository
import com.msmobile.visitas.householder.HouseholderSnapshot
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.SyncVisitCalendarEventUseCase
import com.msmobile.visitas.util.ClipboardHandler
import com.msmobile.visitas.util.DateTimeProvider
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.LatLongParser
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.StringResource
import com.msmobile.visitas.util.VisitDataFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VisitDetailViewModel
@Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val householderRepository: HouseholderRepository,
    private val visitRepository: VisitRepository,
    private val snapshotRepository: SnapshotRepository,
    private val conversationRepository: ConversationRepository,
    private val addressProvider: AddressProvider,
    private val idProvider: IdProvider,
    private val permissionChecker: PermissionChecker,
    private val calendarEventManager: CalendarEventManager,
    private val syncVisitCalendarEvent: SyncVisitCalendarEventUseCase,
    private val visitTimeValidator: VisitTimeValidator,
    private val dateTimeProvider: DateTimeProvider,
    private val latLongParser: LatLongParser,
    private val clipboardHandler: ClipboardHandler,
    private val visitDataFormatter: VisitDataFormatter
) : ViewModel() {
    private val didEditableDataChange: Boolean
        get() {
            return _uiState.value.getEditableDataSnapshot() != initialEditableData
        }
    private val _uiState = MutableStateFlow(
        UiState(
            householder = newHouseholder(),
            visitList = emptyList(),
            conversationList = emptyList(),
            visitTypeList = emptyList(),
            eventState = UiEventState.Idle
        )
    )
    private var conversations: List<Conversation> = listOf()
    @Volatile private var initialEditableData: EditableDataSnapshot? = null
    private var loadAddressAfterPermission = false
    private var isUpdatingVisit: Boolean = false
    private var isAddressFieldFocused: Boolean = false
    private var autoSaveJob: Job? = null
    private var didCreateViewAlready: Boolean = false

    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(uiEvent: UiEvent) {
        when (uiEvent) {
            is UiEvent.ViewCreated -> viewCreated(uiEvent.householderId)
            is UiEvent.HouseholderNameChanged -> nameChanged(uiEvent.value)
            is UiEvent.HouseholderAddressChanged -> addressChanged(uiEvent.value)
            is UiEvent.HouseholderNotesChanged -> notesChanged(uiEvent.value)
            is UiEvent.VisitSubjectChanged -> visitSubjectChanged(
                uiEvent.value,
                uiEvent.visit,
                uiEvent.caretPosition
            )

            is UiEvent.VisitDoneChanged -> visitDoneChanged(uiEvent.value, uiEvent.visit)
            is UiEvent.VisitDateClicked -> visitDateClicked(uiEvent.visit)
            is UiEvent.VisitDateAccepted -> visitDateAccepted(uiEvent.visit, uiEvent.dateTime)
            is UiEvent.RemoveVisitClicked -> removeVisitClicked(uiEvent.visit)
            is UiEvent.ConversationListDismissed -> conversationListDismissed(uiEvent.visit)
            is UiEvent.VisitTypeListDismissed -> visitTypeListDismissed(uiEvent.visit)
            is UiEvent.ConversationSelected -> conversationSelected(
                uiEvent.visit,
                uiEvent.conversation,
                uiEvent.caretPosition
            )

            is UiEvent.ClearSubjectClicked -> clearSubjectClicked(uiEvent.visit)

            is UiEvent.VisitTypeClicked -> visitTypeClicked(uiEvent.visit)
            is UiEvent.NextVisitSuggestionClicked -> nextVisitSuggestionClicked(uiEvent.visit)
            is UiEvent.NextVisitSuggestionAccepted -> nextVisitSuggestionAccepted(uiEvent.visit)
            is UiEvent.NextVisitSuggestionDismissed -> nextVisitSuggestionDismissed()

            is UiEvent.VisitTypeSelected -> visitTypeSelected(
                uiEvent.visit,
                uiEvent.visitType
            )

            is UiEvent.NameFocusChanged -> nameFocusChanged(uiEvent.hasFocus)
            is UiEvent.AddressFocusChanged -> addressFocusChanged(uiEvent.hasFocus)
            is UiEvent.NotesFocusChanged -> notesFocusChanged(uiEvent.hasFocus)
            is UiEvent.VisitSubjectFocusChanged -> visitSubjectFocusChanged(
                uiEvent.hasFocus,
                uiEvent.visit
            )

            is UiEvent.PreferredDayChanged -> preferredDayChanged(uiEvent.value)
            is UiEvent.PreferredTimeChanged -> preferredTimeChanged(uiEvent.value)

            UiEvent.LoadAddressClicked -> loadAddressClicked()
            UiEvent.LookUpAddressFromLatLongClicked -> lookUpAddressFromLatLongClicked()
            UiEvent.CancelClicked -> cancelClicked()
            UiEvent.UndoChangesClicked -> undoChangesClicked()
            UiEvent.UndoChangesConfirmed -> undoChangesConfirmed()
            UiEvent.UndoChangesConfirmationDismissed -> undoChangesConfirmationDismissed()
            UiEvent.DeleteClicked -> deleteClicked()
            UiEvent.DeleteAccepted -> deleteAccepted()
            UiEvent.DeleteDismissed -> deleteDismissed()
            UiEvent.SaveClicked -> saveClicked()
            UiEvent.VisitDateDismissed -> visitDateDismissed()
            UiEvent.AddVisitClicked -> addVisitClicked()
            UiEvent.ClearNameClicked -> clearNameClicked()
            UiEvent.ClearAddressClicked -> clearAddressClicked()
            UiEvent.ClearNotesClicked -> clearNotesClicked()
            UiEvent.ExpandNotesClicked -> expandNotesClicked()
            UiEvent.SnackbarDismissed -> snackbarDismissed()
            UiEvent.LocationRationaleAccepted -> handleLocationRationaleAccepted()
            UiEvent.LocationRationaleDismissed -> handleLocationRationaleDismissed()
            UiEvent.LocationPermissionGranted -> handleLocationPermissionGranted()
            UiEvent.LocationPermissionDialogShown -> handleLocationPermissionDialogShown()
            UiEvent.CalendarRationaleAccepted -> handleCalendarRationaleAccepted()
            UiEvent.CalendarRationaleDismissed -> handleCalendarRationaleDismissed()
            UiEvent.CalendarPermissionGranted -> handleCalendarPermissionGranted()
            UiEvent.CalendarPermissionDialogShown -> handleCalendarPermissionDialogShown()
            UiEvent.CopyVisitDataClicked -> copyVisitDataClicked()
        }
    }

    private fun snackbarDismissed() {
        newState {
            copy(eventState = UiEventState.Idle)
        }
    }

    private fun copyVisitDataClicked() {
        val state = _uiState.value
        val householder = state.householder
        val nextPendingVisit = state.visitList.firstOrNull { !it.isDone }

        val text = visitDataFormatter.format(
            name = householder.name,
            address = householder.address,
            latitude = householder.addressLatitude,
            longitude = householder.addressLongitude,
            notes = householder.notes,
            preferredDay = householder.preferredDay,
            preferredTime = householder.preferredTime,
            nextPendingVisitSubject = nextPendingVisit?.subject,
            nextPendingVisitDate = nextPendingVisit?.date
        )

        clipboardHandler.copyToClipboard(text)

        newState {
            copy(eventState = UiEventState.CopiedToClipboard)
        }
    }

    private fun loadAddressClicked() {
        loadAddressAfterPermission = false

        if (!hasLocationPermission()) {
            loadAddressAfterPermission = true
            newState {
                copy(showLocationRationale = true)
            }
            return
        }

        loadAddress()
    }

    private fun lookUpAddressFromLatLongClicked() {
        val latLong = latLongParser.parse(_uiState.value.householder.address).getOrElse {
            newState {
                copy(
                    eventState = UiEventState.NoAddressFound,
                    householder = householder.copy(isLoadingAddress = false)
                )
            }
            return
        }

        newState {
            copy(
                householder = householder.copy(
                    editable = householder.editable.copy(
                        addressLatitude = latLong.latitude,
                        addressLongitude = latLong.longitude
                    ),
                    isLoadingAddress = true
                ),
                eventState = UiEventState.Idle
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val addressInfo = addressProvider.getAddressFromLatLong(
                latLong.latitude,
                latLong.longitude
            )

            newState {
                when (addressInfo) {
                    AddressProvider.AddressSpecs.NoData -> copy(
                        householder = householder.copy(isLoadingAddress = false),
                        eventState = UiEventState.NoAddressFound
                    )

                    is AddressProvider.AddressSpecs.Data -> {
                        val addressState = resolveAddressState(
                            addressInfo.address,
                            hasFocus = true
                        )
                        copy(
                            householder = householder.copy(
                                editable = householder.editable.copy(address = addressInfo.address),
                                isLoadingAddress = false,
                                addressState = addressState
                            ),
                            eventState = UiEventState.Idle
                        )
                    }
                }
            }
        }
    }

    private fun loadAddress() {
        newState {
            copy(
                householder = householder.copy(isLoadingAddress = true),
                eventState = UiEventState.Idle
            )
        }
        viewModelScope.launch(dispatchers.io) {
            val addressList = addressProvider.getAddressListFromCurrentLocation()
            val addressInfo = addressList.firstOrNull()
            newState {
                when (addressInfo) {
                    is AddressProvider.AddressSpecs.NoData, null -> copy(
                        householder = householder.copy(isLoadingAddress = false),
                        eventState = UiEventState.NoAddressFound
                    )

                    is AddressProvider.AddressSpecs.Data -> {
                        val addressState = resolveAddressState(
                            address = addressInfo.address,
                            hasFocus = true
                        )
                        copy(
                            householder = householder.copy(
                                editable = householder.editable.copy(
                                    address = addressInfo.address,
                                    addressLatitude = addressInfo.latitude,
                                    addressLongitude = addressInfo.longitude
                                ),
                                isLoadingAddress = false,
                                addressState = addressState
                            ),
                            eventState = UiEventState.Idle
                        )
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return permissionChecker.hasPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun handleLocationRationaleAccepted() {
        newState {
            copy(
                showLocationRationale = false,
                showLocationPermissionDialog = true
            )
        }
    }

    private fun handleLocationRationaleDismissed() {
        newState {
            copy(
                showLocationRationale = false,
                showLocationPermissionDialog = false
            )
        }
    }

    private fun handleLocationPermissionGranted() {
        if (loadAddressAfterPermission) {
            loadAddressAfterPermission = false
            loadAddress()
        }
    }

    private fun handleLocationPermissionDialogShown() {
        newState {
            copy(showLocationPermissionDialog = false)
        }
    }

    private fun handleCalendarRationaleAccepted() {
        newState {
            copy(
                showCalendarRationale = false,
                showCalendarPermissionDialog = true
            )
        }
    }

    private fun handleCalendarRationaleDismissed() {
        newState {
            copy(
                showCalendarRationale = false,
                showCalendarPermissionDialog = false
            )
        }
        // Continue saving without calendar integration
        performSave()
    }

    private fun handleCalendarPermissionGranted() {
        performSave()
    }

    private fun handleCalendarPermissionDialogShown() {
        newState {
            copy(showCalendarPermissionDialog = false)
        }
    }

    private fun visitSubjectFocusChanged(
        hasFocus: Boolean,
        visit: VisitState
    ) {
        newState {
            val showClearSubject = hasFocus && visit.subject.isNotEmpty()
            val hasNextConversationSuggestion = visit.nextConversationSuggestion != null
            val showNextVisitSuggestion = hasNextConversationSuggestion && !showClearSubject
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        showClearSubject = showClearSubject,
                        showNextVisitSuggestion = showNextVisitSuggestion
                    )
                )
            }
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun notesFocusChanged(hasFocus: Boolean) {
        newState {
            val showClearNotes = hasFocus && householder.notes?.isNotEmpty() == true
            copy(
                householder = householder.copy(
                    showClearNotes = showClearNotes,
                    isNotesExpanded = hasFocus
                ),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun addressFocusChanged(hasFocus: Boolean) {
        newState {
            val addressState = resolveAddressState(
                address = householder.address,
                hasFocus = hasFocus
            )
            copy(
                householder = householder.copy(addressState = addressState),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun nameFocusChanged(hasFocus: Boolean) {
        newState {
            val showClearName = hasFocus && householder.name.isNotEmpty()
            val showCopyData = !hasFocus
            copy(
                householder = householder.copy(
                    showClearName = showClearName,
                    showCopyData = showCopyData
                ),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun clearNameClicked() {
        nameChanged("")
    }

    private fun clearAddressClicked() {
        addressChanged("")
        newState {
            copy(
                householder = householder.copy(
                    editable = householder.editable.copy(
                        addressLatitude = null,
                        addressLongitude = null
                    )
                )
            )
        }
    }

    private fun clearNotesClicked() {
        notesChanged("")
    }

    private fun expandNotesClicked() {
        newState {
            copy(householder = householder.copy(isNotesExpanded = !householder.isNotesExpanded))
        }
    }

    private fun clearSubjectClicked(visit: VisitState) {
        visitSubjectChanged(value = "", visit = visit, caretPosition = 0)
    }

    private fun nextVisitSuggestionDismissed() {
        newState {
            copy(
                eventState = UiEventState.Idle
            )
        }
    }

    private fun nextVisitSuggestionAccepted(visit: VisitState) {
        newState {
            val newVisitType = visitList.determineNextVisitType().asState
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        editable = visit.editable.copy(isDone = true),
                        nextConversationSuggestion = null,
                        showNextVisitSuggestion = false
                    )
                )
            }
            val nextSubject = visit.nextConversationSuggestion?.questionAndResponse ?: ""
            val nextConversationId = visit.nextConversationSuggestion?.groupIdOrId
            val nextOrderIndex = visit.nextConversationSuggestion?.orderIndex
            val nextConversationSuggestion = conversationList.findNextConversation(
                nextConversationId,
                nextOrderIndex
            )
            val showClearSubject = visit.showClearSubject
            val hasNextConversationSuggestion = nextConversationSuggestion != null
            val showNextVisitSuggestion = hasNextConversationSuggestion && !showClearSubject
            val newVisitOrderIndex = updatedList.nextOrderIndex()
            val baseVisit = newVisit(newVisitOrderIndex)
            val newVisit = baseVisit.copy(
                editable = baseVisit.editable.copy(
                    visitType = newVisitType,
                    subject = nextSubject
                ),
                nextConversationSuggestion = nextConversationSuggestion,
                showNextVisitSuggestion = showNextVisitSuggestion
            )
            val newList = listOf(newVisit).plus(updatedList)
            copy(
                visitList = newList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun nextVisitSuggestionClicked(visit: VisitState) {
        newState {
            copy(
                eventState = UiEventState.NextVisitSuggestionShowing(visit)
            )
        }
    }

    private fun visitTypeClicked(visit: VisitState) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(isVisitTypeListExpanded = !visit.isVisitTypeListExpanded)
                )
            }
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun visitTypeSelected(
        visit: VisitState,
        visitType: VisitTypeState
    ) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        editable = visit.editable.copy(visitType = visitType),
                        isVisitTypeListExpanded = false
                    )
                )
            }
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun addVisitClicked() {
        newState {
            val nextVisitDate = visitList.determineNextVisitDate()
            val nextVisitType = visitList.determineNextVisitType().asState
            val nextOrderIndex = visitList.nextOrderIndex()
            val baseVisit = newVisit(nextOrderIndex)
            val nextVisit = baseVisit.copy(
                editable = baseVisit.editable.copy(
                    visitType = nextVisitType,
                    date = nextVisitDate
                )
            )
            val updatedVisitList = listOf(nextVisit)
                .plus(visitList)
                .revalidatePendingVisits(householder)
            copy(
                visitList = updatedVisitList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun visitDateDismissed() {
        newState {
            copy(
                eventState = UiEventState.Idle
            )
        }
    }

    private fun visitDateClicked(visit: VisitState) {
        newState {
            copy(
                eventState = UiEventState.VisitDateExpanded(visit)
            )
        }
    }

    private fun visitSubjectChanged(value: String, visit: VisitState, caretPosition: Int) {
        newState {
            val lines = value.split('\n')
            val lineIndex = lines.getLineIndex(caretPosition)
            val lineValue = lines.elementAtOrNull(lineIndex) ?: return@newState this
            val filteredConversationList = conversationList.filterBy(lineValue)
            val isConversionListExpanded = filteredConversationList.any { conversation ->
                conversation.show
            }
            val showClearSubject = value.isNotEmpty()
            val hasNextConversationSuggestion = visit.nextConversationSuggestion != null
            val showNextVisitSuggestion = hasNextConversationSuggestion && !showClearSubject
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        editable = visit.editable.copy(subject = value),
                        isConversationListExpanded = isConversionListExpanded,
                        showClearSubject = showClearSubject,
                        showNextVisitSuggestion = showNextVisitSuggestion,
                        caretPosition = caretPosition
                    )
                )
            }
            copy(
                visitList = updatedList,
                conversationList = filteredConversationList,
                eventState = UiEventState.Idle,
            )
        }
    }

    private fun visitDoneChanged(value: Boolean, visit: VisitState) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        editable = visit.editable.copy(isDone = value)
                    )
                )
            }.revalidatePendingVisits(householder)
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun visitDateAccepted(visit: VisitState, dateTime: LocalDateTime) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(editable = visit.editable.copy(date = dateTime))
                )
            }.revalidatePendingVisits(householder)
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun cancelClicked() {
        dismiss()
    }

    private fun dismiss() {
        newState {
            copy(
                eventState = UiEventState.Dismissed
            )
        }
    }

    private fun deleteClicked() {
        newState {
            copy(
                eventState = UiEventState.DeleteConfirmation
            )
        }
    }

    private fun deleteDismissed() {
        newState {
            copy(
                eventState = UiEventState.Idle
            )
        }
    }

    private fun removeVisitClicked(visit: VisitState) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(
                    index = this@apply.indexOfById(visit),
                    element = visit.copy(wasRemoved = true)
                )
            }
            copy(visitList = updatedList)
        }
    }

    private fun conversationSelected(
        visit: VisitState,
        conversation: ConversationState,
        caretPosition: Int
    ) {
        newState {
            val updatedList = visitList.toMutableList()
            val selectedConversation = conversation.questionAndResponse
            val nextConversationSuggestion = conversationList.findNextConversation(
                conversation.groupIdOrId,
                conversation.orderIndex
            )
            val showClearSubject = visit.showClearSubject
            val hasNextConversationSuggestion = nextConversationSuggestion != null
            val showNextVisitSuggestion = hasNextConversationSuggestion && !showClearSubject
            val subjectLines = visit.subject.split('\n') {
                this[getLineIndex(caretPosition)] = selectedConversation
            }
            val visitSubject = subjectLines.joinToString("\n")
            val updatedCaretPosition = let {
                val lineIndex = subjectLines.getLineIndex(caretPosition)
                subjectLines.subListInclusive(0, lineIndex).joinToString("").length + lineIndex
            }
            val visitIndex = updatedList.indexOfById(visit)
            updatedList[visitIndex] = visit.copy(
                editable = visit.editable.copy(subject = visitSubject),
                isConversationListExpanded = false,
                nextConversationSuggestion = nextConversationSuggestion,
                showNextVisitSuggestion = showNextVisitSuggestion,
                caretPosition = updatedCaretPosition
            )
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle,
            )
        }
    }

    private fun conversationListDismissed(visit: VisitState) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(this@apply.indexOfById(visit), visit.copy(isConversationListExpanded = false))
            }
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun visitTypeListDismissed(visit: VisitState) {
        newState {
            val updatedList = visitList.toMutableList().apply {
                set(this@apply.indexOfById(visit), visit.copy(isVisitTypeListExpanded = false))
            }
            copy(
                visitList = updatedList,
                eventState = UiEventState.Idle
            )
        }
    }

    private fun deleteAccepted() {
        newState {
            copy(
                eventState = UiEventState.Deleting
            )
        }
        viewModelScope.launch(dispatchers.io) {
            // Delete all calendar events for this householder's visits
            _uiState.value.visitList.forEach { visit ->
                visit.calendarEventId?.let { eventId ->
                    calendarEventManager.deleteEvent(eventId)
                }
            }

            val id = uiState.value.householder.id
            householderRepository.deleteById(id)
            newState {
                copy(
                    eventState = UiEventState.Deleted
                )
            }
        }
    }

    private fun undoChangesClicked() {
        newState {
            copy(eventState = UiEventState.UndoChangesConfirmation)
        }
    }

    private fun undoChangesConfirmationDismissed() {
        newState {
            copy(eventState = UiEventState.Idle)
        }
    }

    private fun undoChangesConfirmed() {
        val householderId = _uiState.value.householder.id
        val householderIsDraft = _uiState.value.householder.isDraft

        newState {
            copy(eventState = UiEventState.Idle)
        }

        viewModelScope.launch(dispatchers.io) {
            val householderSnapshot = snapshotRepository.getHouseholderSnapshot(householderId)

            if (householderIsDraft && householderSnapshot == null) {
                // Never committed -> nothing to restore to. Delete the draft (cascades visits +
                // snapshots) and reset to a fresh empty form (Reset, do not dismiss).
                householderRepository.deleteById(householderId)
                reinitializeEmptyForm()
                return@launch
            }

            // Committed record: restore from snapshots.
            if (householderSnapshot != null) {
                householderRepository.save(householderSnapshot.householder)
            }

            val visitSnapshots = snapshotRepository.getVisitSnapshots(householderId)
            val committedVisitIds = visitSnapshots.map { snapshot -> snapshot.visit.id }.toSet()
            // Delete ONLY visits added this session: draft in the DB and with no snapshot.
            // Untouched committed visits (not draft, no snapshot) must be preserved.
            val liveVisits = visitRepository.getAll(householderId)
            val addedVisitIds = liveVisits
                .filter { visit -> visit.isDraft && visit.id !in committedVisitIds }
                .map { visit -> visit.id }
            visitRepository.deleteBulk(addedVisitIds)
            visitSnapshots.forEach { visitRepository.save(it.visit) }

            snapshotRepository.deleteHouseholderSnapshot(householderId)
            snapshotRepository.deleteVisitSnapshots(householderId)

            rebuildUiFromDb(householderId)
        }
    }

    private fun reinitializeEmptyForm() {
        newState {
            copy(
                householder = newHouseholder(),
                visitList = listOf(newVisit(0)),
                eventState = UiEventState.Idle
            )
        }
        initialEditableData = _uiState.value.getEditableDataSnapshot()
    }

    private suspend fun rebuildUiFromDb(householderId: UUID) {
        val (householder, visitList) = loadHouseholderState(
            householderId,
            _uiState.value.conversationList
        )
        newState {
            copy(
                householder = householder,
                visitList = visitList,
                eventState = UiEventState.Idle
            )
        }
        initialEditableData = _uiState.value.getEditableDataSnapshot()
    }

    private suspend fun loadHouseholderState(
        householderId: UUID,
        conversationList: List<ConversationState>
    ): Pair<HouseholderState, List<VisitState>> {
        val householder = householderRepository.getById(householderId).asState
        val visitList = visitRepository.getAll(householderId)
            .map { visit ->
                val conversation = conversationList.firstOrNull { conversation ->
                    conversation.id == visit.nextConversationId
                }
                visit.asState(conversation)
            }
            .reindexIfNeeded()
            .revalidatePendingVisits(householder)
        return householder to visitList
    }

    private fun saveClicked() {
        // Check if there are pending visits that need calendar integration
        val hasPendingVisits = _uiState.value.visitList.any { !it.isDone && !it.wasRemoved }

        // If there are pending visits and no calendar permission, show rationale
        if (hasPendingVisits && !calendarEventManager.hasCalendarPermission()) {
            newState {
                copy(showCalendarRationale = true)
            }
            return
        }

        performSave()
    }

    private fun startAutoSave() {
        if (autoSaveJob != null) return
        autoSaveJob = _uiState
            .debounce(250)
            .onEach { state ->
                val baseline = initialEditableData ?: return@onEach

                if (state.getEditableDataSnapshot() == baseline) {
                    return@onEach
                }

                captureSnapshotsAndMarkDrafts(baseline)

                val updatedState = _uiState.value
                saveDraftSilently(updatedState)
                // Rebase from the marked state so the draft flags we just set
                // don't count as a fresh change on the next emission.
                initialEditableData = updatedState.getEditableDataSnapshot()
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    /**
     * For each entity dirtied relative to [baseline], snapshots its committed DB row (once) and
     * marks it a draft. The committed row is read via `getByIdOrNull` so this is robust to a prior
     * manual save. A brand-new record has no committed row: it is still marked draft (so discard can
     * detect "never committed") but is not snapshotted.
     */
    private suspend fun captureSnapshotsAndMarkDrafts(baseline: EditableDataSnapshot) {
        val state = _uiState.value

        // Householder
        val householder = state.householder
        if (!householder.isDraft) {
            val committed = householderRepository.getByIdOrNull(householder.id)
            val householderChanged = householder.editable != baseline.householder
            when {
                committed == null -> markHouseholderDraft()          // new record, nothing to snapshot
                householderChanged -> {                              // committed householder first dirtied
                    if (!committed.isDraft) {
                        snapshotRepository.saveHouseholderSnapshot(HouseholderSnapshot(committed))
                    }
                    markHouseholderDraft()
                }
                // committed && unchanged -> leave isDraft = false
            }
        }

        // Visits
        state.visitList.forEach { visit ->
            if (visit.isDraft || visit.wasRemoved) return@forEach
            val baselineEditable = baseline.visits[visit.id]
            val isNewOrChanged = baselineEditable == null || baselineEditable != visit.editable
            if (!isNewOrChanged) return@forEach

            val committed = visitRepository.getByIdOrNull(visit.id)
            if (committed != null && !committed.isDraft) {
                snapshotRepository.saveVisitSnapshot(VisitSnapshot(committed))
            }
            markVisitDraft(visit.id)
        }
    }

    private fun markHouseholderDraft() {
        newState { copy(householder = householder.copy(isDraft = true)) }
    }

    private fun markVisitDraft(visitId: UUID) {
        newState {
            val updatedList = visitList.map { visit ->
                if (visit.id == visitId) visit.copy(isDraft = true) else visit
            }
            copy(visitList = updatedList)
        }
    }

    private suspend fun saveDraftSilently(state: UiState) {
        val householderModel = state.householder.asModel
        householderRepository.save(householderModel)
        state.visitList
            .filter { !it.wasRemoved }
            .forEach { visitState ->
                visitRepository.save(visitState.asModel(householderModel.id))
            }
    }

    private fun performSave() {
        newState {
            copy(
                eventState = UiEventState.Saving
            )
        }
        viewModelScope.launch(dispatchers.io) {
            val householderModel = _uiState.value.householder.finalized().asModel
            addOrUpdateHouseholder(householderModel)
            val householderId = householderModel.id
            val houseHolder = householderModel.asState
            deleteRemovedVisits(_uiState.value.visitList)
            val visitList = addOrUpdateVisits(
                householderId = householderId,
                householderName = householderModel.name,
                visitList = _uiState.value.visitList.map { it.finalized() }
            )

            snapshotRepository.deleteHouseholderSnapshot(householderId)
            snapshotRepository.deleteVisitSnapshots(householderId)

            newState {
                copy(
                    householder = houseHolder,
                    visitList = visitList,
                    eventState = UiEventState.SaveSucceeded
                )
            }
            // Rebase the autosave baseline to the committed state so a debounced emission landing
            // after this save does not re-dirty (and re-snapshot) the just-committed record.
            initialEditableData = _uiState.value.getEditableDataSnapshot()
        }
    }

    private suspend fun deleteRemovedVisits(visitList: List<VisitState>) {
        val removedVisits = visitList.filter { it.wasRemoved }

        // Delete calendar events for removed visits
        removedVisits.forEach { visit ->
            visit.calendarEventId?.let { eventId ->
                calendarEventManager.deleteEvent(eventId)
            }
        }

        val removedVisitsIds = removedVisits.map { it.id }
        visitRepository.deleteBulk(removedVisitsIds)
        newState {
            val updatedList = visitList.toMutableList().apply {
                removeAll(removedVisits)
            }
            copy(visitList = updatedList)
        }
    }

    private suspend fun addOrUpdateVisits(
        householderId: UUID,
        householderName: String,
        visitList: List<VisitState>
    ): List<VisitState> {
        return visitList.map { visitState ->
            val calendarEventId = syncVisitCalendarEvent(
                calendarEventId = visitState.calendarEventId,
                visitType = visitState.visitType.type,
                subject = visitState.subject,
                date = visitState.date,
                isDone = visitState.isDone,
                householderName = householderName
            )
            val updatedVisitState = visitState.copy(calendarEventId = calendarEventId)
            val visitModel = updatedVisitState.asModel(householderId)
            visitRepository.save(visitModel)
            visitModel.asState(visitState.nextConversationSuggestion)
        }
    }

    private suspend fun addOrUpdateHouseholder(householder: Householder) {
        householderRepository.save(householder)
    }

    private fun nameChanged(value: String) {
        newState {
            val showClearName = value.isNotEmpty()
            copy(
                householder = householder.copy(
                    editable = householder.editable.copy(name = value),
                    showClearName = showClearName
                ),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun addressChanged(value: String) {
        newState {
            val addressState = resolveAddressState(address = value, hasFocus = true)
            copy(
                householder = householder.copy(
                    editable = householder.editable.copy(address = value),
                    addressState = addressState
                ),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun notesChanged(value: String) {
        newState {
            val showClearNotes = value.isNotEmpty()
            copy(
                householder = householder.copy(
                    editable = householder.editable.copy(notes = value),
                    showClearNotes = showClearNotes
                ),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun preferredDayChanged(value: VisitPreferredDay) {
        newState {
            val updatedHouseholder =
                householder.copy(editable = householder.editable.copy(preferredDay = value))
            copy(
                householder = updatedHouseholder,
                visitList = visitList.revalidatePendingVisits(updatedHouseholder),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun preferredTimeChanged(value: VisitPreferredTime) {
        newState {
            val updatedHouseholder =
                householder.copy(editable = householder.editable.copy(preferredTime = value))
            copy(
                householder = updatedHouseholder,
                visitList = visitList.revalidatePendingVisits(updatedHouseholder),
                eventState = UiEventState.Idle
            )
        }
    }

    private fun List<VisitState>.revalidatePendingVisits(householder: HouseholderState): List<VisitState> {
        return map { visit ->
            if (visit.wasRemoved || visit.isDone) {
                return@map visit.copy(hasVisitTimeError = false)
            }

            val isValid = visitTimeValidator.isValidVisitTime(
                visit.date,
                householder.preferredDay,
                householder.preferredTime
            )
            visit.copy(hasVisitTimeError = !isValid)
        }
    }

    private fun List<ConversationState>.findNextConversation(
        groupIdOrId: UUID?,
        orderIndex: Int?
    ): ConversationState? {
        val nextOrderIndex = orderIndex?.plus(1)
        return firstOrNull {
            it.groupIdOrId == groupIdOrId && it.orderIndex == nextOrderIndex
        }
    }

    private fun newHouseholder(): HouseholderState {
        return HouseholderState(
            id = idProvider.generateId(),
            editable = EditableHouseholderData(
                name = "",
                address = "",
                notes = null
            ),
            showClearName = false,
            isNotesExpanded = false,
            addressState = HouseholderAddressState.LoadLocation,
            showClearNotes = false,
            showCopyData = true,
            isLoadingAddress = false
        )
    }

    private fun newVisit(orderIndex: Int): VisitState {
        return VisitState(
            id = idProvider.generateId(),
            editable = EditableVisitData(
                subject = "",
                date = dateTimeProvider.nowLocalDateTime(),
                isDone = false,
                orderIndex = orderIndex,
                visitType = VisitType.FIRST_VISIT.asState
            ),
            householderId = null,
            canBeRemoved = isAllowedRemoveVisit(orderIndex),
            isConversationListExpanded = false,
            isVisitTypeListExpanded = false,
            nextConversationSuggestion = null,
            showNextVisitSuggestion = false,
            showClearSubject = false,
            wasRemoved = false,
            caretPosition = 0,
            isDraft = false,
        )
    }

    private fun viewCreated(householderId: UUID?) {
        this.isUpdatingVisit = householderId != null
        viewModelScope.launch(dispatchers.io) {
            conversations = conversationRepository.listAll()
            val conversationList = conversations.map { conversation ->
                conversation.asState
            }

            if (didCreateViewAlready) {
                newState { copy(conversationList = conversationList) }
                return@launch
            }

            didCreateViewAlready = true

            val visitTypeList = listOf(
                VisitType.BIBLE_STUDY.asState,
                VisitType.RETURN_VISIT.asState,
                VisitType.FIRST_VISIT.asState
            )

            if (householderId == null) {
                newState {
                    copy(
                        visitList = listOf(newVisit(0)),
                        conversationList = conversationList,
                        visitTypeList = visitTypeList,
                        eventState = UiEventState.Idle,
                        showDeleteButton = isUpdatingVisit
                    )
                }
                initialEditableData = _uiState.value.getEditableDataSnapshot()
                startAutoSave()
                return@launch
            }

            val (householder, visitList) = loadHouseholderState(householderId, conversationList)
            newState {
                copy(
                    householder = householder,
                    visitList = visitList,
                    conversationList = conversationList,
                    visitTypeList = visitTypeList,
                    eventState = UiEventState.Idle,
                    showDeleteButton = isUpdatingVisit
                )
            }
            initialEditableData = _uiState.value.getEditableDataSnapshot()
            startAutoSave()
        }
    }

    private fun isAllowedRemoveVisit(orderIndex: Int): Boolean {
        return orderIndex > 0
    }

    private fun List<String>.getLineIndex(caretPosition: Int): Int {
        var position = 0
        this.forEachIndexed { index, line ->
            position += line.length + 1 // +1 for the newline character
            if (position >= caretPosition) {
                return index
            }
        }
        return 0
    }

    private fun resolveAddressState(
        address: String,
        hasFocus: Boolean
    ): HouseholderAddressState {
        return when {
            address.isEmpty() -> HouseholderAddressState.LoadLocation
            latLongParser.parse(address).isSuccess -> HouseholderAddressState.LookUpAddressFromLatLong
            hasFocus -> HouseholderAddressState.ShowClearAddress
            else -> HouseholderAddressState.None
        }
    }

    private fun List<ConversationState>.filterBy(filter: String): List<ConversationState> {
        return map { state ->
            val matchesSearch = state.question.containsAllWords(filter)
            state.copy(show = matchesSearch && filter.length > 3)
        }
    }

    private fun Visit.asState(nextConversation: ConversationState?): VisitState {
        return VisitState(
            id = id,
            editable = EditableVisitData(
                subject = subject,
                date = date,
                isDone = isDone,
                orderIndex = orderIndex,
                visitType = visitType.asState
            ),
            householderId = householderId,
            canBeRemoved = isAllowedRemoveVisit(orderIndex),
            isConversationListExpanded = false,
            isVisitTypeListExpanded = false,
            nextConversationSuggestion = nextConversation,
            showNextVisitSuggestion = nextConversation != null,
            showClearSubject = false,
            wasRemoved = false,
            caretPosition = subject.length,
            calendarEventId = calendarEventId,
            isDraft = isDraft,
        )
    }

    private val HouseholderState.asModel: Householder
        get() {
            return Householder(
                id = id,
                name = name,
                address = address,
                notes = notes,
                addressLatitude = addressLatitude,
                addressLongitude = addressLongitude,
                preferredDay = preferredDay,
                preferredTime = preferredTime,
                isDraft = isDraft
            )
        }

    private val Householder.asState: HouseholderState
        get() {
            val addressState = resolveAddressState(
                address = address,
                hasFocus = isAddressFieldFocused
            )
            return HouseholderState(
                id = id,
                editable = EditableHouseholderData(
                    name = name,
                    address = address,
                    notes = notes,
                    addressLatitude = addressLatitude,
                    addressLongitude = addressLongitude,
                    preferredDay = preferredDay,
                    preferredTime = preferredTime
                ),
                showClearName = false,
                showCopyData = true,
                addressState = addressState,
                showClearNotes = false,
                isNotesExpanded = false, // Notes collapsed by default; user can expand if needed
                isLoadingAddress = false,
                isDraft = isDraft
            )
        }

    private val VisitType.asState: VisitTypeState
        get() {
            val descriptionTextResId = when (this) {
                VisitType.FIRST_VISIT -> R.string.first_visit
                VisitType.RETURN_VISIT -> R.string.return_visit
                VisitType.BIBLE_STUDY -> R.string.bible_study
            }
            return VisitTypeState(
                type = this, description = StringResource(textResId = descriptionTextResId)
            )
        }

    private val Conversation.asState: ConversationState
        get() {
            val questionAndResponse = if (response.isNotEmpty()) {
                question.plus("\n").plus(response)
            } else {
                question
            }
            return ConversationState(
                id = id,
                show = false,
                question = question,
                questionAndResponse = questionAndResponse,
                conversationGroupId = conversationGroupId,
                orderIndex = orderIndex
            )
        }

    private fun HouseholderState.finalized() = copy(isDraft = false)

    private fun VisitState.finalized() = copy(isDraft = false)

    private fun VisitState.asModel(householderId: UUID): Visit {
        return Visit(
            id = id,
            subject = subject,
            date = date,
            isDone = isDone,
            isDraft = isDraft,
            householderId = householderId,
            orderIndex = orderIndex,
            visitType = visitType.type,
            nextConversationId = nextConversationSuggestion?.id,
            calendarEventId = calendarEventId
        )
    }

    private fun List<VisitState>.determineNextVisitType(): VisitType {
        val lastItem = firstOrNull()?.visitType?.type
        return if (lastItem == null) {
            VisitType.FIRST_VISIT
        } else when (lastItem) {
            VisitType.FIRST_VISIT,
            VisitType.RETURN_VISIT -> VisitType.RETURN_VISIT

            VisitType.BIBLE_STUDY -> VisitType.BIBLE_STUDY
        }
    }

    private fun List<VisitState>.determineNextVisitDate(): LocalDateTime {
        val lastItem = firstOrNull()
        return if (lastItem == null) {
            dateTimeProvider.nowLocalDateTime().plusDays(DEFAULT_VISIT_INTERVAL_DAYS)
        } else {
            lastItem.date.plusDays(DEFAULT_VISIT_INTERVAL_DAYS)
        }
    }

    private fun List<VisitState>.nextOrderIndex(): Int {
        val maxOrderIndex = maxOfOrNull { it.orderIndex } ?: -1
        return maxOrderIndex + 1
    }

    /**
     * Normalizes the [VisitState.orderIndex] of all visits when duplicates are detected.
     *
     * This is a defensive step used after operations that can corrupt or duplicate
     * `orderIndex` values.
     *
     * If duplicates are found, visits are:
     * 1. Sorted by [VisitState.date] in **descending** order (newest first).
     * 2. Reindexed sequentially starting from 0 based on that sorted order.
     */
    private fun List<VisitState>.reindexIfNeeded(): List<VisitState> {
        val orderIndices = map { it.orderIndex }
        val hasDuplicates = orderIndices.size != orderIndices.toSet().size
        if (!hasDuplicates) return this
        // Sort by date descending (newest first) so the most recent visits stay at the top
        // and internal indices match the visible ordering in the UI.
        return sortedByDescending { visit -> visit.date }
            .mapIndexed { index, visit ->
                visit.copy(editable = visit.editable.copy(orderIndex = index))
            }
    }

    private fun UiState.getEditableDataSnapshot(): EditableDataSnapshot {
        return EditableDataSnapshot(
            householder = householder.editable,
            visits = visitList.associate { visit -> visit.id to visit.editable }
        )
    }

    private fun newState(block: UiState.() -> UiState) {
        _uiState.update(block)
    }

    private fun List<VisitState>.indexOfById(visit: VisitState): Int {
        return indexOfFirst { visit.id == it.id }
    }

    data class VisitState(
        val id: UUID,
        val editable: EditableVisitData,
        val householderId: UUID?,
        val canBeRemoved: Boolean,
        val isConversationListExpanded: Boolean,
        val isVisitTypeListExpanded: Boolean,
        val nextConversationSuggestion: ConversationState?,
        val showNextVisitSuggestion: Boolean,
        val showClearSubject: Boolean,
        val wasRemoved: Boolean,
        val caretPosition: Int,
        val calendarEventId: Long? = null,
        val hasVisitTimeError: Boolean = false,
        val isDraft: Boolean,
    ) {
        val subject get() = editable.subject
        val date get() = editable.date
        val isDone get() = editable.isDone
        val orderIndex get() = editable.orderIndex
        val visitType get() = editable.visitType
    }

    data class ConversationState(
        val id: UUID?,
        val question: String,
        val questionAndResponse: String,
        val show: Boolean,
        val conversationGroupId: UUID?,
        val orderIndex: Int
    ) {
        val groupIdOrId: UUID?
            get() {
                return conversationGroupId ?: id
            }
    }

    sealed class HouseholderAddressState {
        data object None : HouseholderAddressState()
        data object ShowClearAddress : HouseholderAddressState()
        data object LoadLocation : HouseholderAddressState()
        data object LookUpAddressFromLatLong : HouseholderAddressState()
    }

    data class HouseholderState(
        var id: UUID,
        val editable: EditableHouseholderData,
        val showClearName: Boolean,
        val showCopyData: Boolean,
        val addressState: HouseholderAddressState,
        val showClearNotes: Boolean,
        val isLoadingAddress: Boolean,
        val isNotesExpanded: Boolean,
        val isDraft: Boolean = false
    ) {
        val name get() = editable.name
        val address get() = editable.address
        val notes get() = editable.notes
        val addressLatitude get() = editable.addressLatitude
        val addressLongitude get() = editable.addressLongitude
        val preferredDay get() = editable.preferredDay
        val preferredTime get() = editable.preferredTime
    }

    data class VisitTypeState(val type: VisitType, val description: StringResource)

    data class EditableHouseholderData(
        val name: String,
        val address: String,
        val notes: String?,
        val addressLatitude: Double? = null,
        val addressLongitude: Double? = null,
        val preferredDay: VisitPreferredDay = VisitPreferredDay.ANY,
        val preferredTime: VisitPreferredTime = VisitPreferredTime.ANY
    )

    data class EditableVisitData(
        val subject: String,
        val date: LocalDateTime,
        val isDone: Boolean,
        val orderIndex: Int,
        val visitType: VisitTypeState
    )

    private data class EditableDataSnapshot(
        val householder: EditableHouseholderData,
        val visits: Map<UUID, EditableVisitData>
    )

    sealed class UiEvent {
        data class ViewCreated(val householderId: UUID?) : UiEvent()
        data class HouseholderNameChanged(val value: String) : UiEvent()
        data class HouseholderAddressChanged(val value: String) : UiEvent()
        data class HouseholderNotesChanged(val value: String) : UiEvent()
        data class VisitSubjectChanged(
            val visit: VisitState,
            val value: String,
            val caretPosition: Int
        ) : UiEvent()

        data class VisitDoneChanged(val visit: VisitState, val value: Boolean) : UiEvent()
        data class VisitDateClicked(val visit: VisitState) : UiEvent()
        data class VisitDateAccepted(val visit: VisitState, val dateTime: LocalDateTime) : UiEvent()
        data class RemoveVisitClicked(val visit: VisitState) : UiEvent()
        data class ConversationSelected(
            val visit: VisitState,
            val conversation: ConversationState,
            val caretPosition: Int
        ) : UiEvent()

        data class VisitTypeSelected(
            val visit: VisitState,
            val visitType: VisitTypeState
        ) : UiEvent()

        data class VisitTypeClicked(val visit: VisitState) : UiEvent()
        data class NextVisitSuggestionClicked(val visit: VisitState) : UiEvent()
        data class ClearSubjectClicked(val visit: VisitState) : UiEvent()
        data class NameFocusChanged(val hasFocus: Boolean) : UiEvent()
        data class AddressFocusChanged(val hasFocus: Boolean) : UiEvent()
        data class NotesFocusChanged(val hasFocus: Boolean) : UiEvent()
        data class VisitSubjectFocusChanged(val hasFocus: Boolean, val visit: VisitState) :
            UiEvent()

        data class ConversationListDismissed(val visit: VisitState) : UiEvent()
        data class VisitTypeListDismissed(val visit: VisitState) : UiEvent()
        data class NextVisitSuggestionAccepted(val visit: VisitState) : UiEvent()
        data class PreferredDayChanged(val value: VisitPreferredDay) : UiEvent()
        data class PreferredTimeChanged(val value: VisitPreferredTime) : UiEvent()

        data object LoadAddressClicked : UiEvent()
        data object LookUpAddressFromLatLongClicked : UiEvent()
        data object NextVisitSuggestionDismissed : UiEvent()
        data object VisitDateDismissed : UiEvent()
        data object AddVisitClicked : UiEvent()
        data object DeleteClicked : UiEvent()
        data object DeleteAccepted : UiEvent()
        data object DeleteDismissed : UiEvent()
        data object CancelClicked : UiEvent()
        data object UndoChangesClicked : UiEvent()
        data object UndoChangesConfirmed : UiEvent()
        data object UndoChangesConfirmationDismissed : UiEvent()
        data object SaveClicked : UiEvent()
        data object ClearNameClicked : UiEvent()
        data object ClearAddressClicked : UiEvent()
        data object ClearNotesClicked : UiEvent()
        data object ExpandNotesClicked : UiEvent()
        data object SnackbarDismissed : UiEvent()
        data object LocationRationaleAccepted : UiEvent()
        data object LocationRationaleDismissed : UiEvent()
        data object LocationPermissionGranted : UiEvent()
        data object LocationPermissionDialogShown : UiEvent()
        data object CalendarRationaleAccepted : UiEvent()
        data object CalendarRationaleDismissed : UiEvent()
        data object CalendarPermissionGranted : UiEvent()
        data object CalendarPermissionDialogShown : UiEvent()
        data object CopyVisitDataClicked : UiEvent()
    }

    sealed class UiEventState {
        data object Idle : UiEventState()
        data object NoAddressFound : UiEventState()
        data object Dismissed : UiEventState()
        data object Saving : UiEventState()
        data object SaveSucceeded : UiEventState()
        data object ValidationError : UiEventState()
        data class VisitDateExpanded(val visit: VisitState) : UiEventState()
        data object DeleteConfirmation : UiEventState()
        data object UndoChangesConfirmation : UiEventState()
        data object Deleting : UiEventState()
        data object Deleted : UiEventState()
        data class NextVisitSuggestionShowing(val visit: VisitState) : UiEventState()
        data object CopiedToClipboard : UiEventState()
    }

    data class UiState(
        val householder: HouseholderState,
        val visitList: List<VisitState>,
        val conversationList: List<ConversationState>,
        val visitTypeList: List<VisitTypeState>,
        val eventState: UiEventState,
        val showDeleteButton: Boolean = false,
        val showLocationRationale: Boolean = false,
        val showLocationPermissionDialog: Boolean = false,
        val showCalendarRationale: Boolean = false,
        val showCalendarPermissionDialog: Boolean = false
    ) {
        val hasDrafts: Boolean
            get() = householder.isDraft || visitList.any { !it.wasRemoved && it.isDraft }
    }

    companion object {
        private const val DEFAULT_VISIT_INTERVAL_DAYS = 7L
    }
}
