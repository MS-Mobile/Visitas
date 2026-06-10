package com.msmobile.visitas.settings

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.AppVersionProvider
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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

    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null
    ): SettingsDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val backupHandler = mock<BackupHandler>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                visitMapEngineOption = savedMapEngine
            )
        }
        preferenceRepositoryRef?.value = preferenceRepository
        return SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            backupHandler = backupHandler,
            dispatchers = dispatchers,
            appVersionProvider = AppVersionProvider
        )
    }
}
