package com.msmobile.visitas.conversation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.conversation.ConversationDetailViewModel.ConversationState
import com.msmobile.visitas.util.IntentState
import java.util.UUID

class ConversationDetailPreviewConfigProvider : PreviewParameterProvider<ConversationDetailPreviewConfig> {
    override val values: Sequence<ConversationDetailPreviewConfig> = sequenceOf(
        ConversationDetailPreviewConfig(
            configName = "With Delete Button",
            mainActivityUiState = previewMainActivityUiState,
            showDeleteButton = true,
            uiState = previewConversationDetailUiState
        ),
        ConversationDetailPreviewConfig(
            configName = "Without Delete Button",
            mainActivityUiState = previewMainActivityUiState,
            showDeleteButton = false,
            uiState = previewConversationDetailUiState
        ),
        ConversationDetailPreviewConfig(
            configName = "Single Conversation",
            mainActivityUiState = previewMainActivityUiState,
            showDeleteButton = true,
            uiState = previewConversationDetailUiState.copy(
                conversationList = listOf(
                    previewConversationDetailUiState.conversationList.first()
                )
            )
        ),
        ConversationDetailPreviewConfig(
            configName = "Multiple Conversations",
            mainActivityUiState = previewMainActivityUiState,
            showDeleteButton = true,
            uiState = previewConversationDetailUiState.copy(
                conversationList = previewConversationDetailUiState.conversationList + listOf(
                    ConversationState(
                        id = UUID.randomUUID(),
                        question = "Where do you live?",
                        response = "I live in New York",
                        showQuestionClear = false,
                        showResponseClear = false,
                        orderIndex = 2,
                        conversationGroupId = UUID.randomUUID(),
                        canBeDeleted = true,
                        wasRemoved = false
                    )
                )
            )
        ),
    )

    override fun getDisplayName(index: Int): String? {
        return values.elementAt(index).configName
    }
}

data class ConversationDetailPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val showDeleteButton: Boolean,
    val uiState: ConversationDetailViewModel.UiState,
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    scaffoldState = MainActivityViewModel.ScaffoldState(
        showBottomBar = false,
        showFAB = false
    ),
    eventState = MainActivityViewModel.UiEventState.Idle,
    isTimerRunning = false,
    intentState = IntentState.None
)

private val previewConversationDetailUiState = ConversationDetailViewModel.UiState(
    conversationList = listOf(
        ConversationState(
            id = UUID.randomUUID(),
            question = "What's your name?",
            response = "My name is John",
            showQuestionClear = true,
            showResponseClear = false,
            orderIndex = 0,
            conversationGroupId = UUID.randomUUID(),
            canBeDeleted = false,
            wasRemoved = false
        ),
        ConversationState(
            id = UUID.randomUUID(),
            question = "What's your age?",
            response = "I'm 25 years old",
            showQuestionClear = false,
            showResponseClear = false,
            orderIndex = 1,
            conversationGroupId = UUID.randomUUID(),
            canBeDeleted = true,
            wasRemoved = false
        )
    ),
    eventState = ConversationDetailViewModel.UiEventState.Idle
)

