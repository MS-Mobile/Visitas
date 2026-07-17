package com.msmobile.visitas.util

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class UrlValidatorTest {

    private val validator = UrlValidator()

    @Test
    fun `accepts a valid https url`() {
        assertTrue(validator.isValid("https://www.jw.org/en/bible-teachings/kingdom/"))
    }

    @Test
    fun `accepts a valid http url`() {
        assertTrue(validator.isValid("http://example.com"))
    }

    @Test
    fun `rejects a url without a scheme`() {
        assertFalse(validator.isValid("www.example.com"))
    }

    @Test
    fun `rejects a non-http scheme`() {
        assertFalse(validator.isValid("ftp://example.com/file"))
    }

    @Test
    fun `rejects a blank value`() {
        assertFalse(validator.isValid("   "))
    }

    @Test
    fun `rejects an empty value`() {
        assertFalse(validator.isValid(""))
    }

    @Test
    fun `rejects a value containing whitespace`() {
        assertFalse(validator.isValid("https://example.com/some page"))
    }

    @Test
    fun `rejects a url with no host`() {
        assertFalse(validator.isValid("https://"))
    }

    @Test
    fun `rejects free text that is not a url`() {
        assertFalse(validator.isValid("What is God's Kingdom?"))
    }
}
