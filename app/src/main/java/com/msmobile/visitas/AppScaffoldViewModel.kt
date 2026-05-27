package com.msmobile.visitas

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppScaffoldViewModel
@Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // The token identifies the screen that currently owns the chrome, so a screen
    // leaving composition only clears chrome it still owns. Without this, the exiting
    // screen's onDispose (which runs after the entering screen has already published)
    // would wipe the new screen's chrome.
    private var currentOwner: Any? = null

    fun setUiState(owner: Any, uiState: UiState) {
        currentOwner = owner
        _uiState.value = uiState
    }

    fun clearUiState(owner: Any) {
        if (currentOwner === owner) {
            currentOwner = null
            _uiState.value = UiState()
        }
    }

    data class UiState(
        val topBarActions: List<TopBarAction> = emptyList(),
        val detailFooterActions: DetailFooterActions? = null,
    )
}
