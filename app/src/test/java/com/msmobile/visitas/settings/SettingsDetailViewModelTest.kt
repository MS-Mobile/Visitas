package com.msmobile.visitas.settings

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.AppVersionProvider
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
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

    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        savedAddVisitsToCalendar: Boolean = false,
        hasCalendarPermission: Boolean = true,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null
    ): SettingsDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val backupHandler = mock<BackupHandler>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                visitMapEngineOption = savedMapEngine,
                addVisitsToCalendar = savedAddVisitsToCalendar
            )
        }
        preferenceRepositoryRef?.value = preferenceRepository
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn hasCalendarPermission
        }
        return SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            calendarEventManager = calendarEventManager,
            backupHandler = backupHandler,
            dispatchers = dispatchers,
            appVersionProvider = AppVersionProvider
        )
    }
}
