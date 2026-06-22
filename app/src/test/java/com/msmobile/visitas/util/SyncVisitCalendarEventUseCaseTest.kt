package com.msmobile.visitas.util

import com.msmobile.visitas.visit.VisitType
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import java.time.LocalDateTime

class SyncVisitCalendarEventUseCaseTest {

    private val testDate = LocalDateTime.of(2026, 6, 21, 10, 0)

    @Test
    fun `FIRST_VISIT with existing calendar event deletes the event and returns null`() = runBlocking {
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn true
            onBlocking { deleteEvent(any()) } doReturn true
        }
        val useCase = SyncVisitCalendarEventUseCase(calendarEventManager)

        val result = useCase(
            calendarEventId = 42L,
            visitType = VisitType.FIRST_VISIT,
            subject = "subject",
            date = testDate,
            isDone = false,
            householderName = "name"
        )

        assertNull(result)
        verifyBlocking(calendarEventManager) { deleteEvent(42L) }
    }

    @Test
    fun `FIRST_VISIT with no existing calendar event does not call deleteEvent`() = runBlocking {
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn true
        }
        val useCase = SyncVisitCalendarEventUseCase(calendarEventManager)

        val result = useCase(
            calendarEventId = null,
            visitType = VisitType.FIRST_VISIT,
            subject = "subject",
            date = testDate,
            isDone = false,
            householderName = "name"
        )

        assertNull(result)
        verifyBlocking(calendarEventManager, never()) { deleteEvent(any()) }
    }
}
