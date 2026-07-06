package com.msmobile.visitas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.msmobile.visitas.di.navigationDependencies
import com.msmobile.visitas.extension.currentDestinationWithLifecycle
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.scaffold.AppScaffoldState
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
    val appScaffoldState = remember { AppScaffoldState() }
    val intentState = uiState.intentState
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
    VisitasTheme {
        AppScaffold(
            uiState = uiState,
            currentDestination = currentDestination,
            onEvent = onEvent,
            onNavigateToTab = onNavigateToTab,
            onNavigate = onNavigate,
            topNavigationActions = appScaffoldState.uiState.topNavigationActions,
            topBarActions = appScaffoldState.uiState.topBarActions,
            topMenuActions = appScaffoldState.uiState.topMenuActions,
            detailFooterActions = appScaffoldState.uiState.detailFooterActions,
            floatingActionButtonActions = appScaffoldState.uiState.floatingActionButtonActions,
            subtitle = appScaffoldState.uiState.subtitle,
            content = {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    navController = navController,
                    dependenciesContainerBuilder = navigationDependencies(
                        intentState = intentState,
                        intentStateHandled = intentStateHandled,
                        appScaffoldState = appScaffoldState
                    )
                )
            }
        )
    }
}
