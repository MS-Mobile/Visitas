package com.msmobile.visitas.conversation

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.OnScaffoldConfigurationChanged
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.OnBackPressed
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.ui.views.LazyColumnWithScrollbar
import com.msmobile.visitas.ui.views.SimpleSearchBar
import com.msmobile.visitas.util.ListScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.cardInnerPadding
import com.msmobile.visitas.util.floatingBarBottomPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.verticalFieldPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction

@Destination<RootGraph>(style = ListScreenStyle::class)
@Composable
fun ConversationListScreen(
    navigator: DestinationsNavigator,
    paddingValues: PaddingValues,
    viewModel: ConversationListViewModel,
    scaffoldConfigurationChanged: OnScaffoldConfigurationChanged
) {
    val uiState: ConversationListViewModel.UiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    val conversationsTitle = stringResource(R.string.conversations)
    val onNavigate = { direction: Direction ->
        navigator.navigate(direction)
    }

    LaunchedEffect(key1 = null) {
        scaffoldConfigurationChanged(
            MainActivityViewModel.ScaffoldState(
                showBottomBar = true,
                showFAB = true,
                title = conversationsTitle
            )
        )
        onEvent(ConversationListViewModel.UiEvent.ViewCreated)
    }
    OnBackPressed { }
    ConversationListScreenContent(
        paddingValues = paddingValues,
        uiState = uiState,
        onConversationListEvent = onEvent,
        onNavigate = onNavigate
    )
}

@Composable
private fun ConversationListScreenContent(
    paddingValues: PaddingValues,
    uiState: ConversationListViewModel.UiState,
    onConversationListEvent: (ConversationListViewModel.UiEvent) -> Unit,
    onNavigate: (Direction) -> Unit
) {
    val topPadding = paddingValues.calculateTopPadding()
    Column(
        modifier = Modifier.padding(top = topPadding),
        verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
    ) {
        SummaryCard(
            uiState = uiState,
            onConversationListEvent = onConversationListEvent
        )
        ConversationList(
            modifier = Modifier.padding(
                start = borderPadding,
                end = borderPadding
            ),
            paddingValues = paddingValues,
            uiState = uiState,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun SummaryCard(
    uiState: ConversationListViewModel.UiState,
    onConversationListEvent: (ConversationListViewModel.UiEvent) -> Unit
) {
    val searchValue = uiState.filter.search
    val isSearchEmpty = searchValue.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalFieldPadding,
                end = horizontalFieldPadding,
                top = verticalFieldPadding,
                bottom = borderPadding
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleSearchBar(
                modifier = Modifier.weight(1f, true),
                search = searchValue,
                onValueChange = { search ->
                    onConversationListEvent(ConversationListViewModel.UiEvent.SearchChanged(search))
                },
                isSearchEmpty = isSearchEmpty,
                onFilterCleared = {
                    onConversationListEvent(ConversationListViewModel.UiEvent.FilterCleared)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConversationList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    uiState: ConversationListViewModel.UiState,
    onNavigate: (Direction) -> Unit
) {
    val conversationList = uiState.conversations.filter { !it.hide }
    val listBottomPadding = paddingValues.calculateListBottomPadding()
    Column(modifier = modifier) {
        ConversationListHeader()
        ConversationListItems(conversationList, listBottomPadding, onNavigate)
    }
}

@Composable
private fun ConversationListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = verticalFieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // To match the Visits list height
        Spacer(modifier = Modifier.height(ButtonDefaults.MinHeight))
    }
}

@Composable
private fun ConversationListItems(
    conversationList: List<ConversationListViewModel.ConversationState>,
    listBottomPadding: Dp,
    onNavigate: (Direction) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumnWithScrollbar(listState = listState) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(verticalFieldPadding),
            contentPadding = PaddingValues(
                top = verticalFieldPadding,
                bottom = listBottomPadding
            )
        ) {
            items(
                items = conversationList,
                key = { conversation -> conversation.conversationId }) { conversation ->
                ConversationCard(
                    conversation = conversation,
                    onClick = { onNavigate(ConversationDetailScreenDestination(conversation.parentId)) }
                )
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: ConversationListViewModel.ConversationState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors()
            .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(cardInnerPadding)) {
            Text(text = conversation.question, fontWeight = FontWeight.Bold)
        }
    }
}

@VisibleForTesting
@PreviewPhone
@PreviewFoldable
@Composable
internal fun ConversationListScreenPreview(
    @PreviewParameter(PreviewConfigProvider::class) config: PreviewConfig
) {
    VisitasTheme {
        AppScaffold(
            uiState = config.mainActivityUiState,
            currentDestination = ConversationListScreenDestination,
            onEvent = {},
            onNavigateToTab = {},
            onNavigate = {}
        ) { paddingValues ->
            ConversationListScreenContent(
                paddingValues = paddingValues,
                uiState = config.conversationUiState,
                onConversationListEvent = {},
                onNavigate = {}
            )
        }
    }
}


private fun PaddingValues.calculateListBottomPadding(): Dp {
    val bottomPadding = calculateBottomPadding()
    val calculatedBottomPadding = if (bottomPadding > 0.dp) {
        bottomPadding - verticalFieldPadding
    } else {
        verticalFieldPadding
    }
    return calculatedBottomPadding.coerceAtLeast(0.dp) + floatingBarBottomPadding
}

