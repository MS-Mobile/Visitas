package com.msmobile.visitas.util

private const val GOOGLE_ACCOUNT_TYPE = "com.google"

/**
 * A calendar the app may write events to, as listed for the Settings dropdown and used on the
 * write path. [accountName] is what tells two calendars with the same [displayName] apart;
 * [accountType], [isPrimary] and [isVisible] are the inputs to the automatic pick. [isVisible] is
 * a display preference from the user's calendar app, not a permission — a calendar can be hidden
 * from the day view and still be perfectly writable.
 */
data class CalendarInfo(
    val id: Long,
    /** Empty when the provider supplies no name; the dropdown substitutes a placeholder. */
    val displayName: String,
    val accountName: String?,
    val ownerAccount: String?,
    val accountType: String?,
    val isPrimary: Boolean,
    val isVisible: Boolean
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
 * Google calendar, then a non-Google primary, then everything else — and, dominating all of that,
 * every visible calendar ranks above every hidden one. Hidden calendars used to be excluded from
 * the candidate list entirely, so ranking them last keeps the automatic pick exactly where it was
 * for existing installs whenever at least one visible writable calendar exists.
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
    val accountScore = when {
        isGoogle && isPrimary -> 3
        isGoogle -> 2
        isPrimary -> 1
        else -> 0
    }
    // Visibility dominates every account consideration. Hidden calendars used to be filtered out
    // of the query entirely, so they were never automatic candidates; ranking them below every
    // visible calendar keeps the automatic pick exactly where it was for existing installs.
    return if (isVisible) accountScore + VISIBLE_RANK_OFFSET else accountScore
}

private const val VISIBLE_RANK_OFFSET = 4
