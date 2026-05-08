package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.BottomNavigationTabs
import com.msmobile.visitas.ui.views.FloatingAddButton
import com.msmobile.visitas.ui.views.FloatingBar
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    Scaffold(
        content = { paddingValues ->
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
        },
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FloatingBar(
                        modifier = Modifier.offset(y = -FloatingToolbarDefaults.ScreenOffset),
                        floatingActionButton = {
                            if (uiState.scaffoldState.showFAB) {
                                FloatingAddButton(
                                    modifier = Modifier.offset(y = -FloatingToolbarDefaults.ScreenOffset),
                                    onFabClickedEvent = {
                                        onEvent(
                                            MainActivityViewModel.UiEvent.FabClicked(
                                                currentDestination = currentDestination
                                            )
                                        )
                                    }
                                )
                            }
                        },
                        content = {
                            BottomNavigationTabs(
                                currentDestination,
                                onNavigateToTab
                            )
                        }
                    )
                }
            }
        }
    )
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

@VisibleForTesting
@Composable
@PreviewPhone
@PreviewFoldable
internal fun AppScaffoldPreview(
    @PreviewParameter(AppScaffoldPreviewConfigProvider::class) config: AppScaffoldPreviewConfig
) {
    VisitasTheme {
        AppScaffold(
            uiState = config.uiState,
            currentDestination = config.currentDestination,
            onEvent = {},
            onNavigateToTab = {},
            onNavigate = {}
        ) {}
    }
}