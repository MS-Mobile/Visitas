package com.msmobile.visitas.util

/**
 * A calendar the app may write events to, as returned by
 * [CalendarEventManager.getAvailableCalendars]. [accountType] drives the automatic preference for
 * Google calendars; [accountName] is what tells two calendars with the same [displayName] apart in
 * the Settings dropdown.
 */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val accountType: String?
)

/**
 * Picks the calendar events are written to.
 *
 * The receiver is ordered best-candidate-first, so falling back to the first entry reproduces the
 * automatic choice the app made before calendar selection existed. A [preferredCalendarId] that is
 * no longer in the list — the calendar was deleted, or its account was removed — falls back the
 * same way, so events keep being written instead of silently stopping.
 *
 * Callers must not reorder the list before calling this: the fallback *is* the ordering.
 */
fun List<CalendarInfo>.resolvePreferred(preferredCalendarId: Long?): CalendarInfo? =
    firstOrNull { it.id == preferredCalendarId } ?: firstOrNull()
