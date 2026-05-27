package com.msmobile.visitas

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppScaffoldViewModel
@Inject constructor() : ViewModel() {
    private val _topBarActions = MutableStateFlow<List<TopBarAction>>(emptyList())
    val topBarActions: StateFlow<List<TopBarAction>> = _topBarActions

    private val _detailFooterActions = MutableStateFlow<DetailFooterActions?>(null)
    val detailFooterActions: StateFlow<DetailFooterActions?> = _detailFooterActions

    // Tokens identify the screen that currently owns each slot, so a screen leaving
    // composition only clears chrome it still owns. Without this, the exiting screen's
    // onDispose (which runs after the entering screen has already published) would wipe
    // the new screen's chrome.
    private var topBarActionsOwner: Any? = null
    private var detailFooterActionsOwner: Any? = null

    fun setTopBarActions(owner: Any, actions: List<TopBarAction>) {
        topBarActionsOwner = owner
        _topBarActions.value = actions
    }

    fun clearTopBarActions(owner: Any) {
        if (topBarActionsOwner === owner) {
            topBarActionsOwner = null
            _topBarActions.value = emptyList()
        }
    }

    fun setDetailFooterActions(owner: Any, actions: DetailFooterActions) {
        detailFooterActionsOwner = owner
        _detailFooterActions.value = actions
    }

    fun clearDetailFooterActions(owner: Any) {
        if (detailFooterActionsOwner === owner) {
            detailFooterActionsOwner = null
            _detailFooterActions.value = null
        }
    }
}