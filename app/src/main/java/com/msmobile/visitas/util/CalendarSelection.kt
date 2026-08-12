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
    val ownerAccount: String?,
    val accountType: String?,
    val isPrimary: Boolean
)

/**
 * Identifies a calendar in a way that survives what a row id does not: provider id reuse, a
 * reinstall, and a backup restored on another device signed into the same account.
 *
 * All three parts are needed. On a real device two rows shared the ownerAccount
 * `pt.brazilian#holiday@group.v.calendar.google.com` — the same public holiday calendar subscribed
 * under two different Google accounts — so [accountName] is what tells them apart.
 */
data class CalendarIdentity(
    val accountType: String,
    val accountName: String,
    val ownerAccount: String
)

/**
 * This calendar's stable identity, or null when the provider did not supply all three parts. A
 * calendar without an identity can still be written to; it just cannot be remembered across a
 * reinstall, so it is never matched against a stored preference.
 */
val CalendarInfo.identity: CalendarIdentity?
    get() {
        if (accountType.isNullOrBlank() || accountName.isNullOrBlank() || ownerAccount.isNullOrBlank()) {
            return null
        }
        return CalendarIdentity(accountType, accountName, ownerAccount)
    }

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
 * ordered by [orderedByAutoPickPreference] — both when [preferred] is null and when it names a
 * calendar that is no longer present, so events keep being written instead of silently stopping.
 */
fun List<CalendarInfo>.resolvePreferred(preferred: CalendarIdentity?): CalendarInfo? =
    preferred?.let { wanted -> firstOrNull { it.identity == wanted } } ?: firstOrNull()

private fun CalendarInfo.autoPickScore(): Int {
    val isGoogle = accountType == GOOGLE_ACCOUNT_TYPE
    return when {
        isGoogle && isPrimary -> 3
        isGoogle -> 2
        isPrimary -> 1
        else -> 0
    }
}
