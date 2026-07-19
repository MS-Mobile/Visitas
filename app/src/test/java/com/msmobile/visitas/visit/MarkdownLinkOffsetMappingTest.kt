package com.msmobile.visitas.visit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import junit.framework.TestCase.assertEquals
import org.junit.Test

class MarkdownLinkOffsetMappingTest {
    private val transformation = MarkdownLinkVisualTransformation(linkColor = Color.Blue)

    // Token occupies [3, 23); renders as "go Maps now" with the label at [3, 7)
    private val original = "go [Maps](https://m.co) now"

    @Test
    fun `renders label only with link style`() {
        val transformed = transformation.filter(AnnotatedString(original))

        assertEquals("go Maps now", transformed.text.text)
        val span = transformed.text.spanStyles.single()
        assertEquals(3, span.start)
        assertEquals(7, span.end)
        assertEquals(Color.Blue, span.item.color)
        assertEquals(TextDecoration.Underline, span.item.textDecoration)
    }

    @Test
    fun `text without links is returned unchanged`() {
        val transformed = transformation.filter(AnnotatedString("plain subject"))

        assertEquals("plain subject", transformed.text.text)
        assertEquals(5, transformed.offsetMapping.originalToTransformed(5))
        assertEquals(5, transformed.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun `original to transformed maps boundaries exactly and snaps interior to label end`() {
        val mapping = transformation.filter(AnnotatedString(original)).offsetMapping

        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(3, mapping.originalToTransformed(3)) // token start -> ts
        assertEquals(7, mapping.originalToTransformed(4)) // interior snaps to te
        assertEquals(7, mapping.originalToTransformed(22)) // interior snaps to te
        assertEquals(7, mapping.originalToTransformed(23)) // token end -> te
        assertEquals(11, mapping.originalToTransformed(27)) // text end -> text end
    }

    @Test
    fun `transformed to original maps boundaries exactly and interior into token`() {
        val mapping = transformation.filter(AnnotatedString(original)).offsetMapping

        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(3, mapping.transformedToOriginal(3)) // ts -> token start
        assertEquals(6, mapping.transformedToOriginal(5)) // interior -> start + 1 + (t - ts)
        assertEquals(23, mapping.transformedToOriginal(7)) // te -> token end
        assertEquals(27, mapping.transformedToOriginal(11)) // text end -> text end
    }

    @Test
    fun `maps offsets across multiple tokens`() {
        // "[a](x) and [b](y)" (17 chars) renders as "a and b" (7 chars)
        val mapping = transformation.filter(AnnotatedString("[a](x) and [b](y)")).offsetMapping

        assertEquals(1, mapping.originalToTransformed(6)) // after first token
        assertEquals(6, mapping.originalToTransformed(11)) // second token start
        assertEquals(7, mapping.originalToTransformed(17)) // text end
        assertEquals(6, mapping.transformedToOriginal(1)) // after first token label
        assertEquals(11, mapping.transformedToOriginal(6)) // second ts
        assertEquals(17, mapping.transformedToOriginal(7)) // text end
    }
}
