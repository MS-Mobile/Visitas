package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class CalendarSelectionTest {

    @Test
    fun `resolvePreferred returns the preferred calendar when it is available`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK, LOCAL)

        val resolved = calendars.resolvePreferred(preferred = WORK.identity)

        assertEquals(WORK, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when the preferred one is not in the list`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)
        val gone = CalendarIdentity(
            accountType = "com.google",
            accountName = "someone@else.com",
            ownerAccount = "gone@group.calendar.google.com"
        )

        val resolved = calendars.resolvePreferred(preferred = gone)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when no preference is set`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)

        val resolved = calendars.resolvePreferred(preferred = null)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred returns null when no calendars are available`() {
        val resolved = emptyList<CalendarInfo>().resolvePreferred(preferred = WORK.identity)

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
    fun `orderedByAutoPickPreference keeps the order equally ranked calendars arrived in`() {
        val zulu = WORK.copy(id = 5L, displayName = "Zulu")
        val alpha = WORK.copy(id = 6L, displayName = "Alpha")
        val mike = WORK.copy(id = 7L, displayName = "Mike")
        val calendars = listOf(zulu, LOCAL, alpha, mike)

        val ordered = calendars.orderedByAutoPickPreference()

        // Stable sort: the three Google calendars keep their arrival order despite sorting
        // differently by name. This is what stops an upgrade from moving a user's events.
        assertEquals(listOf(zulu, alpha, mike, LOCAL), ordered)
    }

    @Test
    fun `orderedByAutoPickPreference returns an empty list unchanged`() {
        assertEquals(emptyList<CalendarInfo>(), emptyList<CalendarInfo>().orderedByAutoPickPreference())
    }

    @Test
    fun `resolvePreferred falls back to the receiver's first entry, not the best ranked one`() {
        val calendars = listOf(LOCAL, GOOGLE_PRIMARY)

        val resolved = calendars.resolvePreferred(preferred = null)

        // resolvePreferred trusts the caller's ordering rather than re-ranking; this pins that
        // contract, so slipping a sort in at a call site fails here instead of silently showing
        // one calendar while writing to another.
        assertEquals(LOCAL, resolved)
    }

    @Test
    fun `ordering then resolving with no preference picks the best ranked calendar`() {
        val calendars = listOf(LOCAL, WORK, GOOGLE_PRIMARY)

        val resolved = calendars.orderedByAutoPickPreference().resolvePreferred(preferred = null)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred distinguishes the same shared calendar subscribed under two accounts`() {
        // A real device had one public holiday calendar subscribed under two Google accounts:
        // same ownerAccount, different accountName. Identity must not collapse them.
        val holidaysOnPersonal = CalendarInfo(
            id = 10L,
            displayName = "Holidays",
            accountName = "user@gmail.com",
            ownerAccount = "pt.brazilian#holiday@group.v.calendar.google.com",
            accountType = "com.google",
            isPrimary = false
        )
        val holidaysOnWork = CalendarInfo(
            id = 16L,
            displayName = "Holidays",
            accountName = "user@work.com",
            ownerAccount = "pt.brazilian#holiday@group.v.calendar.google.com",
            accountType = "com.google",
            isPrimary = false
        )
        val calendars = listOf(holidaysOnPersonal, holidaysOnWork)

        val resolved = calendars.resolvePreferred(holidaysOnWork.identity)

        assertEquals(holidaysOnWork, resolved)
    }

    @Test
    fun `resolvePreferred matches an identity whose row id changed`() {
        // The point of identity: the provider reassigned the row id, but it is the same calendar.
        val reinstalled = WORK.copy(id = 999L)
        val calendars = listOf(GOOGLE_PRIMARY, reinstalled)

        val resolved = calendars.resolvePreferred(WORK.identity)

        assertEquals(reinstalled, resolved)
    }

    @Test
    fun `identity is null when the provider omits any part of it`() {
        assertNull(WORK.copy(accountName = null).identity)
        assertNull(WORK.copy(accountType = null).identity)
        assertNull(WORK.copy(ownerAccount = null).identity)
        assertNull(WORK.copy(ownerAccount = "  ").identity)
    }

    @Test
    fun `resolvePreferred with no preference ignores calendars that have no identity`() {
        val anonymous = LOCAL.copy(accountName = null)
        val calendars = listOf(GOOGLE_PRIMARY, anonymous)

        // Must return the first entry, NOT the identity-less one that a naive
        // `it.identity == preferred` would match when preferred is null.
        assertEquals(GOOGLE_PRIMARY, calendars.resolvePreferred(preferred = null))
    }

    private companion object {
        val GOOGLE_PRIMARY = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            ownerAccount = "user@gmail.com",
            accountType = "com.google",
            isPrimary = true
        )
        val WORK = CalendarInfo(
            id = 2L,
            displayName = "Work",
            accountName = "user@work.com",
            ownerAccount = "user@work.com",
            accountType = "com.google",
            isPrimary = false
        )
        val LOCAL = CalendarInfo(
            id = 3L,
            displayName = "Offline",
            accountName = "Local",
            ownerAccount = "local",
            accountType = "LOCAL",
            isPrimary = false
        )
        val LOCAL_PRIMARY = CalendarInfo(
            id = 4L,
            displayName = "Device",
            accountName = "Local",
            ownerAccount = "local.primary",
            accountType = "LOCAL",
            isPrimary = true
        )
    }
}
