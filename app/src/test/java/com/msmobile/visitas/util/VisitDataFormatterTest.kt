package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

class VisitDataFormatterTest {
    private val formatter = VisitDataFormatter(LocaleProvider())

    @Test
    fun `formats label and url as a markdown link`() {
        assertEquals(
            "[Maps](https://m.co)",
            formatter.formatAsMarkdownHyperlink(text = "Maps", url = "https://m.co")
        )
    }

    @Test
    fun `strips brackets from the label so the token stays parseable`() {
        assertEquals(
            "[Q1](https://m.co)",
            formatter.formatAsMarkdownHyperlink(text = "[Q1]", url = "https://m.co")
        )
    }

    @Test
    fun `blank label falls back to the url`() {
        assertEquals(
            "[https://m.co](https://m.co)",
            formatter.formatAsMarkdownHyperlink(text = "  ", url = "https://m.co")
        )
    }

    @Test
    fun `encodes url characters that would terminate the token early`() {
        assertEquals(
            "[Q](https://m.co/a%29b%20c)",
            formatter.formatAsMarkdownHyperlink(text = "Q", url = "https://m.co/a)b c")
        )
    }
}
