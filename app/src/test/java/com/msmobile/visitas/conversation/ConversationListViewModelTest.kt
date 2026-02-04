package com.msmobile.visitas.conversation

import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import java.util.UUID

class ConversationListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.conversations.isEmpty())
        assertEquals("", state.filter.search)
    }

    @Test
    fun `onEvent with ViewCreated loads conversations from repository`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { listAll() }
        val conversations = viewModel.uiState.value.conversations
        assertEquals(3, conversations.size)
        assertEquals(FIRST_CONVERSATION_ID, conversations[0].conversationId)
        assertEquals(SECOND_CONVERSATION_ID, conversations[1].conversationId)
        assertEquals(THIRD_CONVERSATION_ID, conversations[2].conversationId)
    }

    @Test
    fun `onEvent with ViewCreated sets correct parentId for conversations`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

        // Assert
        val conversations = viewModel.uiState.value.conversations
        // First conversation has no group, so parentId equals its own id
        assertEquals(FIRST_CONVERSATION_ID, conversations[0].parentId)
        // Second and third have conversationGroupId pointing to first
        assertEquals(FIRST_CONVERSATION_ID, conversations[1].parentId)
        assertEquals(FIRST_CONVERSATION_ID, conversations[2].parentId)
    }

    @Test
    fun `onEvent with SearchChanged filters conversations by question`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("Question 1"))

        // Assert
        val state = viewModel.uiState.value
        assertEquals("Question 1", state.filter.search)
        val visibleConversations = state.conversations.filter { !it.hide }
        assertEquals(1, visibleConversations.size)
        assertEquals(FIRST_CONVERSATION_ID, visibleConversations[0].conversationId)
    }

    @Test
    fun `onEvent with SearchChanged filters conversations by response`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("Response 2"))

        // Assert
        val state = viewModel.uiState.value
        val visibleConversations = state.conversations.filter { !it.hide }
        assertEquals(1, visibleConversations.size)
        assertEquals(SECOND_CONVERSATION_ID, visibleConversations[0].conversationId)
    }

    @Test
    fun `onEvent with SearchChanged with empty string shows all conversations`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("Question 1"))

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged(""))

        // Assert
        val state = viewModel.uiState.value
        val visibleConversations = state.conversations.filter { !it.hide }
        assertEquals(3, visibleConversations.size)
    }

    @Test
    fun `onEvent with FilterCleared clears the search filter`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("Question 1"))

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.FilterCleared)

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.filter.search)
        val visibleConversations = state.conversations.filter { !it.hide }
        assertEquals(3, visibleConversations.size)
    }

    @Test
    fun `onEvent with SearchChanged with non-matching text hides all conversations`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

        // Act
        viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("NonExistent"))

        // Assert
        val state = viewModel.uiState.value
        val visibleConversations = state.conversations.filter { !it.hide }
        assertTrue(visibleConversations.isEmpty())
    }

    private fun createViewModel(
        conversationRepositoryRef: MockReferenceHolder<ConversationRepository>? = null
    ): ConversationListViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val conversationRepository = mock<ConversationRepository> {
            onBlocking { listAll() } doReturn createConversationList()
        }
        conversationRepositoryRef?.value = conversationRepository

        return ConversationListViewModel(
            dispatchers = dispatchers,
            conversationRepository = conversationRepository
        )
    }

    private fun createConversationList(): List<Conversation> {
        return listOf(
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
    }

    companion object {
        private val FIRST_CONVERSATION_ID = UUID.fromString("3f2b7d9a-8c4e-4e2a-9b1d-5c6a7f8e1a23")
        private val SECOND_CONVERSATION_ID = UUID.fromString("c1a9f7b4-2e3d-4f5a-8b6c-0d1e2f3a4b5c")
        private val THIRD_CONVERSATION_ID = UUID.fromString("7a4e1c9b-6d2f-4a3e-8b5c-0f9d1e2a3c4b")
    }
}
