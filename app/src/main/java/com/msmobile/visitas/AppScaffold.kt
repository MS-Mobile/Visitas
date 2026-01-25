package com.msmobile.visitas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val timerFABIcon = if (uiState.isTimerRunning) {
        Icons.Rounded.Pause
    } else {
        Icons.Rounded.PlayArrow
    }
    VisitasTheme {
        Scaffold(floatingActionButton = {
            Column {
                if (uiState.scaffoldState.showTimerFAB) {
                    FloatingActionButton(onClick = {
                        onEvent(MainActivityViewModel.UiEvent.TimerFabClicked)
                    }) {
                        Icon(
                            timerFABIcon,
                            stringResource(id = R.string.start_timer)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
                showFAB = true,
                showTimerFAB = true
            ),
            eventState = MainActivityViewModel.UiEventState.Idle,
            isTimerRunning = true,
            intentState = IntentState.None
        ),
        currentDestination = VisitListScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) {

    }
}