package com.msmobile.visitas.util

import com.msmobile.visitas.extension.toString
import com.msmobile.visitas.visit.VisitPreferredDay
import com.msmobile.visitas.visit.VisitPreferredTime
import java.time.LocalDateTime
import javax.inject.Inject

class VisitDataFormatter @Inject constructor(
    private val localeProvider: LocaleProvider
) {
    fun format(
        name: String,
        address: String,
        latitude: Double?,
        longitude: Double?,
        notes: String?,
        preferredDay: VisitPreferredDay,
        preferredTime: VisitPreferredTime,
        nextPendingVisitSubject: String?,
        nextPendingVisitDate: LocalDateTime?
    ): String {
        return buildString {
            appendLine(name)

            if (address.isNotEmpty()) {
                appendLine()
                appendLine(address)
            }

            if (latitude != null && longitude != null) {
                appendLine("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            }

            if (!notes.isNullOrBlank()) {
                appendLine()
                appendLine(notes)
            }

            if (preferredDay != VisitPreferredDay.ANY) {
                appendLine(preferredDay.name.lowercase().replaceFirstChar { it.uppercase() })
            }

            if (preferredTime != VisitPreferredTime.ANY) {
                appendLine(preferredTime.name.lowercase().replaceFirstChar { it.uppercase() })
            }

            if (nextPendingVisitSubject != null && nextPendingVisitDate != null) {
                appendLine()
                appendLine(nextPendingVisitSubject)
                appendLine(nextPendingVisitDate.toString(localeProvider.getLocale()))
            }
        }.trim()
    }

    /**
     * Builds a `[text](url)` markdown link the subject field can render: brackets are stripped
     * from the label (a blank label falls back to the url) and the characters that would
     * terminate the token early (spaces, closing parens) are percent-encoded in the url.
     */
    fun formatAsMarkdownHyperlink(text: String, url: String): String {
        val label = text.replace("[", "").replace("]", "").trim().ifEmpty { url }
        val safeUrl = url.replace(" ", "%20").replace(")", "%29")
        return "[$label]($safeUrl)"
    }
}

