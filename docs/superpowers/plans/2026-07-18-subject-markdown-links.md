# Visit Subject Markdown Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render `[text](url)` markdown links in the visit subject field as clickable, atomic (backspace deletes whole token), non-editable hyperlinks — visual-only, stored text stays raw markdown.

**Architecture:** A `VisualTransformation` on the existing Material3 `TextField` hides the markdown syntax and styles the label as a link; a pure `onValueChange` interceptor expands single-char deletions inside a token to full-token deletes and treats a caret landing inside a token (tap) as a click that fires `Context.launchUrl`. No ViewModel, storage, or field-migration changes.

**Tech Stack:** Kotlin, Jetpack Compose (BTF1 `TextFieldValue` API: `VisualTransformation`, `OffsetMapping`), JUnit4 with `junit.framework.TestCase` assertions (project convention).

**Spec:** `docs/superpowers/specs/2026-07-18-subject-markdown-links-design.md`

## File Structure

- Create: `app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt` — parser (`MarkdownLink`, `parseMarkdownLinks`), `MarkdownLinkVisualTransformation` + `MarkdownLinkOffsetMapping`, edit interceptor (`interceptMarkdownLinkEdit`, `MarkdownLinkEditResult`). One file: these three units share the parser types and change together.
- Modify: `app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt` — add `Context.launchUrl`.
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt` (`VisitSubjectDropdownList`, ~line 964-1013) — wire transformation + interceptor.
- Test: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkParserTest.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkOffsetMappingTest.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkEditInterceptorTest.kt`

Offset vocabulary used throughout: a token occupies `[start, end)` in the **original** (stored) text and its label occupies `[ts, te)` in the **transformed** (displayed) text. `end` is exclusive.

---

### Task 1: Markdown link parser

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt`
- Create: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkParserTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.msmobile.visitas.visit

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class MarkdownLinkParserTest {
    @Test
    fun `parses single link with surrounding text`() {
        val links = parseMarkdownLinks("call [John](https://example.com) today")

        assertEquals(1, links.size)
        assertEquals(5, links[0].start)
        assertEquals(32, links[0].end)
        assertEquals("John", links[0].label)
        assertEquals("https://example.com", links[0].url)
    }

    @Test
    fun `parses multiple links`() {
        val links = parseMarkdownLinks("[a](x) and [b](y)")

        assertEquals(2, links.size)
        assertEquals(0, links[0].start)
        assertEquals(6, links[0].end)
        assertEquals("a", links[0].label)
        assertEquals("x", links[0].url)
        assertEquals(11, links[1].start)
        assertEquals(17, links[1].end)
        assertEquals("b", links[1].label)
        assertEquals("y", links[1].url)
    }

    @Test
    fun `returns empty list for plain text`() {
        assertTrue(parseMarkdownLinks("just a plain subject").isEmpty())
    }

    @Test
    fun `ignores malformed syntax`() {
        assertTrue(parseMarkdownLinks("[a] (x)").isEmpty()) // space between ] and (
        assertTrue(parseMarkdownLinks("[a](x").isEmpty()) // missing closing paren
        assertTrue(parseMarkdownLinks("[](x)").isEmpty()) // empty label
        assertTrue(parseMarkdownLinks("[a]()").isEmpty()) // empty url
        assertTrue(parseMarkdownLinks("[a](x y)").isEmpty()) // whitespace in url
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkParserTest"`
Expected: compilation FAILS with unresolved reference `parseMarkdownLinks`

- [ ] **Step 3: Write the parser**

Create `MarkdownLinkTransformation.kt` with:

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkParserTest"`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkParserTest.kt
git commit -m "Add markdown link parser for visit subject"
```

---

### Task 2: Visual transformation with offset mapping

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt`
- Create: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkOffsetMappingTest.kt`

Reference example used by all tests: original `"go [Maps](https://m.co) now"` (length 27). Token: start=3, end=23. Transformed: `"go Maps now"` (length 11), label at ts=3, te=7.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.msmobile.visitas.visit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import junit.framework.TestCase.assertEquals
import org.junit.Test

class MarkdownLinkOffsetMappingTest {
    private val transformation = MarkdownLinkVisualTransformation(linkColor = Color.Blue)
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkOffsetMappingTest"`
Expected: compilation FAILS with unresolved reference `MarkdownLinkVisualTransformation`

- [ ] **Step 3: Implement transformation and mapping**

Append to `MarkdownLinkTransformation.kt` (add the new imports to the file header):

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
```

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkOffsetMappingTest"`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkOffsetMappingTest.kt
git commit -m "Add visual transformation rendering markdown links in subject"
```

---

### Task 3: Edit interceptor (atomic backspace + tap detection)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt`
- Create: `app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkEditInterceptorTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.msmobile.visitas.visit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class MarkdownLinkEditInterceptorTest {
    // Token occupies [3, 23); label renders as "Maps"
    private val subject = "go [Maps](https://m.co) now"

    @Test
    fun `backspace at token end deletes the whole token`() {
        // Caret sat right after the token; IME deleted the closing paren at 22
        val previous = TextFieldValue(subject, selection = TextRange(23))
        val proposed = TextFieldValue("go [Maps](https://m.co now", selection = TextRange(22))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals("go  now", result.value.text)
        assertEquals(TextRange(3), result.value.selection)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `backspace outside a token passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(27))
        val proposed = TextFieldValue("go [Maps](https://m.co) no", selection = TextRange(26))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `typing passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue("Xgo [Maps](https://m.co) now", selection = TextRange(1))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `caret landing inside token is a tap - returns url and snaps caret to token end`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue(subject, selection = TextRange(6))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals("https://m.co", result.clickedUrl)
        assertEquals(TextRange(23), result.value.selection)
        assertEquals(subject, result.value.text)
    }

    @Test
    fun `caret moving outside tokens passes through`() {
        val previous = TextFieldValue(subject, selection = TextRange(0))
        val proposed = TextFieldValue(subject, selection = TextRange(25))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `unchanged selection does not retrigger a tap`() {
        val previous = TextFieldValue(subject, selection = TextRange(6))
        val proposed = TextFieldValue(subject, selection = TextRange(6))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `range delete clipping a token passes through`() {
        // User selected [0, 10) and deleted: multi-char removal, not a backspace
        val previous = TextFieldValue(subject, selection = TextRange(0, 10))
        val proposed = TextFieldValue("https://m.co) now", selection = TextRange(0))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }

    @Test
    fun `text without links passes through`() {
        val previous = TextFieldValue("plain", selection = TextRange(5))
        val proposed = TextFieldValue("plai", selection = TextRange(4))

        val result = interceptMarkdownLinkEdit(previous, proposed)

        assertEquals(proposed, result.value)
        assertNull(result.clickedUrl)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkEditInterceptorTest"`
Expected: compilation FAILS with unresolved reference `interceptMarkdownLinkEdit`

- [ ] **Step 3: Implement the interceptor**

Append to `MarkdownLinkTransformation.kt` (add the two new imports):

```kotlin
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
```

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.MarkdownLinkEditInterceptorTest"`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt app/src/test/java/com/msmobile/visitas/visit/MarkdownLinkEditInterceptorTest.kt
git commit -m "Add edit interceptor for atomic markdown link backspace and tap"
```

---

### Task 4: Context.launchUrl extension

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt`

No unit test: the function is two lines of Android framework calls (`Uri`, `Intent`) and the project has no Robolectric setup; existing launch extensions in this file are likewise untested. Covered by manual verification in Task 5.

- [ ] **Step 1: Add the extension**

Insert before the existing `private fun Context.startActivitySafely` at the bottom of `ContextExtension.kt`:

```kotlin
/** Opens [url] with the default handler (browser). Scheme-less URLs get https:// prefixed. */
fun Context.launchUrl(url: String) {
    val uri = Uri.parse(url).let { parsed ->
        if (parsed.scheme == null) Uri.parse("https://$url") else parsed
    }
    startActivitySafely(Intent(Intent.ACTION_VIEW, uri))
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt
git commit -m "Add Context.launchUrl extension"
```

---

### Task 5: Wire into the subject field

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt` (`VisitSubjectDropdownList`, ~lines 964-1013)

- [ ] **Step 1: Add imports**

In the import block of `VisitDetailScreen.kt`, add (alphabetically among the existing `com.msmobile.visitas.extension.*` imports):

```kotlin
import com.msmobile.visitas.extension.launchUrl
```

(`LocalContext`, `MaterialTheme`, and `remember` are already imported.)

- [ ] **Step 2: Create context, link color, and transformation in the composable**

In `VisitSubjectDropdownList`, immediately before `Column(modifier = modifier) {` (currently line 964), add:

```kotlin
val context = LocalContext.current
val linkColor = MaterialTheme.colorScheme.primary
val markdownLinkTransformation = remember(linkColor) {
    MarkdownLinkVisualTransformation(linkColor)
}
```

- [ ] **Step 3: Apply the transformation and interceptor to the TextField**

On the subject `TextField`, add the `visualTransformation` parameter after `colors = EditableTextFieldColors,` and replace the existing `onValueChange` block:

```kotlin
            colors = EditableTextFieldColors,
            visualTransformation = markdownLinkTransformation,
            onValueChange = { value ->
                val result = interceptMarkdownLinkEdit(textFieldValueState, value)

                // Update local state immediately for smooth typing
                textFieldValueState = result.value

                result.clickedUrl?.let { url -> context.launchUrl(url) }

                // Send to ViewModel
                onEvent(
                    VisitDetailViewModel.UiEvent.VisitSubjectChanged(
                        visit = visit,
                        value = result.value.text,
                        caretPosition = result.value.selection.start
                    )
                )
            })
```

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, no failures (existing + 3 new test classes)

- [ ] **Step 5: Manual verification on device/emulator**

Install and open a visit detail:
1. Type `see [Maps](https://maps.google.com) info` in the subject — field shows `see Maps info` with "Maps" underlined in primary color.
2. Tap "Maps" — browser/Maps opens via `launchUrl`.
3. Place caret after "Maps" (tap just right of it) and press backspace — the whole link disappears; stored subject (reopen the visit) shows plain text without the markdown token.
4. Type plain text before/after the link — behaves normally, caret math correct.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt
git commit -m "Render markdown links in visit subject field"
```
