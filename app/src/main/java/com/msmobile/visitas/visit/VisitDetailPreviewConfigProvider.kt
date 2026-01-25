package com.msmobile.visitas.visit

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.R
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.StringResource
import java.time.LocalDateTime
import java.util.UUID

class VisitDetailPreviewConfigProvider : PreviewParameterProvider<VisitDetailPreviewConfig> {
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
                    isLoadingAddress = false
                ),
                visitList = listOf(
                    VisitDetailViewModel.VisitState(
                        id = UUID.randomUUID(),
                        subject = "",
                        date = LocalDateTime.now(),
                        isDone = false,
                        householderId = UUID.randomUUID(),
                        canBeRemoved = false,
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
                )
            )
        ),
        VisitDetailPreviewConfig(
            configName = "Edit Visit",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewVisitUiState.copy(canBeRemoved = false)
                )
            )
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
            )
        ),
        VisitDetailPreviewConfig(
            configName = "Multiple Visits",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewVisitUiState,
                    previewVisitUiState.copy(
                        id = UUID.randomUUID(),
                        subject = "Como podemos ter certeza de que há um Criador?",
                        isDone = false,
                        canBeRemoved = true
                    )
                )
            )
        ),
        VisitDetailPreviewConfig(
            configName = "Next Visit Suggestion",
            mainActivityUiState = previewMainActivityUiState,
            householderId = null,
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewVisitUiState.copy(
                        showNextVisitSuggestion = true
                    ),
                    previewVisitUiState.copy(
                        id = UUID.randomUUID(),
                        subject = "Como podemos ter certeza de que há um Criador?",
                        isDone = false,
                        canBeRemoved = true
                    )
                )
            )
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
                    previewVisitUiState.copy(
                        isDone = false,
                        canBeRemoved = false,
                        hasVisitTimeError = true
                    ),
                    previewVisitUiState.copy(
                        id = UUID.randomUUID(),
                        subject = "Como podemos ter certeza de que há um Criador?",
                        isDone = false,
                        canBeRemoved = true,
                        hasVisitTimeError = false
                    )
                ),
                eventState = VisitDetailViewModel.UiEventState.ValidationError
            )
        ),
    )

    override fun getDisplayName(index: Int): String? {
        return values.elementAt(index).configName
    }
}

data class VisitDetailPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val householderId: UUID?,
    val uiState: VisitDetailViewModel.UiState,
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

private val previewVisitUiState = VisitDetailViewModel.VisitState(
    id = UUID.randomUUID(),
    subject = "O que é o Reino de Deus?",
    date = LocalDateTime.now(),
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

private val previewVisitDetailUiState = VisitDetailViewModel.UiState(
    householder = VisitDetailViewModel.HouseholderState(
        id = UUID.randomUUID(),
        name = "Pedro",
        address = "Rua 1",
        notes = "Morador receptivo",
        showClearName = true,
        addressState = VisitDetailViewModel.HouseholderAddressState.LoadLocation,
        showClearNotes = false,
        isLoadingAddress = false
    ),
    visitList = listOf(previewVisitUiState),
    conversationList = listOf(),
    visitTypeList = listOf(),
    eventState = VisitDetailViewModel.UiEventState.Idle
)
