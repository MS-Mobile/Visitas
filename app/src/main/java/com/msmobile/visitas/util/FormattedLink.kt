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

fun sanitizeFormattedLinkEdit(
    oldText: String,
    newText: String,
    proposedCaretPosition: Int,
    links: List<FormattedLinkSpan>
): Pair<String, Int> {
    if (links.isEmpty() || oldText == newText) {
        return newText to proposedCaretPosition
    }

    val prefixLength = commonPrefixLength(oldText, newText)
    val suffixLength = commonSuffixLength(oldText, newText, prefixLength)
    val removeStart = prefixLength
    val removeEnd = oldText.length - suffixLength

    val collidingSpan = links.firstOrNull { span ->
        val overlapsSpan = removeStart < span.end && removeEnd > span.start
        val fullyWithinQuestion = removeStart >= span.questionStart && removeEnd <= span.questionEnd
        overlapsSpan && !fullyWithinQuestion
    }

    return if (collidingSpan != null) {
        oldText.removeRange(collidingSpan.start, collidingSpan.end) to collidingSpan.start
    } else {
        newText to proposedCaretPosition
    }
}

private fun commonPrefixLength(a: String, b: String): Int {
    val max = minOf(a.length, b.length)
    var length = 0
    while (length < max && a[length] == b[length]) length++
    return length
}

private fun commonSuffixLength(a: String, b: String, prefixLength: Int): Int {
    val max = minOf(a.length, b.length) - prefixLength
    var length = 0
    while (length < max && a[a.length - 1 - length] == b[b.length - 1 - length]) length++
    return length
}
