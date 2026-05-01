package com.msmobile.visitas.visit

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.R
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.StringResource
import java.time.LocalDateTime
import java.util.UUID

private val previewDate1 = LocalDateTime.of(2024, 1, 15, 10, 12)
private val previewDate2 = previewDate1.plusWeeks(1)

@VisibleForTesting
internal class VisitDetailPreviewConfigProvider : PreviewParameterProvider<VisitDetailPreviewConfig> {

    override val values: Sequence<VisitDetailPreviewConfig> = sequenceOf(
        VisitDetailPreviewConfig(
            configName = "New Visit",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                householder = VisitDetailViewModel.HouseholderState(
                    id = UUID.randomUUID(),
                    name = "",
                    address = "",
                    notes = "",
                    showClearName = false,
                    addressState = VisitDetailViewModel.HouseholderAddressState.LoadLocation,
                    showClearNotes = false,
                    isLoadingAddress = false,
                    isNotesExpanded = false,
                ),
                visitList = listOf(
                    previewNewVisitUiState
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Edit Visit",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState.copy(canBeRemoved = false)
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Loading Address",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    address = "",
                    isLoadingAddress = true
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Multiple Visits",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewReturnVisit,
                    previewFirstVisitUiState
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Next Visit Suggestion",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState.copy(
                        isDone = true,
                        canBeRemoved = false,
                        nextConversationSuggestion = previewConversationSuggestion,
                        showNextVisitSuggestion = true
                    ).copy(),
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Time Preference Error",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    preferredDay = VisitPreferredDay.SATURDAY,
                    preferredTime = VisitPreferredTime.AFTERNOON
                ),
                visitList = listOf(
                    previewFirstVisitUiState.copy(
                        isDone = false,
                        canBeRemoved = false,
                        hasVisitTimeError = true
                    )
                ),
                eventState = VisitDetailViewModel.UiEventState.ValidationError
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Notes Focused",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    notes = "Receptive householder, prefers morning visits. Interested in studying the Bible.",
                    showClearNotes = true,
                    isNotesExpanded = true
                ),
                visitList = listOf(previewFirstVisitUiState.copy(canBeRemoved = false))
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Notes Expanded",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    notes = "Receptive householder, prefers morning visits. Interested in studying the Bible.",
                    showClearNotes = false,
                    isNotesExpanded = true
                ),
                visitList = listOf(previewFirstVisitUiState.copy(canBeRemoved = false))
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Delete Button",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(previewFirstVisitUiState.copy(canBeRemoved = false)),
                showDeleteButton = true
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Notes Collapsed",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    notes = "Receptive householder, prefers morning visits.\nInterested in studying the Bible.",
                    showClearNotes = false,
                    isNotesExpanded = false
                ),
                visitList = listOf(previewFirstVisitUiState.copy(canBeRemoved = false))
            ),
            isDarkMode = false
        ),
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class VisitDetailPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val householderId: UUID?,
    val uiState: VisitDetailViewModel.UiState,
    val isDarkMode: Boolean
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    scaffoldState = MainActivityViewModel.ScaffoldState(
        showBottomBar = false,
        showFAB = false
    ),
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

private val previewNewVisitUiState = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    subject = "",
    date = previewDate1,
    isDone = false,
    householderId = UUID.randomUUID(),
    canBeRemoved = false,
    orderIndex = 0,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    visitType =  VisitDetailViewModel.VisitTypeState(
        type = VisitType.FIRST_VISIT,
        description = StringResource(
            textResId = R.string.first_visit,
            arguments = listOf()
        )
    ),
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0
)

private val previewFirstVisitUiState = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    subject = "What is God's Kingdom?",
    date = previewDate1,
    isDone = true,
    householderId = UUID.randomUUID(),
    canBeRemoved = true,
    orderIndex = 0,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    visitType = VisitDetailViewModel.VisitTypeState(
        type = VisitType.FIRST_VISIT,
        description = StringResource(
            textResId = R.string.first_visit,
            arguments = listOf()
        )
    ),
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0
)

private val previewReturnVisit = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    subject = "Who is the King of God's Kingdom?",
    date = previewDate2,
    isDone = false,
    householderId = UUID.randomUUID(),
    canBeRemoved = false,
    orderIndex = 0,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    visitType = VisitDetailViewModel.VisitTypeState(
        type = VisitType.RETURN_VISIT,
        description = StringResource(
            textResId = R.string.return_visit,
            arguments = listOf()
        )
    ),
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0
)

private val previewConversationSuggestion = VisitDetailViewModel.ConversationState(
    id = UUID.randomUUID(),
    question = previewReturnVisit.subject,
    questionAndResponse = "${previewReturnVisit.subject} - Lucas 1:31-33",
    show = true,
    conversationGroupId = UUID.randomUUID(),
    orderIndex = 0,
)

private val previewVisitDetailUiState = VisitDetailViewModel.UiState(
    householder = VisitDetailViewModel.HouseholderState(
        id = UUID.randomUUID(),
    name = "Pedro",
    address = "Rua 1",
    notes = "Receptive householder",
        showClearName = true,
        addressState = VisitDetailViewModel.HouseholderAddressState.LoadLocation,
        showClearNotes = false,
        isLoadingAddress = false,
        isNotesExpanded = false,
    ),
    visitList = listOf(previewFirstVisitUiState),
    conversationList = listOf(),
    visitTypeList = listOf(),
    eventState = VisitDetailViewModel.UiEventState.Idle,
    showDeleteButton = false
)
