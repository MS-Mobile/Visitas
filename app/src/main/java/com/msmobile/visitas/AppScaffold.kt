package com.msmobile.visitas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.BottomNavigation
import com.msmobile.visitas.util.IntentState
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@Composable
fun AppScaffold(
    uiState: MainActivityViewModel.UiState,
    currentDestination: DestinationSpec,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit,
    onNavigateToTab: (DirectionDestinationSpec) -> Unit,
    onNavigate: (Direction) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val showBottomBar = uiState.scaffoldState.showBottomBar
    VisitasTheme {
        Scaffold(floatingActionButton = {
            if (uiState.scaffoldState.showFAB) {
                FloatingActionButton(onClick = {
                    onEvent(MainActivityViewModel.UiEvent.FabClicked(currentDestination = currentDestination))
                }) {
                    Icon(
                        Icons.Rounded.Add,
                        stringResource(id = R.string.add)
                    )
                }
            }
        }, bottomBar = {
            if (showBottomBar) {
                BottomNavigation(currentDestination, onNavigateToTab)
            }
        }) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                color = MaterialTheme.colorScheme.background
            ) {
                content(paddingValues)
                StateHandler(uiState, onEvent, onNavigate)
            }
        }
    }
}

@Composable
private fun StateHandler(
    uiState: MainActivityViewModel.UiState,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit,
    onNavigate: (Direction) -> Unit
) {
    when (val eventState = uiState.eventState) {
        is MainActivityViewModel.UiEventState.Idle -> {}
        is MainActivityViewModel.UiEventState.HandleFabClick -> {
            onNavigate(Direction(eventState.fabDestination.route))
            onEvent(MainActivityViewModel.UiEvent.FabClickHandled)
        }
    }
}

@Composable
@Preview
private fun PreviewAppScaffold() {
    AppScaffold(
        uiState = MainActivityViewModel.UiState(
            scaffoldState = MainActivityViewModel.ScaffoldState(
                showBottomBar = true,
                showFAB = true
            ),
            eventState = MainActivityViewModel.UiEventState.Idle,
            intentState = IntentState.None
        ),
        currentDestination = VisitListScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) {

    }
}