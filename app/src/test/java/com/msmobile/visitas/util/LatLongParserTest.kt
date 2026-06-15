package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class LatLongParserTest {

    private val parser = LatLongParser()

    @Test
    fun `parses plain comma separated coordinates`() {
        val result = parser.parse("-28.6497173,-49.4233284")

        assertEquals(LatLong(-28.6497173, -49.4233284), result.getOrNull())
    }

    @Test
    fun `parses plain space separated coordinates`() {
        val result = parser.parse("40.7128 -74.0060")

        assertEquals(LatLong(40.7128, -74.0060), result.getOrNull())
    }

    @Test
    fun `parses coordinates with surrounding whitespace`() {
        val result = parser.parse("  12.34 , 56.78  ")

        assertEquals(LatLong(12.34, 56.78), result.getOrNull())
    }

    @Test
    fun `parses integer coordinates`() {
        val result = parser.parse("10,20")

        assertEquals(LatLong(10.0, 20.0), result.getOrNull())
    }

    @Test
    fun `parses coordinates from google maps at url`() {
        val url = "https://www.google.com/maps/@-28.6497173,-49.4233284,3a,88.1y," +
            "335.32h,79.47t/data=!3m4!1e1!3m2!1sub2dc7cB7URMJwA7UqDgaA!2e0" +
            "?utm_campaign=ml-ardl&g_ep=Eg1tbF8yMDI2MDYxMF8wIJvbDyoASAJQAQ%3D%3D"

        val result = parser.parse(url)

        assertEquals(LatLong(-28.6497173, -49.4233284), result.getOrNull())
    }

    @Test
    fun `parses coordinates from google maps query url`() {
        val url = "https://www.google.com/maps?q=-23.5505199,-46.6333094"

        val result = parser.parse(url)

        assertEquals(LatLong(-23.5505199, -46.6333094), result.getOrNull())
    }

    @Test
    fun `parses coordinates from place path url`() {
        val url = "https://www.google.com/maps/place/40.785091,-73.968285"

        val result = parser.parse(url)

        assertEquals(LatLong(40.785091, -73.968285), result.getOrNull())
    }

    @Test
    fun `fails when latitude is out of range`() {
        val result = parser.parse("100.0,20.0")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when longitude is out of range`() {
        val result = parser.parse("20.0,200.0")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when no coordinates are present`() {
        val result = parser.parse("https://www.google.com/maps/search/coffee")

        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `fails for arbitrary text`() {
        val result = parser.parse("Rua das Flores 123")

        assertTrue(result.isFailure)
    }
}
