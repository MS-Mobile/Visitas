package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.BottomNavigation
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
    Scaffold { paddingValues ->
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
            if (showBottomBar) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FloatingBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        floatingActionButton = {
                            if (uiState.scaffoldState.showFAB) {
                                FloatingAddButton(
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
                            BottomNavigation(currentDestination, onNavigateToTab)
                        }
                    )
                }
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