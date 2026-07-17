package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class FormattedLinkTest {

    private val alwaysValid: (String) -> Boolean = { true }
    private val neverValid: (String) -> Boolean = { false }

    @Test
    fun `finds a single formatted link with a valid url`() {
        val text = "(What is God's Kingdom?)[https://example.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertEquals(1, links.size)
        val span = links.first()
        assertEquals(0, span.start)
        assertEquals(text.length, span.end)
        assertEquals("What is God's Kingdom?", span.question)
        assertEquals("https://example.com", span.url)
    }

    @Test
    fun `ignores a match whose url is not valid`() {
        val text = "(What is God's Kingdom?)[not a url]"

        val links = findFormattedLinks(text, neverValid)

        assertTrue(links.isEmpty())
    }

    @Test
    fun `finds multiple formatted links in the same text`() {
        val text = "(First)[https://a.com] and (Second)[https://b.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertEquals(2, links.size)
        assertEquals("First", links[0].question)
        assertEquals("Second", links[1].question)
    }

    @Test
    fun `does not match a link spanning multiple lines`() {
        val text = "(Question\n)[https://example.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertTrue(links.isEmpty())
    }
}
