package com.msmobile.visitas.util

import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

class UrlValidator @Inject constructor() {
    fun isValid(value: String): Boolean {
        if (value.isBlank() || value.any(Char::isWhitespace)) return false
        val uri = try {
            URI(value)
        } catch (e: URISyntaxException) {
            return false
        }
        return uri.isAbsolute && uri.scheme?.lowercase() in VALID_SCHEMES && !uri.host.isNullOrBlank()
    }

    private companion object {
        val VALID_SCHEMES = setOf("http", "https")
    }
}
