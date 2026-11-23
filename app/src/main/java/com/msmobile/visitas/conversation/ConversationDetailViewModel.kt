package com.msmobile.visitas.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val snapshotRepository: ConversationSnapshotRepository,
    private val uuidProvider: IdProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            conversationList = listOf(newConversation(0)),
            eventState = UiEventState.Idle
        )
    )
    val uiState: StateFlow<UiState> = _uiState
    @Volatile private var conversationGroupId: UUID? = null
    @Volatile private var initialEditableData: Map<UUID, EditableConversationData>? = null
    private var autoSaveJob: Job? = null

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> viewCreated(event.firstConversationId)
            is UiEvent.QuestionChanged -> questionChanged(event.conversation, event.value)
            is UiEvent.ResponseChanged -> responseChanged(event.conversation, event.value)
            is UiEvent.UndoChangesClicked -> undoChangesClicked()
            is UiEvent.UndoChangesConfirmed -> undoChangesConfirmed()
            is UiEvent.UndoChangesConfirmationDismissed -> undoChangesConfirmationDismissed()
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
        autoSaveJob?.cancel()
        newState { copy(eventState = UiEventState.Deleting) }
        viewModelScope.launch(dispatchers.io) {
            val id = conversationGroupId ?: return@launch
            conversationRepository.deleteGroup(id)
            conversationGroupId = null
            newState { copy(eventState = UiEventState.Deleted) }
        }
    }

    private fun deleteClicked() {
        newState { copy(eventState = UiEventState.DeleteConfirmation) }
    }

    private fun saveClicked() {
        newState { copy(eventState = UiEventState.Saving) }
        viewModelScope.launch(dispatchers.io) {
            deleteRemovedConversations(uiState.value.conversationList)   // cascades their snapshots
            val finalized = uiState.value.conversationList.map { conversation ->
                conversation.copy(isDraft = false)
            }
            addOrUpdateConversations(finalized)
            conversationGroupId?.let { groupId -> snapshotRepository.deleteByGroup(groupId) }
            newState { copy(conversationList = finalized, eventState = UiEventState.SaveComplete) }
            // Rebase so a debounced pass landing after the save doesn't re-dirty the committed rows.
            initialEditableData = _uiState.value.editableSnapshot()
        }
    }

    private fun undoChangesClicked() {
        newState { copy(eventState = UiEventState.UndoChangesConfirmation) }
    }

    private fun undoChangesConfirmationDismissed() {
        newState { copy(eventState = UiEventState.Idle) }
    }

    /**
     * One data path for both cases: delete draft rows that have no snapshot (added this session, or
     * the whole group if it was never committed), then restore the snapshots. Only the UI step
     * branches: nothing left means the group never existed, so reset to an empty form.
     */
    private fun undoChangesConfirmed() {
        newState { copy(eventState = UiEventState.Idle) }
        val groupId = conversationGroupId ?: return
        viewModelScope.launch(dispatchers.io) {
            val snapshots = snapshotRepository.listByGroup(groupId)
            val committedIds = snapshots.map { snapshot -> snapshot.conversation.id }.toSet()
            val addedIds = conversationRepository.listByIdOrGroupId(groupId)
                .filter { conversation -> conversation.isDraft && conversation.id !in committedIds }
                .map { conversation -> conversation.id }
            if (addedIds.isNotEmpty()) conversationRepository.deleteBulk(addedIds)
            snapshots.forEach { snapshot -> conversationRepository.save(snapshot.conversation) }
            snapshotRepository.deleteByGroup(groupId)

            val remaining = conversationRepository.listByIdOrGroupId(groupId)
            if (remaining.isEmpty()) {
                conversationGroupId = null
                newState { copy(conversationList = listOf(newConversation(0))) }
            } else {
                newState { copy(conversationList = remaining.map { conversation -> conversation.asState }) }
            }
            initialEditableData = _uiState.value.editableSnapshot()
        }
    }

    private fun viewCreated(id: UUID?) {
        if (autoSaveJob != null || conversationGroupId != null) return
        if (id == null) {
            initialEditableData = _uiState.value.editableSnapshot()
            startAutoSave()
            return
        }
        conversationGroupId = id
        viewModelScope.launch(dispatchers.io) {
            val conversations = conversationRepository.listByIdOrGroupId(id).map { conversation ->
                conversation.asState
            }
            newState { copy(conversationList = conversations) }
            initialEditableData = _uiState.value.editableSnapshot()
            startAutoSave()
        }
    }

    private fun startAutoSave() {
        if (autoSaveJob != null) return
        autoSaveJob = _uiState
            .debounce(250)
            .onEach { state ->
                val baseline = initialEditableData ?: return@onEach
                if (state.editableSnapshot() == baseline) return@onEach

                captureSnapshotsAndMarkDrafts(baseline)

                val updatedState = _uiState.value
                saveDraftSilently(updatedState)
                // Rebase from the marked state so the draft flags just set don't count as a change.
                initialEditableData = updatedState.editableSnapshot()
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    /**
     * Marks each dirtied row a draft, first snapshotting its committed DB row (once: rows already
     * draft are skipped). Row 0 of a never-persisted group is marked draft even when only a child
     * changed, so "every row draft with no snapshot" reliably means "never committed" on Undo.
     */
    private suspend fun captureSnapshotsAndMarkDrafts(baseline: Map<UUID, EditableConversationData>) {
        _uiState.value.conversationList.forEachIndexed { index, conversation ->
            if (conversation.isDraft || conversation.wasRemoved) return@forEachIndexed
            val isNewOrChanged = baseline[conversation.id] != conversation.editable
            if (!isNewOrChanged && index != 0) return@forEachIndexed

            val committed = conversationRepository.getByIdOrNull(conversation.id)
            when {
                committed == null -> markDraft(conversation.id)   // never persisted: nothing to snapshot
                !isNewOrChanged -> Unit                           // committed parent, untouched
                else -> {
                    if (!committed.isDraft) {
                        snapshotRepository.save(ConversationSnapshot(committed))
                    }
                    markDraft(conversation.id)
                }
            }
        }
    }

    private fun markDraft(conversationId: UUID) {
        newState {
            copy(conversationList = conversationList.map { conversation ->
                if (conversation.id == conversationId) conversation.copy(isDraft = true) else conversation
            })
        }
    }

    private suspend fun saveDraftSilently(state: UiState) {
        val groupId = state.conversationList.first().id
        state.conversationList.forEachIndexed { index, conversation ->
            if (conversation.wasRemoved) return@forEachIndexed
            conversationRepository.save(conversation.inGroup(index, groupId).asModel)
        }
        conversationGroupId = groupId
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
        val groupId = conversationList.first().id
        conversationList.forEachIndexed { index, conversation ->
            conversationRepository.save(conversation.inGroup(index, groupId).asModel)
        }
        conversationGroupId = groupId
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
        val wasRemoved: Boolean,
        val isDraft: Boolean = false
    )

    private fun List<ConversationState>.indexOfByOrderIndex(visit: ConversationState): Int {
        return indexOfFirst { visit.orderIndex == it.orderIndex }
    }

    private val ConversationState.editable: EditableConversationData
        get() = EditableConversationData(question = question, response = response, orderIndex = orderIndex)

    private fun UiState.editableSnapshot(): Map<UUID, EditableConversationData> =
        conversationList.associate { conversation -> conversation.id to conversation.editable }

    private fun ConversationState.inGroup(index: Int, groupId: UUID): ConversationState =
        if (index == 0) this else copy(conversationGroupId = groupId)

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
                wasRemoved = false,
                isDraft = isDraft
            )
        }

    private val ConversationState.asModel: Conversation
        get() {
            return Conversation(
                id = id,
                question = question,
                response = response,
                conversationGroupId = conversationGroupId,
                orderIndex = orderIndex,
                isDraft = isDraft
            )
        }

    private data class EditableConversationData(
        val question: String,
        val response: String,
        val orderIndex: Int
    )

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
        data object UndoChangesClicked : UiEvent()
        data object UndoChangesConfirmed : UiEvent()
        data object UndoChangesConfirmationDismissed : UiEvent()
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
        data object UndoChangesConfirmation : UiEventState()
    }

    data class UiState(
        val conversationList: List<ConversationState>,
        val eventState: UiEventState
    ) {
        val hasDrafts: Boolean
            get() = conversationList.any { conversation -> !conversation.wasRemoved && conversation.isDraft }
    }
}