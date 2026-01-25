package com.msmobile.visitas.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConversationDetailViewModel
@Inject
constructor(
    private val dispatchers: DispatcherProvider,
    private val conversationRepository: ConversationRepository,
    private val uuidProvider: IdProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            conversationList = listOf(newConversation(0)),
            eventState = UiEventState.Idle
        )
    )
    val uiState: StateFlow<UiState> = _uiState
    private var conversationGroupId: UUID? = null

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> viewCreated(event.firstConversationId)
            is UiEvent.QuestionChanged -> questionChanged(event.conversation, event.value)
            is UiEvent.ResponseChanged -> responseChanged(event.conversation, event.value)
            is UiEvent.CancelClicked -> cancelClicked()
            is UiEvent.SaveClicked -> saveClicked()
            is UiEvent.DeleteClicked -> deleteClicked()
            is UiEvent.DeleteAccepted -> deleteAccepted()
            is UiEvent.DeleteDismissed -> deleteDismissed()
            is UiEvent.AddClicked -> addClicked()
            is UiEvent.RemoveConversationClicked -> removeConversationClicked(event.conversation)
            is UiEvent.QuestionFocusChanged -> questionFocusChanged(
                event.hasFocus,
                event.conversation
            )

            is UiEvent.ResponseFocusChanged -> responseFocusChanged(
                event.hasFocus,
                event.conversation
            )

            is UiEvent.ClearQuestionClicked -> clearQuestionClicked(event.conversation)
            is UiEvent.ClearResponseClicked -> clearResponseClicked(event.conversation)
        }
    }

    private fun clearResponseClicked(conversation: ConversationState) {
        responseChanged(conversation = conversation, value = "")
    }

    private fun clearQuestionClicked(conversation: ConversationState) {
        questionChanged(conversation = conversation, value = "")
    }

    private fun questionFocusChanged(
        hasFocus: Boolean,
        conversation: ConversationState
    ) {
        newState {
            val showQuestionClear = conversation.question.isNotEmpty() && hasFocus
            val updatedList = conversationList.toMutableList().apply {
                set(
                    indexOfByOrderIndex(conversation),
                    conversation.copy(showQuestionClear = showQuestionClear)
                )
            }
            copy(conversationList = updatedList)
        }
    }

    private fun responseFocusChanged(
        hasFocus: Boolean,
        conversation: ConversationState
    ) {
        newState {
            val showResponseClear = conversation.response.isNotEmpty() && hasFocus
            val updatedList = conversationList.toMutableList().apply {
                set(
                    indexOfByOrderIndex(conversation),
                    conversation.copy(showResponseClear = showResponseClear)
                )
            }
            copy(conversationList = updatedList)
        }
    }

    private fun removeConversationClicked(conversation: ConversationState) {
        newState {
            val updatedList = conversationList.toMutableList().apply {
                set(
                    index = indexOfByOrderIndex(conversation),
                    element = conversation.copy(wasRemoved = true)
                )
            }
            copy(conversationList = updatedList)
        }
    }

    private fun addClicked() {
        newState {
            val conversationOrder = conversationList.lastOrNull()?.orderIndex?.plus(1) ?: 0
            val newConversation = newConversation(conversationOrder)
            val newList = conversationList.toMutableList().apply {
                add(newConversation)
            }
            copy(conversationList = newList)
        }
    }

    private fun deleteDismissed() {
        newState { copy(eventState = UiEventState.Idle) }
    }

    private fun deleteAccepted() {
        newState { copy(eventState = UiEventState.Deleting) }
        viewModelScope.launch(dispatchers.io) {
            val id = conversationGroupId ?: return@launch
            conversationRepository.deleteById(id)
            conversationGroupId = null
            newState { copy(eventState = UiEventState.Deleted) }
        }
    }

    private fun deleteClicked() {
        newState { copy(eventState = UiEventState.DeleteConfirmation) }
    }

    private fun saveClicked() {
        newState {
            copy(eventState = UiEventState.Saving)
        }
        viewModelScope.launch(dispatchers.io) {
            deleteRemovedConversations(uiState.value.conversationList)
            addOrUpdateConversations(uiState.value.conversationList)
            newState {
                copy(eventState = UiEventState.SaveComplete)
            }
        }
    }

    private fun cancelClicked() {
        conversationGroupId = null
        newState { copy(eventState = UiEventState.Canceled) }
    }

    private fun viewCreated(id: UUID?) {
        if (conversationGroupId != null) return
        if (id != null) {
            conversationGroupId = id
            viewModelScope.launch(dispatchers.io) {
                val conversations =
                    conversationRepository.listByIdOrGroupId(id).map { conversation ->
                        conversation.asState
                    }
                newState { copy(conversationList = conversations) }
            }
        }
    }

    private fun newConversation(orderIndex: Int): ConversationState {
        return ConversationState(
            id = uuidProvider.generateId(),
            question = "",
            response = "",
            orderIndex = orderIndex,
            conversationGroupId = null,
            showQuestionClear = false,
            showResponseClear = false,
            canBeDeleted = canDeleteConversation(orderIndex),
            wasRemoved = false
        )
    }

    private fun questionChanged(conversation: ConversationState, value: String) {
        newState {
            val showQuestionClear = value.isNotEmpty()
            val updatedList = conversationList.toMutableList().apply {
                set(
                    indexOfByOrderIndex(conversation),
                    conversation.copy(question = value, showQuestionClear = showQuestionClear)
                )
            }
            copy(conversationList = updatedList)
        }
    }

    private fun responseChanged(conversation: ConversationState, value: String) {
        newState {
            val showResponseClear = value.isNotEmpty()
            val updatedList = conversationList.toMutableList().apply {
                set(
                    indexOfByOrderIndex(conversation),
                    conversation.copy(response = value, showResponseClear = showResponseClear)
                )
            }
            copy(conversationList = updatedList)
        }
    }

    private suspend fun deleteRemovedConversations(conversationList: List<ConversationState>) {
        val removedConversations = conversationList.filter { it.wasRemoved }
        removedConversations.forEach { conversation ->
            conversationRepository.deleteById(conversation.id)
        }
        newState {
            val updatedList = conversationList.toMutableList().apply {
                removeAll(removedConversations)
            }
            copy(conversationList = updatedList)
        }
    }

    private suspend fun addOrUpdateConversations(conversationList: List<ConversationState>) {
        conversationList.forEachIndexed { index, conversation ->
            val newConversation = if (index == 0) {
                conversation
            } else {
                conversation.copy(conversationGroupId = conversationGroupId)
            }
            val newConversationModel = newConversation.asModel
            conversationRepository.save(newConversationModel)
            if (index == 0) {
                conversationGroupId = newConversationModel.id
            }
        }
    }

    private fun newState(value: UiState.() -> UiState) {
        _uiState.update(value)
    }

    private fun canDeleteConversation(orderIndex: Int): Boolean {
        return orderIndex > 0
    }

    data class ConversationState(
        val id: UUID,
        val question: String,
        val response: String,
        val orderIndex: Int,
        val conversationGroupId: UUID?,
        val showQuestionClear: Boolean,
        val showResponseClear: Boolean,
        val canBeDeleted: Boolean,
        val wasRemoved: Boolean
    )

    private fun List<ConversationState>.indexOfByOrderIndex(visit: ConversationState): Int {
        return indexOfFirst { visit.orderIndex == it.orderIndex }
    }

    private val Conversation.asState: ConversationState
        get() {
            return ConversationState(
                id = id,
                question = question,
                response = response,
                orderIndex = orderIndex,
                conversationGroupId = conversationGroupId,
                showQuestionClear = false,
                showResponseClear = false,
                canBeDeleted = canDeleteConversation(orderIndex),
                wasRemoved = false
            )
        }

    private val ConversationState.asModel: Conversation
        get() {
            return Conversation(
                id = id,
                question = question,
                response = response,
                conversationGroupId = conversationGroupId,
                orderIndex = orderIndex
            )
        }

    sealed class UiEvent {
        data class QuestionChanged(val conversation: ConversationState, val value: String) :
            UiEvent()

        data class ResponseChanged(val conversation: ConversationState, val value: String) :
            UiEvent()

        data class ViewCreated(val firstConversationId: UUID?) : UiEvent()
        data class RemoveConversationClicked(val conversation: ConversationState) : UiEvent()
        data class QuestionFocusChanged(
            val hasFocus: Boolean,
            val conversation: ConversationState
        ) : UiEvent()

        data class ResponseFocusChanged(
            val hasFocus: Boolean,
            val conversation: ConversationState
        ) : UiEvent()

        data class ClearQuestionClicked(val conversation: ConversationState) : UiEvent()
        data class ClearResponseClicked(val conversation: ConversationState) : UiEvent()

        data object AddClicked : UiEvent()
        data object SaveClicked : UiEvent()
        data object CancelClicked : UiEvent()
        data object DeleteClicked : UiEvent()
        data object DeleteAccepted : UiEvent()
        data object DeleteDismissed : UiEvent()
    }

    sealed class UiEventState {
        data object Idle : UiEventState()
        data object DeleteConfirmation : UiEventState()
        data object Deleting : UiEventState()
        data object Deleted : UiEventState()
        data object Saving : UiEventState()
        data object SaveComplete : UiEventState()
        data object Canceled : UiEventState()
    }

    data class UiState(
        val conversationList: List<ConversationState>,
        val eventState: UiEventState
    )
}