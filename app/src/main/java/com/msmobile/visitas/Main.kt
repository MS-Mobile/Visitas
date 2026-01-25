package com.msmobile.visitas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.msmobile.visitas.di.navigationDependencies
import com.msmobile.visitas.extension.currentDestinationWithLifecycle
import com.msmobile.visitas.util.IntentState
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator

@Composable
fun Main(
    uiState: MainActivityViewModel.UiState,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit
) {
    val navController = rememberNavController()
    val destinationsNavigator = navController.rememberDestinationsNavigator()
    val currentDestination by navController.currentDestinationWithLifecycle()
    val intentState = uiState.intentState
    val scaffoldConfigurationChanged = { scaffoldState: MainActivityViewModel.ScaffoldState ->
        onEvent(MainActivityViewModel.UiEvent.ScaffoldConfigurationChanged(scaffoldState))
    }
    val intentStateHandled = {
        onEvent(MainActivityViewModel.UiEvent.IntentStateHandled)
    }
    val onNavigateToTab = { destination: DirectionDestinationSpec ->
        destinationsNavigator.navigate(destination) {
            launchSingleTop = true
            restoreState = true
            popUpTo(NavGraphs.root.startRoute) {
                saveState = true
            }
        }
    }
    val onNavigate = { direction: Direction ->
        destinationsNavigator.navigate(direction)
    }
    AppScaffold(
        uiState = uiState,
        currentDestination = currentDestination,
        onEvent = onEvent,
        onNavigateToTab = onNavigateToTab,
        onNavigate = onNavigate,
        content = { paddingValues: PaddingValues ->
            DestinationsNavHost(
                navGraph = NavGraphs.root,
                navController = navController,
                dependenciesContainerBuilder = navigationDependencies(
                    intentState = intentState,
                    intentStateHandled = intentStateHandled,
                    scaffoldConfigurationChanged = scaffoldConfigurationChanged,
                    paddingValues = paddingValues
                )
            )
        }
    )
}
