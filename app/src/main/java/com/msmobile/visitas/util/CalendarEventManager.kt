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
        color: Int = DEFAULT_EVENT_COLOR
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
            // calendars. Raw EVENT_COLOR is only honored locally and gets
            // dropped by the Google sync adapter.
            val colorKey = getNearestEventColorKey(calendar, color)
            if (colorKey != null) {
                put(CalendarContract.Events.EVENT_COLOR_KEY, colorKey)
            } else {
                putNull(CalendarContract.Events.EVENT_COLOR_KEY)
                put(CalendarContract.Events.EVENT_COLOR, color)
            }
        }

        return@withContext if (eventId != null && eventExists(eventId)) {
            updateEvent(eventId, values)
        } else {
            insertEvent(values)
        }
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

    /**
     * Finds the key of the account's event color (from [CalendarContract.Colors])
     * closest to [color]. Google accounts sync a fixed palette of event colors,
     * and only colors referenced by key propagate to other devices and the web.
     * Returns null when the account exposes no event colors (e.g. local calendars).
     */
    private fun getNearestEventColorKey(calendar: CalendarInfo, color: Int): String? {
        val accountName = calendar.accountName ?: return null
        val accountType = calendar.accountType ?: return null

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
                if (keyIndex < 0 || colorIndex < 0) return@use null

                var bestKey: String? = null
                var bestDistance = Int.MAX_VALUE

                while (cursor.moveToNext()) {
                    val key = cursor.getString(keyIndex) ?: continue
                    val distance = colorDistance(color, cursor.getInt(colorIndex))
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestKey = key
                    }
                }
                bestKey
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error(TAG, "Failed to query event colors for account $accountName", e)
            null
        }
    }

    private fun colorDistance(first: Int, second: Int): Int {
        val deltaRed = ((first shr 16) and 0xFF) - ((second shr 16) and 0xFF)
        val deltaGreen = ((first shr 8) and 0xFF) - ((second shr 8) and 0xFF)
        val deltaBlue = (first and 0xFF) - (second and 0xFF)
        return deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue
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

    companion object {
        private const val TAG = "CalendarEventManager"
        private const val CHECKMARK = "✅ "
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private val DEFAULT_DURATION: Duration = Duration.ofMinutes(30)
        private const val DEFAULT_EVENT_COLOR: Int = 0xFF489160.toInt() // RGB(72, 145, 96)
    }
}
