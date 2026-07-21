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
internal class VisitDetailPreviewConfigProvider :
    PreviewParameterProvider<VisitDetailPreviewConfig> {

    override val values: Sequence<VisitDetailPreviewConfig> = sequenceOf(
        VisitDetailPreviewConfig(
            configName = "New Visit",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                householder = VisitDetailViewModel.HouseholderState(
                    id = UUID.randomUUID(),
                    editable = VisitDetailViewModel.EditableHouseholderData(
                        name = "",
                        address = "",
                        notes = "",
                    ),
                    showClearName = false,
                    showCopyData = true,
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
                    previewFirstVisitUiState
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Draft Visit",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState.copy(isDraft = true)
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
                    editable = previewVisitDetailUiState.householder.editable.copy(address = ""),
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
                        editable = previewFirstVisitUiState.editable.copy(isDone = true),
                        nextConversationSuggestion = previewConversationSuggestion,
                        showNextVisitSuggestion = true
                    ),
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
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        preferredDay = VisitPreferredDay.SATURDAY,
                        preferredTime = VisitPreferredTime.AFTERNOON
                    )
                ),
                visitList = listOf(
                    previewFirstVisitUiState.copy(
                        editable = previewFirstVisitUiState.editable.copy(isDone = false),
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
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        notes = "Receptive householder, prefers morning visits. Interested in studying the Bible.",
                    ),
                    showClearNotes = true,
                    isNotesExpanded = true
                ),
                visitList = listOf(previewFirstVisitUiState)
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Delete Button",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(previewFirstVisitUiState),
                showDeleteButton = true
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "No Address Found Snackbar",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(previewFirstVisitUiState),
                eventState = VisitDetailViewModel.UiEventState.NoAddressFound
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Copied to Clipboard Snackbar",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(previewFirstVisitUiState),
                eventState = VisitDetailViewModel.UiEventState.CopiedToClipboard
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Notes Overflow Collapsed",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        notes = "Receptive householder, prefers morning visits. Interested in studying the Bible. Asked us to return next week to talk more about the resurrection hope and the promise of a paradise earth.",
                    ),
                    showClearNotes = false,
                    isNotesExpanded = false
                ),
                visitList = listOf(previewFirstVisitUiState)
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Notes Overflow Expanded",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        notes = "Receptive householder, prefers morning visits. Interested in studying the Bible. Asked us to return next week to talk more about the resurrection hope and the promise of a paradise earth.",
                    ),
                    showClearNotes = false,
                    isNotesExpanded = true
                ),
                visitList = listOf(previewFirstVisitUiState)
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Phone Number Input",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        phoneNumber = ""
                    )
                ),
                visitList = listOf(previewFirstVisitUiState),
                showPhoneInputDialog = true
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Phone Options",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                householder = previewVisitDetailUiState.householder.copy(
                    editable = previewVisitDetailUiState.householder.editable.copy(
                        phoneNumber = "+55 11 99999-0000"
                    )
                ),
                visitList = listOf(previewFirstVisitUiState),
                showPhoneOptionsSheet = true
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Conversation contains URL",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState.copy(
                        editable = previewFirstVisitUiState.editable.copy(
                            subject = "Publishing: [What is God's Kingdom?](https://www.jw.org/en/bible-teachings/kingdom/)"
                        )
                    )
                )
            ),
            isDarkMode = false
        ),
        VisitDetailPreviewConfig(
            configName = "Expanded visit date picker",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState
                ),
                eventState = VisitDetailViewModel.UiEventState.VisitDateExpanded(previewFirstVisitUiState)
            ),
            isDarkMode = false,
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
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

private val previewNewVisitUiState = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    editable = VisitDetailViewModel.EditableVisitData(
        subject = "",
        date = previewDate1,
        isDone = false,
        orderIndex = 0,
        visitType = VisitDetailViewModel.VisitTypeState(
            type = VisitType.FIRST_VISIT,
            description = StringResource(
                textResId = R.string.first_visit,
                arguments = listOf()
            )
        )
    ),
    householderId = UUID.randomUUID(),
    canBeRemoved = false,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0,
    isDraft = false,
)

private val previewFirstVisitUiState = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    editable = VisitDetailViewModel.EditableVisitData(
        subject = "What is God's Kingdom?",
        date = previewDate1,
        isDone = true,
        orderIndex = 0,
        visitType = VisitDetailViewModel.VisitTypeState(
            type = VisitType.FIRST_VISIT,
            description = StringResource(
                textResId = R.string.first_visit,
                arguments = listOf()
            )
        )
    ),
    householderId = UUID.randomUUID(),
    canBeRemoved = false,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0,
    isDraft = false,
)

private val previewReturnVisit = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    editable = VisitDetailViewModel.EditableVisitData(
        subject = "Who is the King of God's Kingdom?",
        date = previewDate2,
        isDone = false,
        orderIndex = 0,
        visitType = VisitDetailViewModel.VisitTypeState(
            type = VisitType.RETURN_VISIT,
            description = StringResource(
                textResId = R.string.return_visit,
                arguments = listOf()
            )
        )
    ),
    householderId = UUID.randomUUID(),
    canBeRemoved = true,
    isConversationListExpanded = false,
    isVisitTypeListExpanded = false,
    nextConversationSuggestion = null,
    showNextVisitSuggestion = false,
    showClearSubject = false,
    wasRemoved = false,
    caretPosition = 0,
    isDraft = false,
)

private val previewConversationSuggestion = VisitDetailViewModel.ConversationState(
    id = UUID.randomUUID(),
    question = previewReturnVisit.subject,
    response = "Lucas 1:31-33",
    show = true,
    conversationGroupId = UUID.randomUUID(),
    orderIndex = 0,
)

private val previewVisitDetailUiState = VisitDetailViewModel.UiState(
    householder = VisitDetailViewModel.HouseholderState(
        id = UUID.randomUUID(),
        editable = VisitDetailViewModel.EditableHouseholderData(
            name = "Pedro",
            address = "Rua 1",
            notes = "Receptive householder",
        ),
        showClearName = true,
        showCopyData = false,
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
