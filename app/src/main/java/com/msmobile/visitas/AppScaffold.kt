package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.msmobile.visitas.ui.views.FloatingBar
import com.msmobile.visitas.ui.views.PreviewCompatDropdownMenu
import com.msmobile.visitas.util.borderPadding
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
    onNavigateUp: () -> Unit = {},
    topBarActions: List<TopBarAction> = emptyList(),
    detailFooterActions: List<DetailFooterAction> = emptyList(),
    floatingActionButtonActions: List<FloatingActionButtonAction> = emptyList(),
    subtitle: String? = null,
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
        ConversationDetailScreenDestination,
        SettingsScreenDestination
    )
    val showBackButton = currentDestination == SettingsScreenDestination
    val showBottomNavigation = currentDestination in listOf(
        VisitListScreenDestination,
        ConversationListScreenDestination
    )
    val showSettingsMenu = currentDestination in listOf(
        VisitListScreenDestination,
        ConversationListScreenDestination
    )
    val title = when (currentDestination) {
        VisitListScreenDestination,
        VisitDetailScreenDestination -> stringResource(id = R.string.visits)

        ConversationListScreenDestination,
        ConversationDetailScreenDestination -> stringResource(id = R.string.conversations)

        SettingsScreenDestination -> stringResource(id = R.string.settings)

        else -> stringResource(id = R.string.app_name)
    }
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        ScaffoldTitle(title = title, subtitle = subtitle)
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(id = R.string.navigate_back_content_description)
                                )
                            }
                        }
                    },
                    actions = {
                        ScaffoldTopBar(topBarActions = topBarActions)
                        if (showSettingsMenu) {
                            SettingsMenu(onNavigate = onNavigate)
                        }
                    }
                )
            }
        },
        content = { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
                StateHandler(uiState, onEvent, onNavigate)
            }
        },
        bottomBar = {
            when {
                detailFooterActions.isNotEmpty() || floatingActionButtonActions.isNotEmpty() -> {
                    ScaffoldDetailFooter(
                        detailFooterActions = detailFooterActions,
                        floatingActionButtonActions = floatingActionButtonActions
                    )
                }

                showBottomNavigation -> {
                    ScaffoldBottomNavigation(
                        showFAB = showFAB,
                        currentDestination = currentDestination,
                        onEvent = onEvent,
                        onNavigateToTab = onNavigateToTab
                    )
                }
            }
        }
    )
}

@Composable
private fun ScaffoldTitle(title: String, subtitle: String?) {
    // Retain the last non-null subtitle so it stays rendered while the
    // exit animation plays out (subtitle is already null by then).
    var lastSubtitle by remember { mutableStateOf(subtitle) }
    LaunchedEffect(subtitle) {
        if (subtitle != null) lastSubtitle = subtitle
    }
    Column {
        Text(text = title)
        AnimatedVisibility(visible = subtitle != null) {
            Text(
                text = lastSubtitle.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ScaffoldTopBar(topBarActions: List<TopBarAction>) {
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
}

@Composable
private fun SettingsMenu(onNavigate: (Direction) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { menuExpanded = true }) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.more_options)
        )
    }
    PreviewCompatDropdownMenu(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScaffoldDetailFooter(
    detailFooterActions: List<DetailFooterAction>,
    floatingActionButtonActions: List<FloatingActionButtonAction>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(modifier = Modifier.offset(y = -FloatingToolbarDefaults.ScreenOffset)) {
            FloatingBar(
                modifier = Modifier.weight(weight = .5f, fill = false),
                floatingActionButton = {
                    floatingActionButtonActions.forEach { action ->
                        FloatingActionButton(
                            modifier = Modifier.padding(end = borderPadding),
                            onClick = action.onClick,
                            containerColor = vibrantFloatingToolbarColors().fabContainerColor,
                            contentColor = vibrantFloatingToolbarColors().fabContentColor
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription
                            )
                        }
                    }
                },
                content = {
                    detailFooterActions.forEach { action ->
                        IconButton(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription
                            )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScaffoldBottomNavigation(
    showFAB: Boolean,
    currentDestination: DestinationSpec,
    onEvent: (MainActivityViewModel.UiEvent) -> Unit,
    onNavigateToTab: (DirectionDestinationSpec) -> Unit
) {
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
internal fun AppScaffoldPreview(
    @PreviewParameter(AppScaffoldPreviewConfigProvider::class) config: AppScaffoldPreviewConfig
) {
    VisitasTheme {
        AppScaffold(
            uiState = config.uiState,
            currentDestination = config.currentDestination,
            onEvent = {},
            onNavigateToTab = {},
            onNavigate = {},
            subtitle = config.subtitle
        ) {}
    }
}