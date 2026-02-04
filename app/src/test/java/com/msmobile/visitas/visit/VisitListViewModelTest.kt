package com.msmobile.visitas.visit

import android.net.Uri
import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.routing.OsrmRoutingProvider
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.UserLocationProvider
import com.squareup.moshi.Moshi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import java.time.LocalDateTime
import java.util.UUID

class VisitListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.visitList.isEmpty())
        assertEquals("", state.filter.search)
        assertFalse(state.isVisitsFilterMenuExpanded)
        assertEquals(0, state.selectedTabIndex)
        assertFalse(state.showLocationRationale)
        assertFalse(state.showLocationPermissionDialog)
        assertFalse(state.isLoadingVisits)
        assertFalse(state.showNearbyVisits)
        assertFalse(state.showBackupSheet)
    }

    @Test
    fun `onEvent with ViewCreated loads visits from repository`() {
        // Arrange
        val visitHouseholderRepositoryRef = MockReferenceHolder<VisitHouseholderRepository>()
        val viewModel = createViewModel(visitHouseholderRepositoryRef = visitHouseholderRepositoryRef)

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        // Assert
        val visitHouseholderRepository = requireNotNull(visitHouseholderRepositoryRef.value)
        verifyBlocking(visitHouseholderRepository) { getAll() }
        val visits = viewModel.uiState.value.visitList
        assertEquals(2, visits.size)
    }

    @Test
    fun `onEvent with TabSelected updates selectedTabIndex`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.TabSelected(1))

        // Assert
        assertEquals(1, viewModel.uiState.value.selectedTabIndex)
    }

    @Test
    fun `onEvent with SearchChanged updates filter`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("test search"))

        // Assert
        assertEquals("test search", viewModel.uiState.value.filter.search)
    }

    @Test
    fun `onEvent with FilterCleared clears search filter`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("test search"))

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.FilterCleared)

        // Assert
        assertEquals("", viewModel.uiState.value.filter.search)
    }

    @Test
    fun `onEvent with VisitsFilterButtonClicked expands filter menu`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitsFilterButtonClicked)

        // Assert
        assertTrue(viewModel.uiState.value.isVisitsFilterMenuExpanded)
    }

    @Test
    fun `onEvent with VisitsFilterMenuDismissed collapses filter menu`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitsFilterButtonClicked)

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitsFilterMenuDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.isVisitsFilterMenuExpanded)
    }

    @Test
    fun `onEvent with VisitsFilterOptionSelected updates selected filter option`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitsFilterOptionSelected(VisitListDateFilterOption.Done))

        // Assert
        assertEquals(VisitListDateFilterOption.Done, viewModel.uiState.value.selectedVisitFilterOption)
    }

    @Test
    fun `onEvent with BackupButtonClicked shows backup sheet`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.BackupButtonClicked)

        // Assert
        assertTrue(viewModel.uiState.value.showBackupSheet)
    }

    @Test
    fun `onEvent with BackupSheetDismissed hides backup sheet`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitListViewModel.UiEvent.BackupButtonClicked)

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.BackupSheetDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.showBackupSheet)
    }

    @Test
    fun `onEvent with VisitMapSheetClicked shows visit map sheet`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitMapSheetClicked)

        // Assert
        assertTrue(viewModel.uiState.value.showVisitMapSheet)
    }

    @Test
    fun `onEvent with VisitMapSheetDismissed hides visit map sheet`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitMapSheetClicked)

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.VisitMapSheetDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.showVisitMapSheet)
    }

    @Test
    fun `onEvent with BackupFilePreviewed sets preview backup file state`() {
        // Arrange
        val viewModel = createViewModel()
        val mockUri = mock<Uri>()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.BackupFilePreviewed(mockUri))

        // Assert
        val state = viewModel.uiState.value.previewBackupFileState
        assertTrue(state is VisitListViewModel.PreviewBackupFileState.Previewing)
        assertEquals(mockUri, (state as VisitListViewModel.PreviewBackupFileState.Previewing).fileUri)
    }

    @Test
    fun `onEvent with RestorePreviewedBackupDialogDismissed resets preview backup file state`() {
        // Arrange
        val viewModel = createViewModel()
        val mockUri = mock<Uri>()
        viewModel.onEvent(VisitListViewModel.UiEvent.BackupFilePreviewed(mockUri))

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.RestorePreviewedBackupDialogDismissed)

        // Assert
        assertEquals(VisitListViewModel.PreviewBackupFileState.None, viewModel.uiState.value.previewBackupFileState)
    }

    @Test
    fun `onEvent with LocationRationaleAccepted shows permission dialog`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.LocationRationaleAccepted)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationRationale)
        assertTrue(viewModel.uiState.value.showLocationPermissionDialog)
    }

    @Test
    fun `onEvent with LocationRationaleDismissed hides rationale and permission dialog`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.LocationRationaleDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationRationale)
        assertFalse(viewModel.uiState.value.showLocationPermissionDialog)
    }

    @Test
    fun `onEvent with LocationPermissionDialogShown hides permission dialog`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(VisitListViewModel.UiEvent.LocationRationaleAccepted)
        assertTrue(viewModel.uiState.value.showLocationPermissionDialog)

        // Act
        viewModel.onEvent(VisitListViewModel.UiEvent.LocationPermissionDialogShown)

        // Assert
        assertFalse(viewModel.uiState.value.showLocationPermissionDialog)
    }

    private fun createViewModel(
        visitHouseholderRepositoryRef: MockReferenceHolder<VisitHouseholderRepository>? = null
    ): VisitListViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val locationFlow = MutableStateFlow<UserLocationProvider.UserLocation>(UserLocationProvider.UserLocation.NotAvailable)
        val userLocationProvider = mock<UserLocationProvider> {
            on { location } doReturn locationFlow
        }
        val permissionChecker = mock<PermissionChecker> {
            on { hasPermissions(any(), any()) } doReturn false
        }
        val visitHouseholderRepository = mock<VisitHouseholderRepository> {
            on { getAll() } doReturn createVisitHouseholderList()
        }
        visitHouseholderRepositoryRef?.value = visitHouseholderRepository

        val visitRepository = mock<VisitRepository>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All
            )
        }
        val addressProvider = mock<AddressProvider>()
        val osrmRoutingProvider = mock<OsrmRoutingProvider>()
        val calendarEventManager = mock<CalendarEventManager>()
        val moshi = mock<Moshi>()

        return VisitListViewModel(
            moshi = moshi,
            dispatchers = dispatchers,
            visitRepository = visitRepository,
            visitHouseholderRepository = visitHouseholderRepository,
            preferenceRepository = preferenceRepository,
            addressProvider = addressProvider,
            userLocationProvider = userLocationProvider,
            permissionChecker = permissionChecker,
            osrmRoutingProvider = osrmRoutingProvider,
            calendarEventManager = calendarEventManager
        )
    }

    private fun createVisitHouseholderList(): List<VisitHouseholder> {
        return listOf(
            VisitHouseholder(
                visitId = FIRST_VISIT_ID,
                subject = "Subject 1",
                date = LocalDateTime.now(),
                isDone = false,
                householderId = FIRST_HOUSEHOLDER_ID,
                householderName = "Householder 1",
                householderAddress = "Address 1",
                type = VisitType.FIRST_VISIT,
                householderLatitude = null,
                householderLongitude = null
            ),
            VisitHouseholder(
                visitId = SECOND_VISIT_ID,
                subject = "Subject 2",
                date = LocalDateTime.now().plusDays(1),
                isDone = false,
                householderId = SECOND_HOUSEHOLDER_ID,
                householderName = "Householder 2",
                householderAddress = "Address 2",
                type = VisitType.RETURN_VISIT,
                householderLatitude = 40.7128,
                householderLongitude = -74.0060
            )
        )
    }

    companion object {
        private val FIRST_VISIT_ID = UUID.fromString("3f2b7d9a-8c4e-4e2a-9b1d-5c6a7f8e1a23")
        private val SECOND_VISIT_ID = UUID.fromString("c1a9f7b4-2e3d-4f5a-8b6c-0d1e2f3a4b5c")
        private val FIRST_HOUSEHOLDER_ID = UUID.fromString("7a4e1c9b-6d2f-4a3e-8b5c-0f9d1e2a3c4b")
        private val SECOND_HOUSEHOLDER_ID = UUID.fromString("5c4b3a2f-1e9d-7c6b-4a3e-8b5c0f9d1e2a")
    }
}
