package com.msmobile.visitas.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.views.MonthNavigatorEvent
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.StringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SummaryViewModel
@Inject
constructor(
    private val summaryRepository: SummaryRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            returnVisitCount = "",
            bibleStudyCount = "",
            totalFieldServiceSeconds = "",
            selectedMonth = LocalDateTime.now(),
            isSummaryMenuExpanded = false,
            summaryFilterOptions = listOf(),
            shouldShowSummaryDetails = false
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> viewCreated()
            is UiEvent.SummaryFilterButtonClicked -> summaryFilterButtonClicked()
            is UiEvent.SummaryMenuDismissed -> summaryMenuDismissed()
            is UiEvent.SummaryMenuSelected -> summaryMenuSelected(event.option)
        }
    }

    private fun summaryMenuSelected(option: SummaryMenuOption) {
        newState {
            val isSummaryMenuExpanded = false
            copy(isSummaryMenuExpanded = isSummaryMenuExpanded)
        }
        when (option) {
            SummaryMenuOption.GoToCurrentMonth -> goToCurrentMonth()
            SummaryMenuOption.ShowDetails -> toggleShowDetails()
        }
    }

    private fun toggleShowDetails() {
        newState {
            copy(shouldShowSummaryDetails = !shouldShowSummaryDetails)
        }
    }

    private fun summaryMenuDismissed() {
        newState {
            copy(isSummaryMenuExpanded = false)
        }
    }

    private fun summaryFilterButtonClicked() {
        newState {
            copy(isSummaryMenuExpanded = true)
        }
    }

    private fun viewCreated() {
        newState {
            val summaryFilterOptions = SummaryMenuOption.entries
            copy(summaryFilterOptions = summaryFilterOptions)
        }
        loadSummary(_uiState.value.selectedMonth)
    }

    fun onMonthPickerEvent(monthNavigatorEvent: MonthNavigatorEvent) {
        when (monthNavigatorEvent) {
            MonthNavigatorEvent.NextMonthClicked -> nextMonthClicked()
            MonthNavigatorEvent.PreviousMonthClicked -> previousMonthClicked()
        }
    }

    private fun goToCurrentMonth() {
        newState {
            val currentMonth = LocalDateTime.now()
            copy(selectedMonth = currentMonth)
        }
        loadSummary(_uiState.value.selectedMonth)
    }

    private fun previousMonthClicked() {
        newState {
            val previousMonth = selectedMonth.plusMonths(-1)
            copy(selectedMonth = previousMonth)
        }
        loadSummary(_uiState.value.selectedMonth)
    }

    private fun nextMonthClicked() {
        newState {
            val nextMonth = selectedMonth.plusMonths(1)
            copy(selectedMonth = nextMonth)
        }
        loadSummary(_uiState.value.selectedMonth)
    }

    private fun loadSummary(currentMonth: LocalDateTime) {
        viewModelScope.launch(dispatchers.io) {
            val (start, end) = let {
                currentMonth.with(firstDayOfMonth()) to currentMonth.with(lastDayOfMonth())
            }
            val summary = summaryRepository.getSummary(start, end)
            val returnVisitCount = summary.returnVisitCount.toString()
            val bibleStudyCount = summary.bibleStudyCount.toString()
            val totalFieldServiceSeconds = summary.totalFieldServiceSeconds
            val totalFieldServiceTime = totalFieldServiceSeconds.seconds.toString()

            newState {
                copy(
                    returnVisitCount = returnVisitCount,
                    bibleStudyCount = bibleStudyCount,
                    totalFieldServiceSeconds = totalFieldServiceTime,
                )
            }
        }
    }

    private fun newState(value: UiState.() -> UiState) {
        _uiState.update(value)
    }

    enum class SummaryMenuOption(val description: StringResource) {
        ShowDetails(description = StringResource(R.string.show_summary_details)),
        GoToCurrentMonth(description = StringResource(R.string.go_current_month_summary)),
    }

    sealed class UiEvent {
        data object SummaryFilterButtonClicked : UiEvent()
        data object SummaryMenuDismissed : UiEvent()
        data object ViewCreated : UiEvent()
        data class SummaryMenuSelected(val option: SummaryMenuOption) : UiEvent()
    }

    data class UiState(
        val returnVisitCount: String,
        val bibleStudyCount: String,
        val totalFieldServiceSeconds: String,
        val selectedMonth: LocalDateTime,
        val isSummaryMenuExpanded: Boolean,
        val summaryFilterOptions: List<SummaryMenuOption>,
        val shouldShowSummaryDetails: Boolean,
        val shouldShowSummaryHours: Boolean = false
    )
}