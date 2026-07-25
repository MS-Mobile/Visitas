package com.msmobile.visitas.util

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import java.time.LocalDateTime

class SyncVisitCalendarEventUseCaseTest {

    @Test
    fun `invoke with the calendar preference disabled leaves the event untouched`() = runTest {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val useCase = createUseCase(
            addVisitsToCalendar = false,
            calendarEventManagerRef = calendarEventManagerRef
        )

        val result = useCase.invokeForVisit()

        assertEquals(EXISTING_EVENT_ID, result)
        verifyBlocking(requireNotNull(calendarEventManagerRef.value), never()) {
            saveEvent(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }
    }

    @Test
    fun `invoke with the calendar preference enabled saves the event`() = runTest {
        val useCase = createUseCase(addVisitsToCalendar = true)

        val result = useCase.invokeForVisit()

        assertEquals(SAVED_EVENT_ID, result)
    }

    @Test
    fun `invoke without calendar permission leaves the event untouched`() = runTest {
        val useCase = createUseCase(addVisitsToCalendar = true, hasCalendarPermission = false)

        val result = useCase.invokeForVisit()

        assertEquals(EXISTING_EVENT_ID, result)
    }

    @Test
    fun `invoke for a first visit does not create an event`() = runTest {
        val useCase = createUseCase(addVisitsToCalendar = true)

        val result = useCase.invokeForVisit(visitType = VisitType.FIRST_VISIT)

        assertNull(result)
    }

    private suspend fun SyncVisitCalendarEventUseCase.invokeForVisit(
        visitType: VisitType = VisitType.RETURN_VISIT
    ): Long? {
        return invoke(
            calendarEventId = EXISTING_EVENT_ID,
            visitType = visitType,
            subject = "Subject",
            date = TEST_DATE_TIME,
            isDone = false,
            householderName = "Householder"
        )
    }

    private fun createUseCase(
        addVisitsToCalendar: Boolean = false,
        hasCalendarPermission: Boolean = true,
        calendarEventManagerRef: MockReferenceHolder<CalendarEventManager>? = null
    ): SyncVisitCalendarEventUseCase {
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn hasCalendarPermission
            onBlocking {
                saveEvent(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            } doReturn SAVED_EVENT_ID
        }
        calendarEventManagerRef?.value = calendarEventManager

        val preferenceRepository = mock<PreferenceRepository> {
            onBlocking { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                addVisitsToCalendar = addVisitsToCalendar
            )
        }

        return SyncVisitCalendarEventUseCase(
            calendarEventManager = calendarEventManager,
            preferenceRepository = preferenceRepository
        )
    }

    companion object {
        private const val EXISTING_EVENT_ID = 42L
        private const val SAVED_EVENT_ID = 99L
        private val TEST_DATE_TIME = LocalDateTime.of(2024, 1, 15, 10, 30)
    }
}
