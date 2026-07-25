package com.msmobile.visitas.util

import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.visit.VisitType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncVisitCalendarEventUseCase @Inject constructor(
    private val calendarEventManager: CalendarEventManager,
    private val preferenceRepository: PreferenceRepository
) {
    suspend operator fun invoke(
        calendarEventId: Long?,
        visitType: VisitType,
        subject: String,
        date: LocalDateTime,
        isDone: Boolean,
        householderName: String
    ): Long? {
        // Calendar sync is opt-in. When it is off, events already on the calendar are left
        // untouched: the id is returned unchanged so re-enabling the setting keeps updating them.
        if (!preferenceRepository.get().addVisitsToCalendar) return calendarEventId
        if (!calendarEventManager.hasCalendarPermission()) return calendarEventId
        if (visitType == VisitType.FIRST_VISIT) return null
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
