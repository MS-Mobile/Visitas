package com.msmobile.visitas.visit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class MarkdownLinkEditInterceptorTest {
    // Token occupies [3, 23); label renders as "Maps"
    private val subject = "go [Maps](https://m.co) now"

    @Test
    fun `backspace at token end deletes the whole token`() {
        // Caret sat right after the token; IME deleted the closing paren at 22
        val previous = TextFieldValue(subject, selection = TextRange(23))
        val proposed = TextFieldValue("go [Maps](https://m.co now", selection = TextRange(22))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals("go  now", result.value.text)
        assertEquals(TextRange(3), result.value.selection)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `backspace outside a token passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(27))
        val proposed = TextFieldValue("go [Maps](https://m.co) no", selection = TextRange(26))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `typing passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue("Xgo [Maps](https://m.co) now", selection = TextRange(1))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `caret landing inside token is a tap - returns url and snaps caret to token end`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue(subject, selection = TextRange(6))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals("https://m.co", result.clickedUrl)
        assertEquals(TextRange(23), result.value.selection)
        assertEquals(subject, result.value.text)
    }

    @Test
    fun `caret moving outside tokens passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue(subject, selection = TextRange(25))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `unchanged selection does not retrigger a tap`() {
        val previous = TextFieldValue(subject, selection = TextRange(6))
        val proposed = TextFieldValue(subject, selection = TextRange(6))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `range delete clipping a token passes through`() {
        // User selected [0, 10) and deleted: multi-char removal, not a backspace
        val previous = TextFieldValue(subject, selection = TextRange(0, 10))
        val proposed = TextFieldValue("https://m.co) now", selection = TextRange(0))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `text without links passes through`() {
        val previous = TextFieldValue("plain", selection = TextRange(5))
        val proposed = TextFieldValue("plai", selection = TextRange(4))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }
}
