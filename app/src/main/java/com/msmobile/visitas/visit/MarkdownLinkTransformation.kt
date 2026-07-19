package com.msmobile.visitas.visit

/**
 * A `[label](url)` markdown link found in stored subject text.
 * [start] is inclusive and [end] exclusive, both in stored-text offsets.
 */
data class MarkdownLink(val start: Int, val end: Int, val label: String, val url: String)

// Non-empty label without nested brackets; non-empty URL without whitespace,
// ending at the first closing paren. Anything malformed stays plain text.
private val MARKDOWN_LINK_REGEX = Regex("""\[([^\[\]]+)\]\(([^)\s]+)\)""")

fun parseMarkdownLinks(text: String): List<MarkdownLink> =
    MARKDOWN_LINK_REGEX.findAll(text).map { match ->
        MarkdownLink(
            start = match.range.first,
            end = match.range.last + 1,
            label = match.groupValues[1],
            url = match.groupValues[2]
        )
    }.toList()
