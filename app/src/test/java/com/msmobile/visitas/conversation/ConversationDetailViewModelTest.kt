package com.msmobile.visitas.conversation

import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.util.runSuspend
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.UUID

class ConversationDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEvent with ViewCreated event does nothing when firstConversationId is null`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef
        )

        // Act
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.ViewCreated(
                firstConversationId = null
            )
        )

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyNoInteractions(conversationRepository)
    }

    @Test
    fun `onEvent with ViewCreated event loads conversations when firstConversationId is valid`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef
        )

        // Act
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.ViewCreated(
                firstConversationId = FIRST_CONVERSATION_ID
            )
        )

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        val conversations = viewModel.uiState.value.conversationList
        verify(conversationRepository).runSuspend { listByIdOrGroupId(FIRST_CONVERSATION_ID) }
        assertEquals(3, conversations.size)
        assertEquals(FIRST_CONVERSATION_ID, conversations[0].id)
        assertEquals(SECOND_CONVERSATION_ID, conversations[1].id)
        assertEquals(THIRD_CONVERSATION_ID, conversations[2].id)
    }

    private fun createViewModel(
        conversationRepositoryRef: MockReferenceHolder<ConversationRepository>? = null
    ): ConversationDetailViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val conversationRepository = mock<ConversationRepository> {
            on { listByIdOrGroupId(any()) } doReturn createConversationList()
        }
        val uuidProvider = mock<IdProvider> {
            on { generateId() } doReturn NEW_CONVERSATION_ID
        }

        conversationRepositoryRef?.value = conversationRepository

        return ConversationDetailViewModel(
            dispatchers = dispatchers,
            conversationRepository = conversationRepository,
            uuidProvider = uuidProvider
        )
    }

    private fun createConversationList(): List<Conversation> {
        val conversationList = listOf(
            Conversation(
                id = FIRST_CONVERSATION_ID,
                question = "Question 1",
                response = "Response 1",
                conversationGroupId = null,
                orderIndex = 0
            ),
            Conversation(
                id = SECOND_CONVERSATION_ID,
                question = "Question 2",
                response = "Response 2",
                conversationGroupId = FIRST_CONVERSATION_ID,
                orderIndex = 1
            ),
            Conversation(
                id = THIRD_CONVERSATION_ID,
                question = "Question 3",
                response = "Response 3",
                conversationGroupId = FIRST_CONVERSATION_ID,
                orderIndex = 2
            )
        )
        return conversationList
    }

    companion object {
        private val FIRST_CONVERSATION_ID = UUID.fromString("3f2b7d9a-8c4e-4e2a-9b1d-5c6a7f8e1a23")
        private val SECOND_CONVERSATION_ID = UUID.fromString("c1a9f7b4-2e3d-4f5a-8b6c-0d1e2f3a4b5c")
        private val THIRD_CONVERSATION_ID = UUID.fromString("7a4e1c9b-6d2f-4a3e-8b5c-0f9d1e2a3c4b")
        private val NEW_CONVERSATION_ID = UUID.fromString("5c4b3a2f-1e9d-7c6b-4a3e-8b5c0f9d1e2a")
    }
}