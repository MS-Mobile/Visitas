package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class CalendarSelectionTest {

    @Test
    fun `resolvePreferred returns the preferred calendar when it is available`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK, LOCAL)

        val resolved = calendars.resolvePreferred(preferredCalendarId = WORK.id)

        assertEquals(WORK, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when the preferred one is gone`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)

        val resolved = calendars.resolvePreferred(preferredCalendarId = 999L)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when no preference is set`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)

        val resolved = calendars.resolvePreferred(preferredCalendarId = null)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred returns null when no calendars are available`() {
        val resolved = emptyList<CalendarInfo>().resolvePreferred(preferredCalendarId = 1L)

        assertNull(resolved)
    }

    private companion object {
        val GOOGLE_PRIMARY = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            accountType = "com.google"
        )
        val WORK = CalendarInfo(
            id = 2L,
            displayName = "Work",
            accountName = "user@work.com",
            accountType = "com.google"
        )
        val LOCAL = CalendarInfo(
            id = 3L,
            displayName = "Offline",
            accountName = "Local",
            accountType = "LOCAL"
        )
    }
}
