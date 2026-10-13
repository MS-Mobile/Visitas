package com.msmobile.visitas.util

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.preferredCalendar
import com.msmobile.visitas.visit.VisitType
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncVisitCalendarEventUseCase @Inject constructor(
    private val calendarEventManager: CalendarEventManager
) {
    suspend operator fun invoke(
        calendarEventId: Long?,
        preference: Preference,
        visitType: VisitType,
        subject: String,
        date: LocalDateTime,
        isDone: Boolean,
        householderName: String
    ): Long? {
        if (!calendarEventManager.hasCalendarPermission()) return calendarEventId
        // A first visit gets no event; the others last as long as the user chose for their type.
        val durationMillis = when (visitType) {
            VisitType.FIRST_VISIT -> return null
            VisitType.RETURN_VISIT -> preference.returnVisitDurationMillis
            VisitType.BIBLE_STUDY -> preference.bibleStudyDurationMillis
        }
        val title = if (subject.isNotBlank()) {
            "$householderName - ${subject.lines().firstOrNull() ?: ""}"
        } else {
            householderName
        }
        return calendarEventManager.saveEvent(
            eventId = calendarEventId,
            calendar = preference.preferredCalendar,
            title = title,
            description = subject,
            startTime = date,
            duration = Duration.ofMillis(durationMillis),
            isDone = isDone
        )
    }
}
