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

    @Test
    fun `sanitize passes through an edit fully inside the question`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(1, 2) // delete "Q"

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 1, links = links
        )

        assertEquals(newText, result)
        assertEquals(1, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting the opening parenthesis`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(0, 1) // delete "("

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting the closing parenthesis`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val closingParenIndex = oldText.indexOf(')')
        val newText = oldText.removeRange(closingParenIndex, closingParenIndex + 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = closingParenIndex, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting inside the url`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val urlCharIndex = oldText.indexOf("https") + 1
        val newText = oldText.removeRange(urlCharIndex, urlCharIndex + 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = urlCharIndex, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when inserting inside the url`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val urlCharIndex = oldText.indexOf("https") + 1
        val newText = oldText.substring(0, urlCharIndex) + "X" + oldText.substring(urlCharIndex)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = urlCharIndex + 1, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize passes through an insertion inside the question`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val insertIndex = oldText.indexOf(')')
        val newText = oldText.substring(0, insertIndex) + "!" + oldText.substring(insertIndex)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = insertIndex + 1, links = links
        )

        assertEquals(newText, result)
        assertEquals(insertIndex + 1, caret)
    }

    @Test
    fun `sanitize passes through an edit outside the span unaffected`() {
        val oldText = "prefix (Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(0, 1) // delete "p" from "prefix"

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = links
        )

        assertEquals(newText, result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize passes through when there are no links`() {
        val oldText = "Just plain text"
        val newText = oldText.removeRange(0, 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = emptyList()
        )

        assertEquals(newText, result)
        assertEquals(0, caret)
    }
}
