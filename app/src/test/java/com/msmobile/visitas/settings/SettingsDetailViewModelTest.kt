package com.msmobile.visitas.settings

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.preference.preferredCalendar
import com.msmobile.visitas.preference.withPreferredCalendar
import com.msmobile.visitas.util.AppVersionProvider
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.CalendarIdentity
import com.msmobile.visitas.util.CalendarInfo
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.util.identity
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking

class SettingsDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEvent with ViewCreated loads map engine from saved preference`() {
        val viewModel = createViewModel(savedMapEngine = VisitMapEngineOption.Leaflet)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(VisitMapEngineOption.Leaflet, viewModel.uiState.value.selectedMapEngine)
    }

    @Test
    fun `onEvent with MapEngineSelected updates state to selected engine`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(VisitMapEngineOption.Leaflet))

        assertEquals(VisitMapEngineOption.Leaflet, viewModel.uiState.value.selectedMapEngine)
    }

    @Test
    fun `onEvent with MapEngineSelected saves preference to repository`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(preferenceRepositoryRef = preferenceRepositoryRef)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(VisitMapEngineOption.Leaflet))

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) { save(any()) }
    }

    @Test
    fun `onEvent with ViewCreated loads add visits to calendar from saved preference`() {
        val viewModel = createViewModel(savedAddVisitsToCalendar = true)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertTrue(viewModel.uiState.value.addVisitsToCalendar)
    }

    @Test
    fun `onEvent with AddVisitsToCalendarToggled enabled and permission granted enables setting`() {
        val viewModel = createViewModel(hasCalendarPermission = true)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        assertTrue(viewModel.uiState.value.addVisitsToCalendar)
        assertFalse(viewModel.uiState.value.showCalendarRationale)
    }

    @Test
    fun `onEvent with AddVisitsToCalendarToggled enabled and permission granted saves preference`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { addVisitsToCalendar })
        }
    }

    @Test
    fun `onEvent with AddVisitsToCalendarToggled enabled without permission shows rationale`() {
        val viewModel = createViewModel(hasCalendarPermission = false)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        assertTrue(viewModel.uiState.value.showCalendarRationale)
        assertFalse(viewModel.uiState.value.addVisitsToCalendar)
    }

    @Test
    fun `onEvent with AddVisitsToCalendarToggled enabled without permission does not save preference`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            hasCalendarPermission = false,
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value), never()) { save(any()) }
    }

    @Test
    fun `onEvent with AddVisitsToCalendarToggled disabled saves preference without permission check`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            savedAddVisitsToCalendar = true,
            hasCalendarPermission = false,
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = false))

        assertFalse(viewModel.uiState.value.addVisitsToCalendar)
        assertFalse(viewModel.uiState.value.showCalendarRationale)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { !addVisitsToCalendar })
        }
    }

    @Test
    fun `onEvent with CalendarRationaleAccepted requests the permission`() {
        val viewModel = createViewModel(hasCalendarPermission = false)
        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarRationaleAccepted)

        assertFalse(viewModel.uiState.value.showCalendarRationale)
        assertTrue(viewModel.uiState.value.showCalendarPermissionDialog)
    }

    @Test
    fun `onEvent with CalendarRationaleDismissed leaves the setting off`() {
        val viewModel = createViewModel(hasCalendarPermission = false)
        viewModel.onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled = true))

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarRationaleDismissed)

        assertFalse(viewModel.uiState.value.showCalendarRationale)
        assertFalse(viewModel.uiState.value.showCalendarPermissionDialog)
        assertFalse(viewModel.uiState.value.addVisitsToCalendar)
    }

    @Test
    fun `onEvent with CalendarPermissionGranted enables the setting`() {
        val viewModel = createViewModel(hasCalendarPermission = false)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionGranted)

        assertTrue(viewModel.uiState.value.addVisitsToCalendar)
        assertFalse(viewModel.uiState.value.showCalendarPermissionDialog)
    }

    @Test
    fun `onEvent with CalendarPermissionDenied leaves the setting off`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            hasCalendarPermission = false,
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionDenied)

        assertFalse(viewModel.uiState.value.addVisitsToCalendar)
        assertFalse(viewModel.uiState.value.showCalendarPermissionDialog)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value), never()) { save(any()) }
    }

    @Test
    fun `onEvent with CalendarPermissionGranted does not let the calendar load revert the setting`() {
        // Regression test for a race: enabling the setting and loading the calendar list each did an
        // independent read-modify-write of the same preference row, from two separate coroutines.
        // StandardTestDispatcher lets their get() calls interleave before either save() lands, the
        // way real Room I/O on Dispatchers.IO can; UnconfinedTestDispatcher (used by
        // mainDispatcherRule elsewhere in this file) runs everything eagerly to completion and
        // cannot reproduce it. So this test builds its own dispatcher and a small stateful fake
        // repository instead of going through createViewModel.
        val testDispatcher = StandardTestDispatcher()
        var stored = Preference(
            visitListDateFilterOption = VisitListDateFilterOption.All,
            visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
            addVisitsToCalendar = false
        ).withPreferredCalendar(MINISTRY_CALENDAR.identity) // stale: not in availableCalendars below
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doSuspendableAnswer {
                // Snapshot before suspending, like a real query racing a concurrent write.
                val snapshot = stored
                delay(1)
                snapshot
            }
            on { save(any()) } doSuspendableAnswer { invocation ->
                stored = invocation.getArgument(0)
            }
        }
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn true
            on { getAvailableCalendars() } doReturn listOf(PERSONAL_CALENDAR)
        }
        val viewModel = SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            calendarEventManager = calendarEventManager,
            backupHandler = mock(),
            dispatchers = DispatcherProvider(io = testDispatcher),
            appVersionProvider = AppVersionProvider
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionGranted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "addVisitsToCalendar was reverted by a losing, stale write from loadAvailableCalendars",
            stored.addVisitsToCalendar
        )
    }

    @Test
    fun `onEvent with ViewCreated loads the available calendars when permission is granted`() {
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(
            listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            viewModel.uiState.value.availableCalendars
        )
    }

    @Test
    fun `onEvent with ViewCreated does not query calendars without permission`() {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val viewModel = createViewModel(
            hasCalendarPermission = false,
            availableCalendars = listOf(PERSONAL_CALENDAR),
            calendarEventManagerRef = calendarEventManagerRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertTrue(viewModel.uiState.value.availableCalendars.isEmpty())
        verifyBlocking(requireNotNull(calendarEventManagerRef.value), never()) {
            getAvailableCalendars()
        }
    }

    @Test
    fun `onEvent with ViewCreated keeps a chosen calendar that is still available`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            savedPreferredCalendar = MINISTRY_CALENDAR.identity,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(MINISTRY_CALENDAR.identity, viewModel.uiState.value.preferredCalendar)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value), never()) { save(any()) }
    }

    @Test
    fun `onEvent with ViewCreated excludes calendars whose provider omits part of their identity`() {
        val unidentifiable = PERSONAL_CALENDAR.copy(id = 3L, accountName = null)
        val viewModel = createViewModel(
            availableCalendars = listOf(PERSONAL_CALENDAR, unidentifiable)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(listOf(PERSONAL_CALENDAR), viewModel.uiState.value.availableCalendars)
    }

    @Test
    fun `onEvent with CalendarPermissionGranted clears a chosen calendar that no longer exists`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            savedPreferredCalendar = MINISTRY_CALENDAR.identity,
            availableCalendars = listOf(PERSONAL_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionGranted)

        assertNull(viewModel.uiState.value.preferredCalendar)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { preferredCalendar == null })
        }
    }

    @Test
    fun `onEvent with ViewCreated clears a chosen calendar that no longer exists`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            savedPreferredCalendar = MINISTRY_CALENDAR.identity,
            availableCalendars = listOf(PERSONAL_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertNull(viewModel.uiState.value.preferredCalendar)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { preferredCalendar == null })
        }
    }

    @Test
    fun `onEvent with CalendarSelected updates state to the selected calendar`() {
        val viewModel = createViewModel(
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarSelected(MINISTRY_CALENDAR))

        assertEquals(MINISTRY_CALENDAR.identity, viewModel.uiState.value.preferredCalendar)
    }

    @Test
    fun `onEvent with CalendarSelected saves the preference`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarSelected(MINISTRY_CALENDAR))

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { preferredCalendar == MINISTRY_CALENDAR.identity })
        }
    }

    @Test
    fun `onEvent with CalendarPermissionGranted loads the available calendars`() {
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionGranted)

        assertEquals(
            listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            viewModel.uiState.value.availableCalendars
        )
    }

    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        savedAddVisitsToCalendar: Boolean = false,
        savedPreferredCalendar: CalendarIdentity? = null,
        availableCalendars: List<CalendarInfo> = emptyList(),
        hasCalendarPermission: Boolean = true,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null,
        calendarEventManagerRef: MockReferenceHolder<CalendarEventManager>? = null
    ): SettingsDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val backupHandler = mock<BackupHandler>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                visitMapEngineOption = savedMapEngine,
                addVisitsToCalendar = savedAddVisitsToCalendar
            ).withPreferredCalendar(savedPreferredCalendar)
        }
        preferenceRepositoryRef?.value = preferenceRepository
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn hasCalendarPermission
            on { getAvailableCalendars() } doReturn availableCalendars
        }
        calendarEventManagerRef?.value = calendarEventManager
        return SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            calendarEventManager = calendarEventManager,
            backupHandler = backupHandler,
            dispatchers = dispatchers,
            appVersionProvider = AppVersionProvider
        )
    }

    private companion object {
        val PERSONAL_CALENDAR = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            ownerAccount = "user@gmail.com",
            accountType = "com.google",
            isPrimary = true,
            isVisible = true
        )
        val MINISTRY_CALENDAR = CalendarInfo(
            id = 2L,
            displayName = "Ministry",
            accountName = "user@gmail.com",
            ownerAccount = "ministry123@group.calendar.google.com",
            accountType = "com.google",
            isPrimary = false,
            isVisible = true
        )
    }
}
