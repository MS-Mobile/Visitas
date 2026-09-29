package com.msmobile.visitas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.msmobile.visitas.conversation.ConversationDetailScreen
import com.msmobile.visitas.conversation.ConversationListScreen
import com.msmobile.visitas.navigation.AppDestination
import com.msmobile.visitas.settings.SettingsScreen
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.ListScreenStyle
import com.msmobile.visitas.util.scaffold.AppScaffoldState
import com.msmobile.visitas.visit.VisitDetailScreen
import com.msmobile.visitas.visit.VisitListScreen

@Composable
fun Main(
    uiState: MainActivityViewModel.UiState,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit
) {
    val backStack = rememberNavBackStack(AppDestination.VisitList)
    val currentDestination by remember {
        derivedStateOf { backStack.last() as AppDestination }
    }
    val appScaffoldState = remember { AppScaffoldState() }
    val intentState = uiState.intentState
    val intentStateHandled = {
        onEvent(MainActivityViewModel.UiEvent.IntentStateHandled)
    }
    // Tabs sit directly on top of the start destination, so switching tabs drops whatever the
    // current tab stacked above the root before pushing the new one.
    val onNavigateToTab = { destination: AppDestination ->
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (destination != AppDestination.VisitList) {
            backStack.add(destination)
        }
    }
    // A double tap during the exit animation must not push the same key twice: Navigation 3 keys
    // entry state by key, so a duplicate would share (or clash with) the entry below it.
    val onNavigate = { destination: AppDestination ->
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }
    val onNavigateUp = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
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
                NavDisplay(
                    backStack = backStack,
                    onBack = { onNavigateUp() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<AppDestination.VisitList>(metadata = ListScreenStyle.metadata) {
                            VisitListScreen(
                                onNavigate = onNavigate,
                                appScaffoldState = appScaffoldState,
                                intentState = intentState,
                                onIntentStateHandled = intentStateHandled
                            )
                        }
                        entry<AppDestination.ConversationList>(metadata = ListScreenStyle.metadata) {
                            ConversationListScreen(
                                onNavigate = onNavigate,
                                appScaffoldState = appScaffoldState
                            )
                        }
                        entry<AppDestination.VisitDetail>(metadata = DetailScreenStyle.metadata) { key ->
                            VisitDetailScreen(
                                householderId = key.householderId,
                                onNavigate = onNavigate,
                                onNavigateUp = onNavigateUp,
                                appScaffoldState = appScaffoldState
                            )
                        }
                        entry<AppDestination.ConversationDetail>(metadata = DetailScreenStyle.metadata) { key ->
                            ConversationDetailScreen(
                                firstConversationId = key.firstConversationId,
                                onNavigateUp = onNavigateUp,
                                appScaffoldState = appScaffoldState
                            )
                        }
                        entry<AppDestination.Settings>(metadata = DetailScreenStyle.metadata) {
                            SettingsScreen(
                                onNavigateUp = onNavigateUp,
                                appScaffoldState = appScaffoldState
                            )
                        }
                    }
                )
            }
        )
    }
}
