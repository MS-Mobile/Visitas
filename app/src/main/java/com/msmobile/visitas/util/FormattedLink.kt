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

/**
 * Detects `(question)[url]` spans in [text]. [isValidUrl] gates which matches count
 * as links and defaults to accepting any well-formed match: the view model only ever
 * inserts this format after validating the URL, so the format itself is the contract
 * and the rendering layer does not need to re-validate.
 */
fun findFormattedLinks(
    text: String,
    isValidUrl: (String) -> Boolean = { true }
): List<FormattedLinkSpan> {
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

/**
 * A contiguous slice of the original subject text that survives collapsing. An
 * [isLink] segment is the question text of a formatted link (rendered styled);
 * any other segment is plain, unchanged text. The surrounding "(", ")" and
 * "[url]" of each link are dropped, so they never appear as a segment.
 */
data class RenderedSegment(
    val originalStart: Int,
    val originalEnd: Int,
    val isLink: Boolean
)

/**
 * Splits [text] into the ordered segments that should remain visible once each
 * `(question)[url]` formatted link collapses down to just its styled question.
 * [links] must be the spans returned by [findFormattedLinks] for the same text,
 * ordered by position and non-overlapping.
 */
fun collapseFormattedLinks(text: String, links: List<FormattedLinkSpan>): List<RenderedSegment> {
    if (links.isEmpty()) {
        return if (text.isEmpty()) emptyList() else listOf(RenderedSegment(0, text.length, isLink = false))
    }
    val segments = mutableListOf<RenderedSegment>()
    var cursor = 0
    links.forEach { link ->
        if (link.start > cursor) {
            segments.add(RenderedSegment(cursor, link.start, isLink = false))
        }
        if (link.questionEnd > link.questionStart) {
            segments.add(RenderedSegment(link.questionStart, link.questionEnd, isLink = true))
        }
        cursor = link.end
    }
    if (cursor < text.length) {
        segments.add(RenderedSegment(cursor, text.length, isLink = false))
    }
    return segments
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
