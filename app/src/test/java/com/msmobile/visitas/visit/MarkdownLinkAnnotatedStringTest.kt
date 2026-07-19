package com.msmobile.visitas.visit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class MarkdownLinkAnnotatedStringTest {
    private val linkColor = Color.Blue

    @Test
    fun `plain text is returned unchanged without annotations`() {
        val result = annotateMarkdownLinks("just a plain subject", linkColor) {}

        assertEquals("just a plain subject", result.text)
        assertTrue(result.getLinkAnnotations(0, result.length).isEmpty())
    }

    @Test
    fun `link token renders as styled label`() {
        val result =
            annotateMarkdownLinks("call [John](https://example.com) today", linkColor) {}

        assertEquals("call John today", result.text)

        val annotations = result.getLinkAnnotations(0, result.length)
        assertEquals(1, annotations.size)
        assertEquals(5, annotations[0].start)
        assertEquals(9, annotations[0].end)

        val annotation = annotations[0].item as LinkAnnotation.Clickable
        assertEquals("https://example.com", annotation.tag)
        val style = annotation.styles?.style
        assertEquals(linkColor, style?.color)
        assertEquals(TextDecoration.Underline, style?.textDecoration)
    }

    @Test
    fun `clicking the annotation invokes callback with url`() {
        var clickedUrl: String? = null
        val result = annotateMarkdownLinks("[Maps](https://maps.google.com)", linkColor) {
            clickedUrl = it
        }

        val annotation =
            result.getLinkAnnotations(0, result.length).single().item as LinkAnnotation.Clickable
        annotation.linkInteractionListener?.onClick(annotation)

        assertEquals("https://maps.google.com", clickedUrl)
    }

    @Test
    fun `multiple links keep surrounding text`() {
        val result = annotateMarkdownLinks("[a](x) and [b](y)!", linkColor) {}

        assertEquals("a and b!", result.text)

        val annotations = result.getLinkAnnotations(0, result.length)
        assertEquals(2, annotations.size)
        assertEquals(0, annotations[0].start)
        assertEquals(1, annotations[0].end)
        assertEquals("x", (annotations[0].item as LinkAnnotation.Clickable).tag)
        assertEquals(6, annotations[1].start)
        assertEquals(7, annotations[1].end)
        assertEquals("y", (annotations[1].item as LinkAnnotation.Clickable).tag)
    }

    @Test
    fun `malformed syntax stays plain text`() {
        val result = annotateMarkdownLinks("[a] (x)", linkColor) {}

        assertEquals("[a] (x)", result.text)
        assertTrue(result.getLinkAnnotations(0, result.length).isEmpty())
    }
}
