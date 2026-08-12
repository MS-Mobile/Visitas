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
    fun `resolvePreferred falls back to the first calendar when the preferred one is not in the list`() {
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

    @Test
    fun `orderedByAutoPickPreference puts a primary Google calendar first`() {
        val calendars = listOf(LOCAL, WORK, LOCAL_PRIMARY, GOOGLE_PRIMARY)

        val ordered = calendars.orderedByAutoPickPreference()

        assertEquals(listOf(GOOGLE_PRIMARY, WORK, LOCAL_PRIMARY, LOCAL), ordered)
    }

    @Test
    fun `orderedByAutoPickPreference prefers a secondary Google calendar over a local primary`() {
        val calendars = listOf(LOCAL_PRIMARY, WORK)

        val ordered = calendars.orderedByAutoPickPreference()

        assertEquals(listOf(WORK, LOCAL_PRIMARY), ordered)
    }

    @Test
    fun `orderedByAutoPickPreference keeps provider order between equally ranked calendars`() {
        val secondGoogleCalendar = WORK.copy(id = 5L, displayName = "Aaa first alphabetically")
        val calendars = listOf(WORK, secondGoogleCalendar)

        val ordered = calendars.orderedByAutoPickPreference()

        // Stable sort: WORK stays ahead despite sorting later by name. This is what stops an
        // upgrade from moving a user's events to a different calendar.
        assertEquals(listOf(WORK, secondGoogleCalendar), ordered)
    }

    @Test
    fun `orderedByAutoPickPreference returns an empty list unchanged`() {
        assertEquals(emptyList<CalendarInfo>(), emptyList<CalendarInfo>().orderedByAutoPickPreference())
    }

    @Test
    fun `resolvePreferred falls back to the receiver's first entry, not the best ranked one`() {
        val calendars = listOf(LOCAL, GOOGLE_PRIMARY)

        val resolved = calendars.resolvePreferred(preferredCalendarId = null)

        // resolvePreferred trusts the caller's ordering rather than re-ranking; this pins that
        // contract, so slipping a sort in at a call site fails here instead of silently showing
        // one calendar while writing to another.
        assertEquals(LOCAL, resolved)
    }

    private companion object {
        val GOOGLE_PRIMARY = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            accountType = "com.google",
            isPrimary = true
        )
        val WORK = CalendarInfo(
            id = 2L,
            displayName = "Work",
            accountName = "user@work.com",
            accountType = "com.google",
            isPrimary = false
        )
        val LOCAL = CalendarInfo(
            id = 3L,
            displayName = "Offline",
            accountName = "Local",
            accountType = "LOCAL",
            isPrimary = false
        )
        val LOCAL_PRIMARY = CalendarInfo(
            id = 4L,
            displayName = "Device",
            accountName = "Local",
            accountType = "LOCAL",
            isPrimary = true
        )
    }
}
