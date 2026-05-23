package com.msmobile.visitas.conversation

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.util.IntentState
import java.util.UUID

@VisibleForTesting
internal class PreviewConfigProvider : PreviewParameterProvider<PreviewConfig> {
    override val values: Sequence<PreviewConfig> = sequenceOf(
        PreviewConfig(
            configName = "Conversation list",
            mainActivityUiState = previewMainActivityUiState,
            conversationUiState = previewConversationUiState
        ),
        PreviewConfig(
            configName = "Empty list",
            mainActivityUiState = previewMainActivityUiState,
            conversationUiState = previewConversationUiState.copy(
                conversations = emptyList()
            )
        ),
        PreviewConfig(
            configName = "Filtering conversations",
            mainActivityUiState = previewMainActivityUiState,
            conversationUiState = previewConversationUiState.copy(
                filter = previewConversationUiState.filter.copy(search = "God")
            )
        )
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class PreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val conversationUiState: ConversationListViewModel.UiState
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    scaffoldState = MainActivityViewModel.ScaffoldState(
        showTopBar = true,
        showBottomBar = true,
        showFAB = true,
        title = "Conversations"
    ),
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

private val previewConversationUiState = ConversationListViewModel.UiState(
    conversations = listOf(
        ConversationListViewModel.ConversationState(
            conversationId = UUID.randomUUID(),
            parentId = UUID.randomUUID(),
            question = "What will God's Kingdom do for us?",
            hide = false
        ),
        ConversationListViewModel.ConversationState(
            conversationId = UUID.randomUUID(),
            parentId = UUID.randomUUID(),
            question = "Does God hear all prayers?",
            hide = false
        )
    ),
    filter = ConversationListViewModel.ConversationFilter(search = "")
)

