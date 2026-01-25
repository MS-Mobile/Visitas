package com.msmobile.visitas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.TimerManager
import com.msmobile.visitas.util.TimerManager.TimerState
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

typealias OnScaffoldConfigurationChanged = (MainActivityViewModel.ScaffoldState) -> Unit
typealias OnIntentStateHandled = () -> Unit

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    timerManager: TimerManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            intentState = IntentState.None,
            scaffoldState = ScaffoldState(),
            eventState = UiEventState.Idle,
            isTimerRunning = false
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    init {
        timerManager.timer.onEach(::timerChanged).launchIn(viewModelScope)
    }

    fun onEvent(uiEvent: UiEvent) {
        when (uiEvent) {
            is UiEvent.FabClicked -> fabClicked(uiEvent.currentDestination)
            is UiEvent.ScaffoldConfigurationChanged -> scaffoldConfigurationChanged(uiEvent.scaffoldState)
            is UiEvent.FabClickHandled -> fabClickHandled()
            is UiEvent.NetworkStatusChangeAcknowledged -> networkStatusChangeAcknowledged()
            is UiEvent.TimerFabClicked -> timerFabClicked()
            is UiEvent.IntentStateChanged -> intentStateChanged(uiEvent.intentState)
            is UiEvent.IntentStateHandled -> intentStateHandled()
        }
    }

    private fun timerChanged(timerState: TimerState) {
        when (timerState) {
            is TimerState.Running -> newState {
                copy(isTimerRunning = true)
            }

            is TimerState.Paused, TimerState.Stopped -> newState {
                copy(isTimerRunning = false)
            }
        }
    }

    private fun timerFabClicked() {
        newState {
            copy(isTimerRunning = !isTimerRunning)
        }
    }

    private fun intentStateChanged(intentState: IntentState) {
        newState {
            copy(intentState = intentState)
        }
    }

    private fun intentStateHandled() {
        newState {
            copy(intentState = IntentState.None)
        }
    }

    private fun networkStatusChangeAcknowledged() {
        newState {
            copy(eventState = UiEventState.Idle)
        }
    }

    private fun fabClicked(currentDestination: DestinationSpec) {
        newState {
            val fabDestination = currentDestination.asFabDestination
            copy(eventState = UiEventState.HandleFabClick(fabDestination))
        }
    }

    private fun fabClickHandled() {
        newState {
            copy(eventState = UiEventState.Idle)
        }
    }

    private fun scaffoldConfigurationChanged(scaffoldState: ScaffoldState) {
        newState {
            copy(scaffoldState = scaffoldState)
        }
    }

    private fun newState(value: UiState.() -> UiState) {
        _uiState.update(value)
    }

    private val DestinationSpec.asFabDestination: DestinationSpec
        get() {
            return when (this) {
                is VisitListScreenDestination -> VisitDetailScreenDestination
                is ConversationListScreenDestination -> ConversationDetailScreenDestination
                else -> this
            }
        }

    sealed class UiEvent {
        data class FabClicked(val currentDestination: DestinationSpec) : UiEvent()
        data class ScaffoldConfigurationChanged(val scaffoldState: ScaffoldState) : UiEvent()
        data object FabClickHandled : UiEvent()
        data object NetworkStatusChangeAcknowledged : UiEvent()
        data object TimerFabClicked : UiEvent()
        data class IntentStateChanged(val intentState: IntentState) : UiEvent()
        data object IntentStateHandled : UiEvent()
    }

    data class ScaffoldState(
        val showBottomBar: Boolean = false,
        val showFAB: Boolean = false,
        val showTimerFAB: Boolean = false
    )

    sealed class UiEventState {
        data object Idle : UiEventState()

        data class HandleFabClick(
            val fabDestination: DestinationSpec
        ) : UiEventState()
    }

    data class UiState(
        val intentState: IntentState,
        val scaffoldState: ScaffoldState,
        val eventState: UiEventState,
        val isTimerRunning: Boolean
    )
}