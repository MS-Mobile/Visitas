package com.msmobile.visitas.conversation

import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
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
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

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
        verifyBlocking(conversationRepository) { listByIdOrGroupId(FIRST_CONVERSATION_ID) }
        assertEquals(3, conversations.size)
        assertEquals(FIRST_CONVERSATION_ID, conversations[0].id)
        assertEquals(SECOND_CONVERSATION_ID, conversations[1].id)
        assertEquals(THIRD_CONVERSATION_ID, conversations[2].id)
    }

    @Test
    fun `hasDrafts is true when a loaded conversation is a draft`() {
        // Arrange
        val loaded = createConversationList().mapIndexed { index, conversation ->
            if (index == 1) conversation.copy(isDraft = true) else conversation
        }
        val viewModel = createViewModel(listByGroupResults = listOf(loaded))

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

        // Assert
        assertTrue(viewModel.uiState.value.hasDrafts)
        assertTrue(viewModel.uiState.value.conversationList[1].isDraft)
    }

    @Test
    fun `auto save marks an edited committed conversation as draft and snapshots it`() {
        // Arrange
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
        val second = viewModel.uiState.value.conversationList[1]

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.QuestionChanged(second, "Edited"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversations = viewModel.uiState.value.conversationList
        assertFalse(conversations[0].isDraft)
        assertTrue(conversations[1].isDraft)
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository) {
            save(ConversationSnapshot(createConversationList()[1]))
        }
    }

    @Test
    fun `auto save snapshots an edited conversation only once across passes`() {
        // Arrange
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

        // Act
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edit 1"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edit 2"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository, times(1)) { save(any()) }
    }

    @Test
    fun `auto save on a new group marks the parent draft, links children and sets no snapshot`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            snapshotRepositoryRef = snapshotRepositoryRef,
            committedConversations = emptyList()
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.AddClicked)
        val child = viewModel.uiState.value.conversationList[1]

        // Act: only the child changes
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.QuestionChanged(child, "Child"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversations = viewModel.uiState.value.conversationList
        assertTrue(conversations[0].isDraft)
        assertTrue(conversations[1].isDraft)
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) {
            save(argThat {
                id == NEW_CHILD_CONVERSATION_ID &&
                    conversationGroupId == NEW_CONVERSATION_ID &&
                    isDraft
            })
        }
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository, never()) { save(any()) }
    }

    @Test
    fun `delete cancels a pending auto save`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edited"
            )
        )

        // Act: delete before the 250 ms debounce elapses
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository, never()) { save(any()) }
    }

    @Test
    fun `delete of an auto-saved new group deletes the whole group`() {
        // Arrange: before this feature a new group had no id until Save. Auto-save gives it one, and
        // deleting only the parent would orphan the draft children.
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            committedConversations = emptyList()
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.AddClicked)
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Child"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { deleteGroup(NEW_CONVERSATION_ID) }
        verifyBlocking(conversationRepository, never()) { deleteById(any()) }
        assertEquals(
            ConversationDetailViewModel.UiEventState.Deleted,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `save finalizes drafts and deletes the group's snapshots`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            snapshotRepositoryRef = snapshotRepositoryRef
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edited"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.SaveClicked)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.hasDrafts)
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) {
            save(argThat { id == SECOND_CONVERSATION_ID && question == "Edited" && !isDraft })
        }
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository) { deleteByGroup(FIRST_CONVERSATION_ID) }
    }

    @Test
    fun `undo changes clicked asks for confirmation`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesClicked)

        // Assert
        assertEquals(
            ConversationDetailViewModel.UiEventState.UndoChangesConfirmation,
            viewModel.uiState.value.eventState
        )
    }

    @Test
    fun `undo changes confirmed restores edited rows from snapshot and deletes added rows`() {
        // Arrange
        val committed = createConversationList()
        val added = Conversation(
            id = ADDED_CONVERSATION_ID,
            question = "Added",
            response = "",
            orderIndex = 3,
            conversationGroupId = FIRST_CONVERSATION_ID,
            isDraft = true
        )
        val live = committed.mapIndexed { index, conversation ->
            if (index == 1) conversation.copy(question = "Edited", isDraft = true) else conversation
        } + added
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            snapshotRepositoryRef = snapshotRepositoryRef,
            listByGroupResults = listOf(live, live, committed),
            snapshots = listOf(ConversationSnapshot(committed[1]))
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { deleteBulk(listOf(ADDED_CONVERSATION_ID)) }
        verifyBlocking(conversationRepository) { save(committed[1]) }
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository) { deleteByGroup(FIRST_CONVERSATION_ID) }
        val state = viewModel.uiState.value
        assertEquals(3, state.conversationList.size)
        assertEquals("Question 2", state.conversationList[1].question)
        assertFalse(state.hasDrafts)
    }

    @Test
    fun `undo changes confirmed on a never-committed group deletes it and resets the form`() {
        // Arrange
        val draftParent = Conversation(
            id = NEW_CONVERSATION_ID,
            question = "Typed",
            response = "",
            orderIndex = 0,
            conversationGroupId = null,
            isDraft = true
        )
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            listByGroupResults = listOf(listOf(draftParent), emptyList()),
            committedConversations = emptyList()
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[0], "Typed"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { deleteBulk(listOf(NEW_CONVERSATION_ID)) }
        val state = viewModel.uiState.value
        assertEquals(1, state.conversationList.size)
        assertEquals("", state.conversationList[0].question)
        assertFalse(state.hasDrafts)
    }

    @Test
    fun `editing a row back to its original text does not auto save`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

        // Act
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edited"
            )
        )
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Question 2"
            )
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository, never()) { save(any()) }
        assertFalse(viewModel.uiState.value.hasDrafts)
    }

    @Test
    fun `undo changes confirmed without a group id makes no repository calls`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
        val viewModel = createViewModel(
            conversationRepositoryRef = conversationRepositoryRef,
            snapshotRepositoryRef = snapshotRepositoryRef
        )
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        verifyNoInteractions(requireNotNull(conversationRepositoryRef.value))
        verifyNoInteractions(requireNotNull(snapshotRepositoryRef.value))
    }

    @Test
    fun `save deletes rows marked as removed`() {
        // Arrange
        val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
        val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.RemoveConversationClicked(
                viewModel.uiState.value.conversationList[2]
            )
        )

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.SaveClicked)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val conversationRepository = requireNotNull(conversationRepositoryRef.value)
        verifyBlocking(conversationRepository) { deleteById(THIRD_CONVERSATION_ID) }
    }

    @Test
    fun `delete waits for an in-flight auto save before deleting the group`() {
        // Arrange: a Room write can't be interrupted, so the fake save finishes even when cancelled.
        val callLog = mutableListOf<String>()
        val viewModel = createViewModel(saveDurationMillis = 1_000, callLog = callLog)
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
        viewModel.onEvent(
            ConversationDetailViewModel.UiEvent.QuestionChanged(
                viewModel.uiState.value.conversationList[1], "Edited"
            )
        )
        // Past the 250 ms debounce: the auto-save pass is now inside its first save.
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(300)

        // Act
        viewModel.onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(callLog.contains("save"))
        assertEquals("deleteGroup", callLog.last())
    }

    private fun createViewModel(
        conversationRepositoryRef: MockReferenceHolder<ConversationRepository>? = null,
        snapshotRepositoryRef: MockReferenceHolder<ConversationSnapshotRepository>? = null,
        // Consecutive results of listByIdOrGroupId: [load, undo live rows, undo remaining rows].
        listByGroupResults: List<List<Conversation>> = listOf(createConversationList()),
        committedConversations: List<Conversation> = createConversationList(),
        snapshots: List<ConversationSnapshot> = emptyList(),
        // When set, each save takes this long and can't be cancelled midway, like a Room write.
        saveDurationMillis: Long = 0,
        // Records completed saves and group deletes, in completion order.
        callLog: MutableList<String>? = null
    ): ConversationDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val conversationRepository = mock<ConversationRepository> {
            on { listByIdOrGroupId(any()) }.doReturnConsecutively(listByGroupResults)
            on { getByIdOrNull(any()) } doAnswer { invocation ->
                val id = invocation.getArgument<UUID>(0)
                committedConversations.firstOrNull { conversation -> conversation.id == id }
            }
            if (callLog != null) {
                on { save(any()) } doSuspendableAnswer {
                    withContext(NonCancellable) {
                        delay(saveDurationMillis)
                        callLog.add("save")
                    }
                    Unit
                }
                on { deleteGroup(any()) } doSuspendableAnswer {
                    callLog.add("deleteGroup")
                    Unit
                }
            }
        }
        val snapshotRepository = mock<ConversationSnapshotRepository> {
            on { listByGroup(any()) } doReturn snapshots
        }
        val uuidProvider = mock<IdProvider> {
            on { generateId() }.doReturn(NEW_CONVERSATION_ID, NEW_CHILD_CONVERSATION_ID)
        }
        conversationRepositoryRef?.value = conversationRepository
        snapshotRepositoryRef?.value = snapshotRepository

        return ConversationDetailViewModel(
            dispatchers = dispatchers,
            conversationRepository = conversationRepository,
            snapshotRepository = snapshotRepository,
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
        private val NEW_CHILD_CONVERSATION_ID = UUID.fromString("9e8d7c6b-5a4f-4e3d-8c2b-1a0f9e8d7c6b")
        private val ADDED_CONVERSATION_ID = UUID.fromString("0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d")
    }
}