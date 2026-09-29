package com.msmobile.visitas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.msmobile.visitas.conversation.ConversationDetailScreen
import com.msmobile.visitas.conversation.ConversationListScreen
import com.msmobile.visitas.navigation.AppDestination
import com.msmobile.visitas.settings.SettingsScreen
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.ListScreenStyle
import com.msmobile.visitas.util.scaffold.AppScaffoldState
import com.msmobile.visitas.visit.VisitDetailScreen
import com.msmobile.visitas.visit.VisitListScreen

/**
 * Hosts the app's navigation. Each bottom-navigation tab keeps its own back stack, decorated
 * separately, so a tab's screens keep their ViewModels and saved UI state (search text, scroll
 * position) while another tab is shown. Only the selected tab's entries are handed to NavDisplay.
 *
 * Each tab's root is the bottom of its own stack, so NavDisplay has nothing to pop on a tab root and
 * the tab screen's own back handling always applies.
 */
@Composable
fun Main(
    uiState: MainActivityViewModel.UiState,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit
) {
    val visitsBackStack = rememberNavBackStack(Tab.Visits.root)
    val conversationsBackStack = rememberNavBackStack(Tab.Conversations.root)
    var currentTab by rememberSaveable { mutableStateOf(Tab.Visits) }
    val currentBackStack = when (currentTab) {
        Tab.Visits -> visitsBackStack
        Tab.Conversations -> conversationsBackStack
    }
    val currentDestination = currentBackStack.last() as AppDestination
    val appScaffoldState = remember { AppScaffoldState() }
    // Entries are remembered by back-stack contents, so their content must read changing values
    // through State rather than capture them when the entry is first created.
    val intentState by rememberUpdatedState(uiState.intentState)
    val intentStateHandled = {
        onEvent(MainActivityViewModel.UiEvent.IntentStateHandled)
    }
    val entryProviderFor = { backStack: NavBackStack<NavKey> ->
        appEntryProvider(
            backStack = backStack,
            appScaffoldState = appScaffoldState,
            intentState = { intentState },
            onIntentStateHandled = intentStateHandled
        )
    }
    val visitsEntries = rememberTabEntries(visitsBackStack, entryProviderFor(visitsBackStack))
    val conversationsEntries =
        rememberTabEntries(conversationsBackStack, entryProviderFor(conversationsBackStack))
    val onNavigateToTab = { destination: AppDestination ->
        currentTab = Tab.entries.first { tab -> tab.root == destination }
    }
    VisitasTheme {
        AppScaffold(
            uiState = uiState,
            currentDestination = currentDestination,
            onEvent = onEvent,
            onNavigateToTab = onNavigateToTab,
            onNavigate = { destination -> currentBackStack.navigate(destination) },
            topNavigationActions = appScaffoldState.uiState.topNavigationActions,
            topBarActions = appScaffoldState.uiState.topBarActions,
            topMenuActions = appScaffoldState.uiState.topMenuActions,
            detailFooterActions = appScaffoldState.uiState.detailFooterActions,
            floatingActionButtonActions = appScaffoldState.uiState.floatingActionButtonActions,
            subtitle = appScaffoldState.uiState.subtitle,
            content = {
                NavDisplay(
                    entries = when (currentTab) {
                        Tab.Visits -> visitsEntries
                        Tab.Conversations -> conversationsEntries
                    },
                    onBack = { currentBackStack.navigateUp() }
                )
            }
        )
    }
}

private enum class Tab(val root: AppDestination) {
    Visits(AppDestination.VisitList),
    Conversations(AppDestination.ConversationList)
}

/**
 * Decorates one tab's back stack with decorators of its own. Separate decorators keep each tab's
 * entry state apart even when both stacks hold the same key (e.g. [AppDestination.Settings]), and
 * keep it alive while the tab is not displayed: state is only cleared when a key leaves its stack.
 */
@Composable
private fun rememberTabEntries(
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>
): List<NavEntry<NavKey>> {
    return rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider
    )
}

private fun appEntryProvider(
    backStack: NavBackStack<NavKey>,
    appScaffoldState: AppScaffoldState,
    intentState: () -> IntentState,
    onIntentStateHandled: OnIntentStateHandled
): (NavKey) -> NavEntry<NavKey> {
    val onNavigate = { destination: AppDestination -> backStack.navigate(destination) }
    val onNavigateUp = { backStack.navigateUp() }
    return entryProvider {
        entry<AppDestination.VisitList>(metadata = ListScreenStyle.metadata) {
            VisitListScreen(
                onNavigate = onNavigate,
                appScaffoldState = appScaffoldState,
                intentState = intentState(),
                onIntentStateHandled = onIntentStateHandled
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
}

// A double tap during the exit animation must not push the same key twice: Navigation 3 keys
// entry state by key, so a duplicate would share (or clash with) the entry below it.
private fun NavBackStack<NavKey>.navigate(destination: AppDestination) {
    if (lastOrNull() != destination) {
        add(destination)
    }
}

private fun NavBackStack<NavKey>.navigateUp() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}
