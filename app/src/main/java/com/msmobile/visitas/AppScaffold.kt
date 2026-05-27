package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.BottomNavigation
import com.msmobile.visitas.ui.views.DetailFooter
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    uiState: MainActivityViewModel.UiState,
    currentDestination: DestinationSpec,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit,
    onNavigateToTab: (DirectionDestinationSpec) -> Unit,
    onNavigate: (Direction) -> Unit,
    topBarActions: List<TopBarAction> = emptyList(),
    detailFooterActions: DetailFooterActions? = null,
    content: @Composable () -> Unit
) {
    val showFAB = currentDestination in listOf(
        VisitListScreenDestination,
        ConversationListScreenDestination
    )
    val showTopBar = currentDestination in listOf(
        VisitListScreenDestination,
        ConversationListScreenDestination,
        VisitDetailScreenDestination,
        ConversationDetailScreenDestination
    )
    val showBottomNavigation = currentDestination in listOf(
        VisitListScreenDestination,
        ConversationListScreenDestination,
        ConversationDetailScreenDestination
    )
    val showSettingsMenu = currentDestination != VisitDetailScreenDestination
    val title = if (currentDestination == VisitDetailScreenDestination) {
        stringResource(id = R.string.visits)
    } else {
        ""
    }
    Scaffold(
        topBar = {
            if (showTopBar) {
                var menuExpanded by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text(text = title) },
                    actions = {
                        topBarActions.forEach { action ->
                            Box {
                                IconButton(onClick = action.onClick) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.contentDescription
                                    )
                                }
                                action.menu?.invoke()
                            }
                        }
                        if (showSettingsMenu) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(id = R.string.more_options)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(stringResource(id = R.string.settings)) },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigate(SettingsScreenDestination)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        content = { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
                StateHandler(uiState, onEvent, onNavigate)
            }
        },
        bottomBar = {
            when {
                detailFooterActions != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DetailFooter(
                            modifier = Modifier.offset(y = -FloatingToolbarDefaults.ScreenOffset),
                            onBackClicked = detailFooterActions.onBack,
                            onSaveClickedEvent = detailFooterActions.onSave,
                            onFabClickedEvent = detailFooterActions.onAdd
                        )
                    }
                }

                showBottomNavigation -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .offset(y = -FloatingToolbarDefaults.ScreenOffset),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BottomNavigation(
                            showFAB = showFAB,
                            onFabClickedEvent = {
                                onEvent(
                                    MainActivityViewModel.UiEvent.FabClicked(
                                        currentDestination = currentDestination
                                    )
                                )
                            },
                            currentDestination = currentDestination,
                            onNavigateToTab = onNavigateToTab
                        )
                    }
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