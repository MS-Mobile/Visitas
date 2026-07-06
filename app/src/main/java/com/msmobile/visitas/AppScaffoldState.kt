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
        val topNavigationActions: List<TopNavigationAction> = emptyList(),
        val topBarActions: List<TopBarAction> = emptyList(),
        val topMenuActions: List<TopMenuAction> = emptyList(),
        val detailFooterActions: List<DetailFooterAction> = emptyList(),
        val floatingActionButtonActions: List<FloatingActionButtonAction> = emptyList(),
        // Optional accent-styled supporting line shown under the top bar title that any
        // screen can set (e.g. to surface a status like "Draft").
        val subtitle: String? = null,
    )
}
