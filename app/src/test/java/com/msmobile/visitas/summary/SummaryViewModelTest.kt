package com.msmobile.visitas.summary

import com.msmobile.visitas.ui.views.MonthNavigatorEvent
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import java.time.LocalDateTime

class SummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.returnVisitCount)
        assertEquals("", state.bibleStudyCount)
        assertEquals("", state.totalFieldServiceSeconds)
        assertFalse(state.isSummaryMenuExpanded)
        assertTrue(state.summaryFilterOptions.isEmpty())
        assertFalse(state.shouldShowSummaryDetails)
    }

    @Test
    fun `onEvent with ViewCreated loads summary from repository`() {
        // Arrange
        val summaryRepositoryRef = MockReferenceHolder<SummaryRepository>()
        val viewModel = createViewModel(summaryRepositoryRef = summaryRepositoryRef)

        // Act
        viewModel.onEvent(SummaryViewModel.UiEvent.ViewCreated)

        // Assert
        val summaryRepository = requireNotNull(summaryRepositoryRef.value)
        verifyBlocking(summaryRepository) { getSummary(any(), any()) }
        val state = viewModel.uiState.value
        assertEquals("5", state.returnVisitCount)
        assertEquals("2", state.bibleStudyCount)
        assertEquals(SummaryViewModel.SummaryMenuOption.entries, state.summaryFilterOptions)
    }

    @Test
    fun `onEvent with SummaryFilterButtonClicked expands summary menu`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(SummaryViewModel.UiEvent.SummaryFilterButtonClicked)

        // Assert
        assertTrue(viewModel.uiState.value.isSummaryMenuExpanded)
    }

    @Test
    fun `onEvent with SummaryMenuDismissed collapses summary menu`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(SummaryViewModel.UiEvent.SummaryFilterButtonClicked)
        assertTrue(viewModel.uiState.value.isSummaryMenuExpanded)

        // Act
        viewModel.onEvent(SummaryViewModel.UiEvent.SummaryMenuDismissed)

        // Assert
        assertFalse(viewModel.uiState.value.isSummaryMenuExpanded)
    }

    @Test
    fun `onEvent with SummaryMenuSelected ShowDetails toggles shouldShowSummaryDetails`() {
        // Arrange
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.shouldShowSummaryDetails)

        // Act
        viewModel.onEvent(
            SummaryViewModel.UiEvent.SummaryMenuSelected(SummaryViewModel.SummaryMenuOption.ShowDetails)
        )

        // Assert
        assertTrue(viewModel.uiState.value.shouldShowSummaryDetails)
        assertFalse(viewModel.uiState.value.isSummaryMenuExpanded)

        // Act again
        viewModel.onEvent(
            SummaryViewModel.UiEvent.SummaryMenuSelected(SummaryViewModel.SummaryMenuOption.ShowDetails)
        )

        // Assert
        assertFalse(viewModel.uiState.value.shouldShowSummaryDetails)
    }

    @Test
    fun `onEvent with SummaryMenuSelected GoToCurrentMonth resets to current month`() {
        // Arrange
        val summaryRepositoryRef = MockReferenceHolder<SummaryRepository>()
        val viewModel = createViewModel(summaryRepositoryRef = summaryRepositoryRef)
        viewModel.onEvent(SummaryViewModel.UiEvent.ViewCreated)
        // Navigate away from current month
        viewModel.onMonthPickerEvent(MonthNavigatorEvent.PreviousMonthClicked)

        // Act
        viewModel.onEvent(
            SummaryViewModel.UiEvent.SummaryMenuSelected(SummaryViewModel.SummaryMenuOption.GoToCurrentMonth)
        )

        // Assert
        val state = viewModel.uiState.value
        val now = LocalDateTime.now()
        assertEquals(now.month, state.selectedMonth.month)
        assertEquals(now.year, state.selectedMonth.year)
        assertFalse(state.isSummaryMenuExpanded)
    }

    @Test
    fun `onMonthPickerEvent with NextMonthClicked advances to next month`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(SummaryViewModel.UiEvent.ViewCreated)
        val initialMonth = viewModel.uiState.value.selectedMonth

        // Act
        viewModel.onMonthPickerEvent(MonthNavigatorEvent.NextMonthClicked)

        // Assert
        val expectedMonth = initialMonth.plusMonths(1)
        assertEquals(expectedMonth.month, viewModel.uiState.value.selectedMonth.month)
    }

    @Test
    fun `onMonthPickerEvent with PreviousMonthClicked goes to previous month`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(SummaryViewModel.UiEvent.ViewCreated)
        val initialMonth = viewModel.uiState.value.selectedMonth

        // Act
        viewModel.onMonthPickerEvent(MonthNavigatorEvent.PreviousMonthClicked)

        // Assert
        val expectedMonth = initialMonth.plusMonths(-1)
        assertEquals(expectedMonth.month, viewModel.uiState.value.selectedMonth.month)
    }

    private fun createViewModel(
        summaryRepositoryRef: MockReferenceHolder<SummaryRepository>? = null
    ): SummaryViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val summaryRepository = mock<SummaryRepository> {
            onBlocking { getSummary(any(), any()) } doReturn createSummaryResult()
        }
        summaryRepositoryRef?.value = summaryRepository

        return SummaryViewModel(
            summaryRepository = summaryRepository,
            dispatchers = dispatchers
        )
    }

    private fun createSummaryResult(): SummaryResult {
        return SummaryResult(
            returnVisitCount = 5,
            bibleStudyCount = 2,
            totalFieldServiceSeconds = 3600,
            fieldServiceInProgressSeconds = 0
        )
    }
}
