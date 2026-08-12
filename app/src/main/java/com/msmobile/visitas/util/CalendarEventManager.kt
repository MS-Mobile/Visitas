package com.msmobile.visitas.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

class CalendarEventManager(
    private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val logger: Logger
) {
    fun hasCalendarPermission(): Boolean {
        return permissionChecker.hasPermissions(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }

    suspend fun saveEvent(
        eventId: Long? = null,
        calendar: CalendarIdentity? = null,
        title: String,
        description: String,
        startTime: LocalDateTime,
        duration: Duration = DEFAULT_DURATION,
        isDone: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext null
        }

        val resolvedCalendar = resolveCalendar(calendar) ?: return@withContext null
        val eventTitle = if (isDone) "$CHECKMARK$title" else title
        val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + duration.toMillis()

        // EVENT_COLOR_KEY is deliberately not written: an event with no color of its own renders
        // in the color of the calendar it belongs to. Note this block also feeds updateEvent, and
        // omitting the column leaves any key an existing event already carries untouched — events
        // created before this change keep their old color, which is intended.
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, resolvedCalendar.id)
            put(CalendarContract.Events.TITLE, eventTitle)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
        }

        return@withContext if (eventId != null && eventExists(eventId)) {
            updateEvent(eventId, values)
        } else {
            insertEvent(values)
        }
    }

    /**
     * The calendars the app may write to, ordered best-candidate-first. Empty without calendar
     * permission, and also on a provider error, which is logged.
     */
    suspend fun getAvailableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext emptyList()
        }
        queryWritableCalendars()
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext false
        }

        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error(TAG, "Failed to delete calendar event $eventId", e)
            false
        }
    }

    private fun insertEvent(values: ContentValues): Long? {
        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            logger.error(TAG, "Failed to insert calendar event", e)
            null
        }
    }

    private fun updateEvent(eventId: Long, values: ContentValues): Long? {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            if (rowsUpdated > 0) eventId else null
        } catch (e: Exception) {
            logger.error(TAG, "Failed to update calendar event $eventId", e)
            null
        }
    }

    private fun resolveCalendar(preferred: CalendarIdentity?): CalendarInfo? =
        queryWritableCalendars().resolvePreferred(preferred)

    /**
     * The writable calendars, ranked by [orderedByAutoPickPreference]. The ordering is the contract:
     * [resolvePreferred] falls back to the first entry, so returning these unranked would silently
     * change which calendar events land in.
     */
    private fun queryWritableCalendars(): List<CalendarInfo> {
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                WRITABLE_CALENDAR_PROJECTION,
                WRITABLE_CALENDAR_SELECTION,
                WRITABLE_CALENDAR_SELECTION_ARGS,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIndex < 0) return@use emptyList()

                val displayNameIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val isPrimaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val accountNameIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val ownerAccountIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                val accountTypeIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val visibleIndex = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarInfo(
                                id = cursor.getLong(idIndex),
                                displayName = if (displayNameIndex >= 0) {
                                    cursor.getString(displayNameIndex).orEmpty()
                                } else {
                                    ""
                                },
                                accountName = if (accountNameIndex >= 0) {
                                    cursor.getString(accountNameIndex)
                                } else {
                                    null
                                },
                                ownerAccount = if (ownerAccountIndex >= 0) {
                                    cursor.getString(ownerAccountIndex)
                                } else {
                                    null
                                },
                                accountType = if (accountTypeIndex >= 0) {
                                    cursor.getString(accountTypeIndex)
                                } else {
                                    null
                                },
                                isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) == 1,
                                // A missing column defaults to visible, so an unknown calendar ranks
                                // exactly as it would have before VISIBLE was read at all.
                                isVisible = visibleIndex < 0 || cursor.getInt(visibleIndex) == 1
                            )
                        )
                    }
                }.orderedByAutoPickPreference()
            } ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error(TAG, "Failed to query calendars", e)
            emptyList()
        }
    }

    private fun eventExists(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return try {
            context.contentResolver.query(
                /* uri = */ uri,
                /* projection = */ arrayOf(CalendarContract.Events._ID),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            logger.error(TAG, "Failed to check if calendar event $eventId exists", e)
            false
        }
    }

    companion object {
        private const val TAG = "CalendarEventManager"
        private const val CHECKMARK = "✅ "
        private val DEFAULT_DURATION: Duration = Duration.ofMinutes(30)

        private val WRITABLE_CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE
        )

        // VISIBLE is deliberately not part of the selection: it is a display preference in the
        // user's calendar app, not a permission, so it has no business gating which calendars the
        // app may offer for an explicit choice. It is still read into CalendarInfo.isVisible below,
        // because it dominates the automatic-pick ranking (see orderedByAutoPickPreference).
        private val WRITABLE_CALENDAR_SELECTION = """
            ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND
            ${CalendarContract.Calendars.SYNC_EVENTS} = ?
        """.trimIndent()

        private val WRITABLE_CALENDAR_SELECTION_ARGS = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString(),
            "1"   // Sync events enabled
        )
    }
}
