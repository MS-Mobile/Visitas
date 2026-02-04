package com.msmobile.visitas.visit

import com.msmobile.visitas.conversation.Conversation
import com.msmobile.visitas.conversation.ConversationRepository
import com.msmobile.visitas.householder.Householder
import com.msmobile.visitas.householder.HouseholderRepository
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DateTimeProvider
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.LatLongParser
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.util.PermissionChecker
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import java.time.LocalDateTime
import java.util.UUID

class VisitDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.householder.name)
        assertEquals("", state.householder.address)
        assertEquals(1, state.visitList.size)
        assertTrue(state.conversationList.isEmpty())
        assertEquals(VisitDetailViewModel.UiEventState.Idle, state.eventState)
        assertFalse(state.showDeleteButton)
    }

    @Test
    fun `onEvent with ViewCreated without householderId loads conversation list`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = null))

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { listAll() }
        val state = viewModel.uiState.value
        assertEquals(3, state.conversationList.size)
        assertFalse(state.showDeleteButton)
    }

    @Test
    fun `onEvent with ViewCreated with householderId loads householder and visits`() {
        // Arrange
        val householderRepositoryRef = MockReferenceHolder<HouseholderRepository>()
        val visitRepositoryRef = MockReferenceHolder<VisitRepository>()
        val viewModel = createViewModel(
            householderRepositoryRef = householderRepositoryRef,
            visitRepositoryRef = visitRepositoryRef
        )

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))

        // Assert
        val householderRepository = requireNotNull(householderRepositoryRef.value)
        val visitRepository = requireNotNull(visitRepositoryRef.value)
        verifyBlocking(householderRepository) { getById(HOUSEHOLDER_ID) }
        verifyBlocking(visitRepository) { getAll(HOUSEHOLDER_ID) }
        val state = viewModel.uiState.value
        assertEquals("Test Name", state.householder.name)
        assertEquals("Test Address", state.householder.address)
        assertTrue(state.showDeleteButton)
    }

    @Test
    fun `onEvent with HouseholderNameChanged updates householder name`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("New Name"))

        // Assert
        assertEquals("New Name", viewModel.uiState.value.householder.name)
    }

    @Test
    fun `onEvent with HouseholderAddressChanged updates householder address`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderAddressChanged("New Address"))

        // Assert
        assertEquals("New Address", viewModel.uiState.value.householder.address)
    }

    @Test
    fun `onEvent with HouseholderNotesChanged updates householder notes`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNotesChanged("Some notes"))

        // Assert
        assertEquals("Some notes", viewModel.uiState.value.householder.notes)
    }

    @Test
    fun `onEvent with ClearNameClicked clears householder name`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("Test Name"))

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ClearNameClicked)

        // Assert
        assertEquals("", viewModel.uiState.value.householder.name)
    }

    @Test
    fun `onEvent with ClearAddressClicked clears householder address`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderAddressChanged("Test Address"))

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ClearAddressClicked)

        // Assert
        assertEquals("", viewModel.uiState.value.householder.address)
    }

    @Test
    fun `onEvent with ClearNotesClicked clears householder notes`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNotesChanged("Some notes"))

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ClearNotesClicked)

        // Assert
        assertEquals("", viewModel.uiState.value.householder.notes)
    }

    @Test
    fun `onEvent with AddVisitClicked adds a new visit`() {
        // Arrange
        val viewModel = createViewModel()
        val initialCount = viewModel.uiState.value.visitList.size

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.AddVisitClicked)

        // Assert
        assertEquals(initialCount + 1, viewModel.uiState.value.visitList.size)
    }

    @Test
    fun `onEvent with DeleteClicked shows delete confirmation`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.DeleteClicked)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.DeleteConfirmation,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with DeleteDismissed returns to Idle state`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.DeleteClicked)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.DeleteDismissed)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Idle,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with CancelClicked discards changes when no edits made`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.CancelClicked)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Canceled,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with DiscardChangesDismissed returns to Idle state`() {
        // Arrange
        val viewModel = createViewModel()
        // Make an edit to trigger discard confirmation
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = null))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("New Name"))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.CancelClicked)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.DiscardChangesDismissed)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Idle,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with DiscardChangesAccepted cancels and discards changes`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = null))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("New Name"))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.CancelClicked)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.DiscardChangesAccepted)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Canceled,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with LocationRationaleAccepted shows permission dialog`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.LocationRationaleAccepted)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationRationale)
        assertTrue(viewModel.uiState.value.showLocationPermissionDialog)
    }

    @Test
    fun `onEvent with LocationRationaleDismissed hides rationale and permission dialog`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.LocationRationaleDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationRationale)
        assertFalse(viewModel.uiState.value.showLocationPermissionDialog)
    }

    @Test
    fun `onEvent with LocationPermissionDialogShown hides permission dialog`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.LocationRationaleAccepted)
        assertTrue(viewModel.uiState.value.showLocationPermissionDialog)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.LocationPermissionDialogShown)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationPermissionDialog)
    }

    @Test
    fun `onEvent with VisitDateDismissed returns to Idle state`() {
        // Arrange
        val viewModel = createViewModel()
        val visit = viewModel.uiState.value.visitList.first()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.VisitDateClicked(visit))

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.VisitDateDismissed)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Idle,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `onEvent with SnackbarDismissed returns to Idle state`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.SnackbarDismissed)

        // Assert
        assertEquals(
            VisitDetailViewModel.UiEventState.Idle,
            viewModel.uiState.value.eventState
        )
    }

    private fun createViewModel(
        conversationRepositoryRef: MockReferenceHolder<ConversationRepository>? = null,
        householderRepositoryRef: MockReferenceHolder<HouseholderRepository>? = null,
        visitRepositoryRef: MockReferenceHolder<VisitRepository>? = null
    ): VisitDetailViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val conversationRepository = mock<ConversationRepository> {
            onBlocking { listAll() } doReturn createConversationList()
        }
        conversationRepositoryRef?.value = conversationRepository

        val householderRepository = mock<HouseholderRepository> {
            onBlocking { getById(any()) } doReturn createHouseholder()
        }
        householderRepositoryRef?.value = householderRepository

        val visitRepository = mock<VisitRepository> {
            onBlocking { getAll(any()) } doReturn createVisitList()
        }
        visitRepositoryRef?.value = visitRepository

        val addressProvider = mock<AddressProvider>()
        val idProvider = mock<IdProvider> {
            on { generateId() } doReturn NEW_UUID
        }
        val permissionChecker = mock<PermissionChecker> {
            on { hasPermissions(any(), any()) } doReturn false
        }
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn false
        }
        val visitTimeValidator = mock<VisitTimeValidator> {
            on { isValidVisitTime(any(), any(), any()) } doReturn true
        }
        val dateTimeProvider = mock<DateTimeProvider> {
            on { nowLocalDateTime() } doReturn TEST_DATE_TIME
        }
        val latLongParser = LatLongParser()

        return VisitDetailViewModel(
            dispatchers = dispatchers,
            householderRepository = householderRepository,
            visitRepository = visitRepository,
            conversationRepository = conversationRepository,
            addressProvider = addressProvider,
            idProvider = idProvider,
            permissionChecker = permissionChecker,
            calendarEventManager = calendarEventManager,
            visitTimeValidator = visitTimeValidator,
            dateTimeProvider = dateTimeProvider,
            latLongParser = latLongParser
        )
    }

    private fun createConversationList(): List<Conversation> {
        return listOf(
            Conversation(
                id = FIRST_CONVERSATION_ID,
                question = "Question 1",
                response = "Response 1",
                conversationGroupId = null,
                orderIndex = 0
            ),
            Conversation(
                id = SECOND_CONVERSATION_ID,
                question = "Question 2",
                response = "Response 2",
                conversationGroupId = FIRST_CONVERSATION_ID,
                orderIndex = 1
            ),
            Conversation(
                id = THIRD_CONVERSATION_ID,
                question = "Question 3",
                response = "Response 3",
                conversationGroupId = FIRST_CONVERSATION_ID,
                orderIndex = 2
            )
        )
    }

    private fun createHouseholder(): Householder {
        return Householder(
            id = HOUSEHOLDER_ID,
            name = "Test Name",
            address = "Test Address",
            notes = "Test Notes",
            addressLatitude = null,
            addressLongitude = null
        )
    }

    private fun createVisitList(): List<Visit> {
        return listOf(
            Visit(
                id = FIRST_VISIT_ID,
                subject = "Subject 1",
                date = TEST_DATE_TIME,
                isDone = false,
                householderId = HOUSEHOLDER_ID,
                orderIndex = 0,
                visitType = VisitType.FIRST_VISIT,
                nextConversationId = null,
                calendarEventId = null
            )
        )
    }

    companion object {
        private val HOUSEHOLDER_ID = UUID.fromString("3f2b7d9a-8c4e-4e2a-9b1d-5c6a7f8e1a23")
        private val FIRST_CONVERSATION_ID = UUID.fromString("c1a9f7b4-2e3d-4f5a-8b6c-0d1e2f3a4b5c")
        private val SECOND_CONVERSATION_ID = UUID.fromString("7a4e1c9b-6d2f-4a3e-8b5c-0f9d1e2a3c4b")
        private val THIRD_CONVERSATION_ID = UUID.fromString("5c4b3a2f-1e9d-7c6b-4a3e-8b5c0f9d1e2a")
        private val FIRST_VISIT_ID = UUID.fromString("9e8d7c6b-5a4f-3e2d-1c0b-a9e8d7c6b5a4")
        private val NEW_UUID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        private val TEST_DATE_TIME = LocalDateTime.of(2024, 1, 15, 10, 30)
    }
}
