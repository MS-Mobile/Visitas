package com.msmobile.visitas

import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class MainActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(IntentState.None, state.intentState)
        assertFalse(state.scaffoldState.showBottomBar)
        assertFalse(state.scaffoldState.showFAB)
        assertEquals(MainActivityViewModel.UiEventState.Idle, state.eventState)
    }

    @Test
    fun `onEvent with ScaffoldConfigurationChanged updates scaffoldState`() {
        // Arrange
        val viewModel = createViewModel()
        val newScaffoldState = MainActivityViewModel.ScaffoldState(
            showBottomBar = true,
            showFAB = true
        )

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.ScaffoldConfigurationChanged(newScaffoldState))

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.scaffoldState.showBottomBar)
        assertTrue(state.scaffoldState.showFAB)
    }

    @Test
    fun `onEvent with FabClicked from VisitListScreen navigates to VisitDetailScreen`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.FabClicked(VisitListScreenDestination))

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.eventState is MainActivityViewModel.UiEventState.HandleFabClick)
        val handleFabClick = state.eventState as MainActivityViewModel.UiEventState.HandleFabClick
        assertEquals(VisitDetailScreenDestination, handleFabClick.fabDestination)
    }

    @Test
    fun `onEvent with FabClicked from ConversationListScreen navigates to ConversationDetailScreen`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.FabClicked(ConversationListScreenDestination))

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.eventState is MainActivityViewModel.UiEventState.HandleFabClick)
        val handleFabClick = state.eventState as MainActivityViewModel.UiEventState.HandleFabClick
        assertEquals(ConversationDetailScreenDestination, handleFabClick.fabDestination)
    }

    @Test
    fun `onEvent with FabClickHandled resets eventState to Idle`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(MainActivityViewModel.UiEvent.FabClicked(VisitListScreenDestination))

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.FabClickHandled)

        // Assert
        val state = viewModel.uiState.value
        assertEquals(MainActivityViewModel.UiEventState.Idle, state.eventState)
    }

    @Test
    fun `onEvent with IntentStateChanged updates intentState`() {
        // Arrange
        val uriRef = MockReferenceHolder<android.net.Uri>()
        val viewModel = createViewModel(uriRef = uriRef)
        val uri = requireNotNull(uriRef.value)
        val newIntentState = IntentState.PreviewBackupFile(uri)

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.IntentStateChanged(newIntentState))

        // Assert
        assertEquals(newIntentState, viewModel.uiState.value.intentState)
    }

    @Test
    fun `onEvent with IntentStateHandled resets intentState to None`() {
        // Arrange
        val uriRef = MockReferenceHolder<android.net.Uri>()
        val viewModel = createViewModel(uriRef = uriRef)
        val uri = requireNotNull(uriRef.value)
        viewModel.onEvent(MainActivityViewModel.UiEvent.IntentStateChanged(IntentState.PreviewBackupFile(uri)))

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.IntentStateHandled)

        // Assert
        assertEquals(IntentState.None, viewModel.uiState.value.intentState)
    }

    @Test
    fun `onEvent with NetworkStatusChangeAcknowledged resets eventState to Idle`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(MainActivityViewModel.UiEvent.FabClicked(VisitListScreenDestination))

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.NetworkStatusChangeAcknowledged)

        // Assert
        assertEquals(MainActivityViewModel.UiEventState.Idle, viewModel.uiState.value.eventState)
    }

    private fun createViewModel(
        uriRef: MockReferenceHolder<android.net.Uri>? = null
    ): MainActivityViewModel {
        val mockUri = mock<android.net.Uri>()
        uriRef?.value = mockUri

        return MainActivityViewModel()
    }
}
