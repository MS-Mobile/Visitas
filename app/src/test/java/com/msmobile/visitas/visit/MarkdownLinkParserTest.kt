package com.msmobile.visitas.visit

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class MarkdownLinkParserTest {
    @Test
    fun `parses single link with surrounding text`() {
        val links = parseMarkdownLinks("call [John](https://example.com) today")

        assertEquals(1, links.size)
        assertEquals(5, links[0].start)
        assertEquals(32, links[0].end)
        assertEquals("John", links[0].label)
        assertEquals("https://example.com", links[0].url)
    }

    @Test
    fun `parses multiple links`() {
        val links = parseMarkdownLinks("[a](x) and [b](y)")

        assertEquals(2, links.size)
        assertEquals(0, links[0].start)
        assertEquals(6, links[0].end)
        assertEquals("a", links[0].label)
        assertEquals("x", links[0].url)
        assertEquals(11, links[1].start)
        assertEquals(17, links[1].end)
        assertEquals("b", links[1].label)
        assertEquals("y", links[1].url)
    }

    @Test
    fun `returns empty list for plain text`() {
        assertTrue(parseMarkdownLinks("just a plain subject").isEmpty())
    }

    @Test
    fun `ignores malformed syntax`() {
        assertTrue(parseMarkdownLinks("[a] (x)").isEmpty()) // space between ] and (
        assertTrue(parseMarkdownLinks("[a](x").isEmpty()) // missing closing paren
        assertTrue(parseMarkdownLinks("[](x)").isEmpty()) // empty label
        assertTrue(parseMarkdownLinks("[a]()").isEmpty()) // empty url
        assertTrue(parseMarkdownLinks("[a](x y)").isEmpty()) // whitespace in url
    }
}
