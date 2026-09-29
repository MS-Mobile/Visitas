package com.msmobile.visitas

import androidx.lifecycle.ViewModel
import com.msmobile.visitas.navigation.AppDestination
import com.msmobile.visitas.util.IntentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

typealias OnIntentStateHandled = () -> Unit

@HiltViewModel
class MainActivityViewModel
@Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            intentState = IntentState.None,
            eventState = UiEventState.Idle
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(uiEvent: UiEvent) {
        when (uiEvent) {
            is UiEvent.FabClicked -> fabClicked(uiEvent.currentDestination)
            is UiEvent.FabClickHandled -> fabClickHandled()
            is UiEvent.NetworkStatusChangeAcknowledged -> networkStatusChangeAcknowledged()
            is UiEvent.IntentStateChanged -> intentStateChanged(uiEvent.intentState)
            is UiEvent.IntentStateHandled -> intentStateHandled()
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

    private fun fabClicked(currentDestination: AppDestination) {
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

    private fun newState(value: UiState.() -> UiState) {
        _uiState.update(value)
    }

    private val AppDestination.asFabDestination: AppDestination
        get() {
            return when (this) {
                is AppDestination.VisitList -> AppDestination.VisitDetail()
                is AppDestination.ConversationList -> AppDestination.ConversationDetail()
                else -> this
            }
        }

    sealed class UiEvent {
        data class FabClicked(val currentDestination: AppDestination) : UiEvent()
        data object FabClickHandled : UiEvent()
        data object NetworkStatusChangeAcknowledged : UiEvent()
        data class IntentStateChanged(val intentState: IntentState) : UiEvent()
        data object IntentStateHandled : UiEvent()
    }

    sealed class UiEventState {
        data object Idle : UiEventState()

        data class HandleFabClick(
            val fabDestination: AppDestination
        ) : UiEventState()
    }

    data class UiState(
        val intentState: IntentState,
        val eventState: UiEventState
    )
}