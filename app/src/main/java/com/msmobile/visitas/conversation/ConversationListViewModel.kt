package com.msmobile.visitas.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.extension.containsAllWords
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.visit.VisitListViewModel.VisitDistanceFilter
import com.msmobile.visitas.visit.VisitListViewModel.VisitHouseholderState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel
@Inject
constructor(
    private val dispatchers: DispatcherProvider,
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            conversations = emptyList(),
            filter = ConversationFilter(search = "")
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> loadConversations()
            is UiEvent.SearchChanged -> searchChanged(event.search)
            is UiEvent.FilterCleared -> filterCleared()
        }
    }

    private fun loadConversations() {
        viewModelScope.launch(dispatchers.io) {
            val conversations = conversationRepository.listAll()
            val conversationStates = conversations.map { conversation ->
                val parentId = conversation.conversationGroupId ?: conversation.id
                ConversationState(
                    conversationId = conversation.id,
                    parentId = parentId,
                    question = conversation.question,
                    response = conversation.response
                )
            }
            newState {
                copy(conversations = conversationStates)
            }
        }
    }

    private fun searchChanged(value: String) {
        newState {
            val filter = filter.copy(search = value)
            val filteredConversationList = conversations.filterBy(filter)
            copy(
                conversations = filteredConversationList,
                filter = filter
            )
        }
    }

    private fun filterCleared() {
        searchChanged("")
    }

    private fun newState(value: UiState.() -> UiState) {
        _uiState.update(value)
    }

    private fun List<ConversationState>.filterBy(filter: ConversationFilter): List<ConversationState> {
        return map { conversation ->
            val (search) = filter
            val matchesQuestion = conversation.question.containsAllWords(search)
            val matchesResponse = conversation.response.containsAllWords(filter.search)
            val isSearchEmpty = search.isEmpty()
            val show = isSearchEmpty || matchesQuestion || matchesResponse
            conversation.copy(hide = !show)
        }
    }

    sealed class UiEvent {
        data object ViewCreated : UiEvent()
        data class SearchChanged(val search: String) : UiEvent()
        data object FilterCleared : UiEvent()
    }

    data class ConversationFilter(
        val search: String
    )

    data class ConversationState(
        val conversationId: UUID,
        val parentId: UUID? = null,
        val question: String = "",
        val response: String = "",
        val hide: Boolean = false
    )

    data class UiState(
        val conversations: List<ConversationState>,
        val filter: ConversationFilter,
    )
}