package com.msmobile.visitas.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Duration

class CalendarEventManager(
    private val context: Context,
    private val permissionChecker: PermissionChecker
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

        val calendarId = getFirstCalendarId() ?: return@withContext null
        val eventTitle = if (isDone) "$CHECKMARK$title" else title
        val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + duration.toMillis()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, eventTitle)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.EVENT_COLOR, color)
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
            false
        }
    }

    private fun insertEvent(values: ContentValues): Long? {
        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun updateEvent(eventId: Long, values: ContentValues): Long? {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            if (rowsUpdated > 0) eventId else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getFirstCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.IS_PRIMARY,
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
            val accountTypeIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

            if (idIndex < 0) return null

            var bestCalendarId: Long? = null
            var bestScore = -1

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(idIndex)
                val isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) == 1
                val accountType = if (accountTypeIndex >= 0) cursor.getString(accountTypeIndex) else null
                val isGoogle = accountType == GOOGLE_ACCOUNT_TYPE

                val score = calculateCalendarScore(isGoogle, isPrimary)
                if (score > bestScore) {
                    bestScore = score
                    bestCalendarId = calendarId
                }
            }
            return bestCalendarId
        }
        return null
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
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        private const val CHECKMARK = "✅ "
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private val DEFAULT_DURATION: Duration = Duration.ofMinutes(30)
        private const val DEFAULT_EVENT_COLOR: Int = 0xFF489160.toInt() // RGB(72, 145, 96)
    }
}
