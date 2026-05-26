package com.msmobile.visitas.conversation

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.conversation.ConversationDetailViewModel.ConversationState
import com.msmobile.visitas.util.IntentState
import java.util.UUID

@VisibleForTesting
internal class ConversationDetailPreviewConfigProvider :
    PreviewParameterProvider<ConversationDetailPreviewConfig> {

    override val values: Sequence<ConversationDetailPreviewConfig> =  sequenceOf(
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
                        question = "What does the Bible say about the resurrection?",
                        response = "The dead will be resurrected — John 5:28, 29",
                        showQuestionClear = false,
                        showResponseClear = false,
                        orderIndex = 2,
                        conversationGroupId = UUID.randomUUID(),
                        canBeDeleted = true,
                        wasRemoved = false
                    )
                )
            )
        )
    )

    override fun getDisplayName(index: Int): String? {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class ConversationDetailPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val showDeleteButton: Boolean,
    val uiState: ConversationDetailViewModel.UiState
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

private val previewConversationDetailUiState = ConversationDetailViewModel.UiState(
    conversationList = listOf(
        ConversationState(
            id = UUID.randomUUID(),
            question = "What is God's Kingdom?",
            response = "It is God's heavenly government through Jesus Christ — Daniel 2:44",
            showQuestionClear = true,
            showResponseClear = false,
            orderIndex = 0,
            conversationGroupId = UUID.randomUUID(),
            canBeDeleted = false,
            wasRemoved = false
        ),
        ConversationState(
            id = UUID.randomUUID(),
            question = "Who is the King of God's Kingdom?",
            response = "Jesus Christ, the Son of God — Luke 1:31-33",
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

