package com.msmobile.visitas.visit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

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

/**
 * Renders each `[label](url)` token as just its label, styled as a hyperlink.
 * The stored text is never modified; this is display-only.
 */
class MarkdownLinkVisualTransformation(private val linkColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val links = parseMarkdownLinks(text.text)
        if (links.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val transformed = buildAnnotatedString {
            var consumed = 0
            links.forEach { link ->
                append(text.text.substring(consumed, link.start))
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(link.label)
                }
                consumed = link.end
            }
            append(text.text.substring(consumed))
        }
        return TransformedText(transformed, MarkdownLinkOffsetMapping(links))
    }
}

/**
 * Piecewise mapping between stored markdown offsets and displayed label offsets.
 * Original interior offsets snap to the label end so the caret never renders inside
 * a link; transformed interior offsets map inside the original token on purpose —
 * [interceptMarkdownLinkEdit] reads that as a tap on the link and snaps the caret out.
 */
private class MarkdownLinkOffsetMapping(private val links: List<MarkdownLink>) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var removed = 0
        for (link in links) {
            val labelLength = link.label.length
            if (offset <= link.start) break
            if (offset < link.end) return link.start - removed + labelLength
            removed += (link.end - link.start) - labelLength
        }
        return offset - removed
    }

    override fun transformedToOriginal(offset: Int): Int {
        var removed = 0
        for (link in links) {
            val labelStart = link.start - removed
            val labelEnd = labelStart + link.label.length
            if (offset <= labelStart) break
            if (offset < labelEnd) return link.start + 1 + (offset - labelStart)
            removed += (link.end - link.start) - link.label.length
        }
        return offset + removed
    }
}

/** Result of [interceptMarkdownLinkEdit]: the value to apply and an optional tapped URL. */
data class MarkdownLinkEditResult(val value: TextFieldValue, val clickedUrl: String?)

/**
 * Gives markdown link tokens their atomic behavior. Call first thing in onValueChange:
 * - a single-character deletion inside a token becomes a whole-token deletion
 * - a caret landing strictly inside a token (only reachable by tapping the rendered
 *   label) is treated as a click: the URL is returned and the caret snaps to the end
 * Everything else passes through untouched.
 */
fun interceptMarkdownLinkEdit(
    previous: TextFieldValue,
    proposed: TextFieldValue
): MarkdownLinkEditResult {
    val links = parseMarkdownLinks(previous.text)
    if (links.isEmpty()) return MarkdownLinkEditResult(proposed, null)

    singleCharDeletionIndex(previous.text, proposed.text)?.let { index ->
        val link = links.firstOrNull { index >= it.start && index < it.end }
        if (link != null) {
            return MarkdownLinkEditResult(
                TextFieldValue(
                    text = previous.text.removeRange(link.start, link.end),
                    selection = TextRange(link.start)
                ),
                clickedUrl = null
            )
        }
    }

    if (proposed.text == previous.text &&
        proposed.selection.collapsed &&
        proposed.selection != previous.selection
    ) {
        val caret = proposed.selection.start
        val link = links.firstOrNull { caret > it.start && caret < it.end }
        if (link != null) {
            return MarkdownLinkEditResult(
                proposed.copy(selection = TextRange(link.end)),
                clickedUrl = link.url
            )
        }
    }

    return MarkdownLinkEditResult(proposed, null)
}

/** Index of the removed character when [proposed] is [previous] minus one char, else null. */
private fun singleCharDeletionIndex(previous: String, proposed: String): Int? {
    if (proposed.length != previous.length - 1) return null
    val index = previous.indices.firstOrNull { i ->
        i >= proposed.length || previous[i] != proposed[i]
    } ?: return null
    return if (previous.removeRange(index, index + 1) == proposed) index else null
}
