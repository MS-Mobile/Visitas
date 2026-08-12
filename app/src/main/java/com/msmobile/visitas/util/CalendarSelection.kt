package com.msmobile.visitas.util

private const val GOOGLE_ACCOUNT_TYPE = "com.google"

/**
 * A calendar the app may write events to, as listed for the Settings dropdown and used on the
 * write path. [accountName] is what tells two calendars with the same [displayName] apart;
 * [accountType] and [isPrimary] are the inputs to the automatic pick.
 */
data class CalendarInfo(
    val id: Long,
    /** Empty when the provider supplies no name; the dropdown substitutes a placeholder. */
    val displayName: String,
    val accountName: String?,
    val accountType: String?,
    val isPrimary: Boolean
)

/**
 * Orders calendars best-candidate-first: a Google account's primary calendar, then any other
 * Google calendar, then a non-Google primary, then everything else.
 *
 * The sort is stable, so calendars with equal standing keep the order they arrived in. That is
 * deliberate — it reproduces the pick the app made before calendar selection existed, so upgrading
 * does not silently move a user's events to a different calendar.
 */
fun List<CalendarInfo>.orderedByAutoPickPreference(): List<CalendarInfo> =
    sortedByDescending { it.autoPickScore() }

/**
 * Picks the calendar events are written to.
 *
 * Falls back to the receiver's first entry — the best automatic candidate, provided the receiver is
 * ordered by [orderedByAutoPickPreference] — both when [preferredCalendarId] is null and when it
 * names a calendar that is no longer in the list, deleted or its account removed, so events keep
 * being written instead of silently stopping.
 */
fun List<CalendarInfo>.resolvePreferred(preferredCalendarId: Long?): CalendarInfo? =
    firstOrNull { it.id == preferredCalendarId } ?: firstOrNull()

private fun CalendarInfo.autoPickScore(): Int {
    val isGoogle = accountType == GOOGLE_ACCOUNT_TYPE
    return when {
        isGoogle && isPrimary -> 3
        isGoogle -> 2
        isPrimary -> 1
        else -> 0
    }
}
