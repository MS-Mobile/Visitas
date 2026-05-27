package com.msmobile.visitas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class AppScaffoldState {
    var uiState: UiState by mutableStateOf(UiState())
        private set

    // The token identifies the screen that currently owns the chrome, so a screen
    // leaving composition only clears chrome it still owns. Without this, the exiting
    // screen's onDispose (which runs after the entering screen has already published)
    // would wipe the new screen's chrome.
    private var currentOwner: Any? = null

    fun setUiState(owner: Any, uiState: UiState) {
        currentOwner = owner
        this.uiState = uiState
    }

    fun clearUiState(owner: Any) {
        if (currentOwner === owner) {
            currentOwner = null
            uiState = UiState()
        }
    }

    data class UiState(
        val topBarActions: List<TopBarAction> = emptyList(),
        val detailFooterActions: DetailFooterActions? = null,
    )
}
