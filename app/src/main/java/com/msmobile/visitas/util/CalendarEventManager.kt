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
        title: String,
        description: String,
        startTime: LocalDateTime,
        duration: Duration = DEFAULT_DURATION,
        isDone: Boolean = false,
        color: ColorKey = getDefaultColorKey()
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext null
        }

        val calendar = getFirstCalendar() ?: return@withContext null
        val eventTitle = if (isDone) "$CHECKMARK$title" else title
        val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + duration.toMillis()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendar.id)
            put(CalendarContract.Events.TITLE, eventTitle)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            // EVENT_COLOR_KEY references the account's synced color palette
            // (CalendarContract.Colors), so the color survives sync for Google
            // calendars. The provider rejects keys the account doesn't have,
            // so only apply the key when it exists in the palette.
            if (queryEventColors(calendar).any { it.key == color }) {
                put(CalendarContract.Events.EVENT_COLOR_KEY, color.value)
            } else {
                putNull(CalendarContract.Events.EVENT_COLOR_KEY)
            }
        }

        return@withContext if (eventId != null && eventExists(eventId)) {
            updateEvent(eventId, values)
        } else {
            insertEvent(values)
        }
    }

    /**
     * Returns the event colors available for the calendar events are saved to,
     * as synced by the account (from [CalendarContract.Colors]). Empty when the
     * account exposes no event color palette (e.g. local calendars).
     */
    suspend fun getAvailableColors(): List<EventColor> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext emptyList()
        }
        val calendar = getFirstCalendar() ?: return@withContext emptyList()
        queryEventColors(calendar)
    }

    fun getDefaultColorKey(): ColorKey = DEFAULT_COLOR_KEY

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

    private fun getFirstCalendar(): CalendarInfo? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        val selection = """
            ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND
            ${CalendarContract.Calendars.VISIBLE} = ? AND
            ${CalendarContract.Calendars.SYNC_EVENTS} = ?
        """.trimIndent()
        val selectionArgs = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString(),
            "1",  // Visible
            "1"   // Sync events enabled
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val isPrimaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accountNameIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

            if (idIndex < 0) return null

            var bestCalendar: CalendarInfo? = null
            var bestScore = -1

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(idIndex)
                val isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) == 1
                val accountName = if (accountNameIndex >= 0) cursor.getString(accountNameIndex) else null
                val accountType = if (accountTypeIndex >= 0) cursor.getString(accountTypeIndex) else null
                val isGoogle = accountType == GOOGLE_ACCOUNT_TYPE

                val score = calculateCalendarScore(isGoogle, isPrimary)
                if (score > bestScore) {
                    bestScore = score
                    bestCalendar = CalendarInfo(calendarId, accountName, accountType)
                }
            }
            return bestCalendar
        }
        return null
    }

    private fun queryEventColors(calendar: CalendarInfo): List<EventColor> {
        val accountName = calendar.accountName ?: return emptyList()
        val accountType = calendar.accountType ?: return emptyList()

        val projection = arrayOf(
            CalendarContract.Colors.COLOR_KEY,
            CalendarContract.Colors.COLOR
        )
        val selection = """
            ${CalendarContract.Colors.ACCOUNT_NAME} = ? AND
            ${CalendarContract.Colors.ACCOUNT_TYPE} = ? AND
            ${CalendarContract.Colors.COLOR_TYPE} = ?
        """.trimIndent()
        val selectionArgs = arrayOf(
            accountName,
            accountType,
            CalendarContract.Colors.TYPE_EVENT.toString()
        )

        return try {
            context.contentResolver.query(
                CalendarContract.Colors.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val keyIndex = cursor.getColumnIndex(CalendarContract.Colors.COLOR_KEY)
                val colorIndex = cursor.getColumnIndex(CalendarContract.Colors.COLOR)
                if (keyIndex < 0 || colorIndex < 0) return@use emptyList()

                buildList {
                    while (cursor.moveToNext()) {
                        val key = cursor.getString(keyIndex) ?: continue
                        add(EventColor(ColorKey(key), cursor.getInt(colorIndex)))
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error(TAG, "Failed to query event colors for account $accountName", e)
            emptyList()
        }
    }

    private fun calculateCalendarScore(isGoogle: Boolean, isPrimary: Boolean): Int {
        return when {
            isGoogle && isPrimary -> 3
            isGoogle -> 2
            isPrimary -> 1
            else -> 0
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

    private data class CalendarInfo(
        val id: Long,
        val accountName: String?,
        val accountType: String?
    )

    @JvmInline
    value class ColorKey(val value: String)

    data class EventColor(val key: ColorKey, val argb: Int)

    companion object {
        private const val TAG = "CalendarEventManager"
        private const val CHECKMARK = "✅ "
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private val DEFAULT_DURATION: Duration = Duration.ofMinutes(30)
        // Google Calendar's "Sage" green, the palette color closest to the
        // RGB(72, 145, 96) the app used before switching to color keys
        private val DEFAULT_COLOR_KEY = ColorKey("2")
    }
}
