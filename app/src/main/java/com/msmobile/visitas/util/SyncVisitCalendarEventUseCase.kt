package com.msmobile.visitas.util

import com.msmobile.visitas.visit.VisitType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncVisitCalendarEventUseCase @Inject constructor(
    private val calendarEventManager: CalendarEventManager
) {
    suspend operator fun invoke(
        calendarEventId: Long?,
        visitType: VisitType,
        subject: String,
        date: LocalDateTime,
        isDone: Boolean,
        householderName: String
    ): Long? {
        if (!calendarEventManager.hasCalendarPermission()) return calendarEventId
        if (visitType == VisitType.FIRST_VISIT) {
            calendarEventId?.let { calendarEventManager.deleteEvent(it) }
            return null
        }
        val title = if (subject.isNotBlank()) {
            "$householderName - ${subject.lines().firstOrNull() ?: ""}"
        } else {
            householderName
        }
        return calendarEventManager.saveEvent(
            eventId = calendarEventId,
            title = title,
            description = subject,
            startTime = date,
            isDone = isDone
        )
    }
}
