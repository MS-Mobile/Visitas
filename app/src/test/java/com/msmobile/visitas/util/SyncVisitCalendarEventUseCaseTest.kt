package com.msmobile.visitas.util

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitType
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import java.time.Duration
import java.time.LocalDateTime

class SyncVisitCalendarEventUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `invoke writes a return visit with the return visit duration`() = runTest {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val useCase = createUseCase(calendarEventManagerRef = calendarEventManagerRef)

        useCase.sync(VisitType.RETURN_VISIT)

        verifyBlocking(requireNotNull(calendarEventManagerRef.value)) {
            saveEvent(anyOrNull(), anyOrNull(), any(), any(), any(), eq(RETURN_VISIT_DURATION), any())
        }
    }

    @Test
    fun `invoke writes a bible study with the bible study duration`() = runTest {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val useCase = createUseCase(calendarEventManagerRef = calendarEventManagerRef)

        useCase.sync(VisitType.BIBLE_STUDY)

        verifyBlocking(requireNotNull(calendarEventManagerRef.value)) {
            saveEvent(anyOrNull(), anyOrNull(), any(), any(), any(), eq(BIBLE_STUDY_DURATION), any())
        }
    }

    @Test
    fun `invoke writes no event for a first visit`() = runTest {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val useCase = createUseCase(calendarEventManagerRef = calendarEventManagerRef)

        val eventId = useCase.sync(VisitType.FIRST_VISIT)

        assertNull(eventId)
        verifyBlocking(requireNotNull(calendarEventManagerRef.value), never()) {
            saveEvent(anyOrNull(), anyOrNull(), any(), any(), any(), any(), any())
        }
    }

    private suspend fun SyncVisitCalendarEventUseCase.sync(visitType: VisitType): Long? = invoke(
        calendarEventId = null,
        preference = Preference(
            visitListDateFilterOption = VisitListDateFilterOption.All,
            visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
            returnVisitDurationMillis = RETURN_VISIT_DURATION.toMillis(),
            bibleStudyDurationMillis = BIBLE_STUDY_DURATION.toMillis()
        ),
        visitType = visitType,
        subject = "Subject",
        date = LocalDateTime.of(2026, 10, 13, 10, 0),
        isDone = false,
        householderName = "Householder"
    )

    private fun createUseCase(
        calendarEventManagerRef: MockReferenceHolder<CalendarEventManager>? = null
    ): SyncVisitCalendarEventUseCase {
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn true
        }
        calendarEventManagerRef?.value = calendarEventManager
        return SyncVisitCalendarEventUseCase(calendarEventManager)
    }

    private companion object {
        // Neither is a default, so a test can tell the chosen duration from the fallback.
        val RETURN_VISIT_DURATION: Duration = Duration.ofMinutes(45)
        val BIBLE_STUDY_DURATION: Duration = Duration.ofMinutes(90)
    }
}
