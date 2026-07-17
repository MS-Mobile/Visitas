package com.msmobile.visitas.util

data class FormattedLinkSpan(
    val start: Int,
    val end: Int,
    val questionStart: Int,
    val questionEnd: Int,
    val question: String,
    val url: String
)

private val FORMATTED_LINK_PATTERN = Regex("""\(([^()\[\]\n]*)\)\[([^\[\]\n]*)\]""")

fun findFormattedLinks(text: String, isValidUrl: (String) -> Boolean): List<FormattedLinkSpan> {
    return FORMATTED_LINK_PATTERN.findAll(text)
        .filter { match -> isValidUrl(match.groupValues[2]) }
        .map { match ->
            val questionRange = match.groups[1]!!.range
            FormattedLinkSpan(
                start = match.range.first,
                end = match.range.last + 1,
                questionStart = questionRange.first,
                questionEnd = questionRange.last + 1,
                question = match.groupValues[1],
                url = match.groupValues[2]
            )
        }
        .toList()
}
