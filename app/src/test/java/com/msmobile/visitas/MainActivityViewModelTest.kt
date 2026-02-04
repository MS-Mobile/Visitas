package com.msmobile.visitas

import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.TimerManager
import com.msmobile.visitas.util.TimerManager.TimerState
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

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
        assertFalse(state.scaffoldState.showTimerFAB)
        assertEquals(MainActivityViewModel.UiEventState.Idle, state.eventState)
        assertFalse(state.isTimerRunning)
    }

    @Test
    fun `onEvent with ScaffoldConfigurationChanged updates scaffoldState`() {
        // Arrange
        val viewModel = createViewModel()
        val newScaffoldState = MainActivityViewModel.ScaffoldState(
            showBottomBar = true,
            showFAB = true,
            showTimerFAB = true
        )

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.ScaffoldConfigurationChanged(newScaffoldState))

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.scaffoldState.showBottomBar)
        assertTrue(state.scaffoldState.showFAB)
        assertTrue(state.scaffoldState.showTimerFAB)
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
    fun `onEvent with TimerFabClicked toggles isTimerRunning`() {
        // Arrange
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isTimerRunning)

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.TimerFabClicked)

        // Assert
        assertTrue(viewModel.uiState.value.isTimerRunning)

        // Act again
        viewModel.onEvent(MainActivityViewModel.UiEvent.TimerFabClicked)

        // Assert
        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `onEvent with IntentStateChanged updates intentState`() {
        // Arrange
        val viewModel = createViewModel()
        val mockUri = mock<android.net.Uri>()
        val newIntentState = IntentState.PreviewBackupFile(mockUri)

        // Act
        viewModel.onEvent(MainActivityViewModel.UiEvent.IntentStateChanged(newIntentState))

        // Assert
        assertEquals(newIntentState, viewModel.uiState.value.intentState)
    }

    @Test
    fun `onEvent with IntentStateHandled resets intentState to None`() {
        // Arrange
        val viewModel = createViewModel()
        val mockUri = mock<android.net.Uri>()
        viewModel.onEvent(MainActivityViewModel.UiEvent.IntentStateChanged(IntentState.PreviewBackupFile(mockUri)))

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

    @Test
    fun `timer running state updates isTimerRunning to true`() {
        // Arrange
        val timerFlow = MutableStateFlow<TimerState>(TimerState.Stopped)
        val timerManager = mock<TimerManager> {
            on { timer } doReturn timerFlow
        }
        val viewModel = MainActivityViewModel(timerManager)

        // Act
        timerFlow.value = TimerState.Running(
            elapsedTime = 10.seconds,
            tickUnit = 1.seconds
        )

        // Assert
        assertTrue(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `timer paused state updates isTimerRunning to false`() {
        // Arrange
        val timerFlow = MutableStateFlow<TimerState>(TimerState.Running(10.seconds, 1.seconds))
        val timerManager = mock<TimerManager> {
            on { timer } doReturn timerFlow
        }
        val viewModel = MainActivityViewModel(timerManager)
        assertTrue(viewModel.uiState.value.isTimerRunning)

        // Act
        timerFlow.value = TimerState.Paused(
            elapsedTime = 10.seconds,
            tickUnit = 1.seconds
        )

        // Assert
        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    private fun createViewModel(): MainActivityViewModel {
        val timerFlow = MutableStateFlow<TimerState>(TimerState.Stopped)
        val timerManager = mock<TimerManager> {
            on { timer } doReturn timerFlow
        }
        return MainActivityViewModel(timerManager)
    }
}
