# Formatted Link Conversation Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render `(question)[url]` conversation-response links in the visit subject field as styled, atomically-editable text, backed by a real URL validator (replacing the `fooBar` stub).

**Architecture:** A DI-injectable `UrlValidator` (pure `java.net.URI` parsing) replaces the stub. A new pure-function utility, `FormattedLink.kt`, detects `(question)[url]` spans and enforces edit atomicity (question freely editable, `[url]` + brackets atomic) via a text-diff algorithm. The ViewModel calls it in `visitSubjectChanged` before committing state; the Compose screen calls the same span-detection function inside a `VisualTransformation` to render the styling, so detection logic exists in exactly one place.

**Tech Stack:** Kotlin, Jetpack Compose (Material3 `TextField` + `VisualTransformation`), Hilt DI, JUnit + Mockito-Kotlin unit tests, Android Studio Preview screenshot tests (`@PreviewTest`).

**Spec:** `docs/superpowers/specs/2026-07-17-formatted-link-conversation-design.md`

**Branch:** `feature/formatted-link-conversation` (already created, carries the spec-doc commit; work on top of it)

---

### Task 1: Fix the pre-existing compile break and add `UrlValidator`

The branch currently fails to compile: `VisitDetailViewModel.kt:1314` calls an undefined `fooBar(address)`, and `VisitDetailPreviewConfigProvider.kt:339` constructs a `ConversationState` missing the (already-added) `response` field. This task fixes both by introducing the real `UrlValidator` and wiring it in, restoring a green build before any further work.

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/util/UrlValidator.kt`
- Create: `app/src/test/java/com/msmobile/visitas/util/UrlValidatorTest.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt:339`
- Modify: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/msmobile/visitas/util/UrlValidatorTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the full test suite to confirm the pre-existing break**

Run: `./gradlew.bat testDebugUnitTest --console=plain`
Expected: FAILURE. The build fails at `:app:compileDebugKotlin` with two errors:
```
e: .../VisitDetailViewModel.kt:1314:16 Unresolved reference 'fooBar'.
```
(the new `UrlValidatorTest.kt` also won't compile yet since `UrlValidator` doesn't exist — this is expected, both are fixed below)

- [ ] **Step 3: Create `UrlValidator`**

Create `app/src/main/java/com/msmobile/visitas/util/UrlValidator.kt`:

```kotlin
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
```

- [ ] **Step 4: Wire `UrlValidator` into `VisitDetailViewModel`**

Modify `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`. Add the import after the existing `StringResource` import (around line 23):

```kotlin
import com.msmobile.visitas.util.StringResource
import com.msmobile.visitas.util.UrlValidator
import com.msmobile.visitas.util.VisitDataFormatter
```

Add the constructor parameter after `latLongParser` (around line 54):

```kotlin
    private val latLongParser: LatLongParser,
    private val urlValidator: UrlValidator,
    private val clipboardHandler: ClipboardHandler,
```

Replace the `isValidHttpAddress` body (currently lines 1313-1315):

```kotlin
    private fun isValidHttpAddress(address: String): Boolean {
        return urlValidator.isValid(address)
    }
```

- [ ] **Step 5: Fix the preview config compile error**

In `app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt`, `previewConversationSuggestion` (around line 339) is missing the `response` field added to `ConversationState`. Update it:

```kotlin
private val previewConversationSuggestion = VisitDetailViewModel.ConversationState(
    id = UUID.randomUUID(),
    question = previewReturnVisit.subject,
    response = "",
    questionAndResponse = "${previewReturnVisit.subject} - Lucas 1:31-33",
    show = true,
    conversationGroupId = UUID.randomUUID(),
    orderIndex = 0,
)
```

- [ ] **Step 6: Update the ViewModel test builder**

In `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`, add the import after `LatLongParser` (around line 15):

```kotlin
import com.msmobile.visitas.util.LatLongParser
import com.msmobile.visitas.util.UrlValidator
```

In `createViewModel()` (around line 1239), add a real `UrlValidator` instance next to `latLongParser` and pass it through:

```kotlin
        val latLongParser = mock<LatLongParser>()
        val urlValidator = UrlValidator()
        val clipboardHandler = mock<ClipboardHandler>()
```

```kotlin
        return VisitDetailViewModel(
            dispatchers = dispatchers,
            householderRepository = householderRepository,
            visitRepository = visitRepository,
            snapshotRepository = snapshotRepository,
            conversationRepository = conversationRepository,
            addressProvider = addressProvider,
            idProvider = idProvider,
            permissionChecker = permissionChecker,
            calendarEventManager = calendarEventManager,
            syncVisitCalendarEvent = syncVisitCalendarEvent,
            visitTimeValidator = visitTimeValidator,
            dateTimeProvider = dateTimeProvider,
            latLongParser = latLongParser,
            urlValidator = urlValidator,
            clipboardHandler = clipboardHandler,
            visitDataFormatter = visitDataFormatter
        )
```

A real instance is used (not a mock) so `ConversationSelected`/`VisitSubjectChanged` tests exercise real URL-validity behavior — matching how `visitDataFormatter` is already a real instance in this same builder.

- [ ] **Step 7: Run the full test suite to confirm the fix**

Run: `./gradlew.bat testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL, all existing tests plus the 9 new `UrlValidatorTest` cases pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/UrlValidator.kt \
        app/src/test/java/com/msmobile/visitas/util/UrlValidatorTest.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Add UrlValidator and wire it into VisitDetailViewModel

Replaces the fooBar stub used to detect valid HTTP(S) conversation
responses, and fixes the preview-provider compile break from the
in-progress ConversationState.response field."
```

---

### Task 2: `FormattedLink` — span detection

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt`
- Create: `app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt`:

```kotlin
package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class FormattedLinkTest {

    private val alwaysValid: (String) -> Boolean = { true }
    private val neverValid: (String) -> Boolean = { false }

    @Test
    fun `finds a single formatted link with a valid url`() {
        val text = "(What is God's Kingdom?)[https://example.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertEquals(1, links.size)
        val span = links.first()
        assertEquals(0, span.start)
        assertEquals(text.length, span.end)
        assertEquals("What is God's Kingdom?", span.question)
        assertEquals("https://example.com", span.url)
    }

    @Test
    fun `ignores a match whose url is not valid`() {
        val text = "(What is God's Kingdom?)[not a url]"

        val links = findFormattedLinks(text, neverValid)

        assertTrue(links.isEmpty())
    }

    @Test
    fun `finds multiple formatted links in the same text`() {
        val text = "(First)[https://a.com] and (Second)[https://b.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertEquals(2, links.size)
        assertEquals("First", links[0].question)
        assertEquals("Second", links[1].question)
    }

    @Test
    fun `does not match a link spanning multiple lines`() {
        val text = "(Question\n)[https://example.com]"

        val links = findFormattedLinks(text, alwaysValid)

        assertTrue(links.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.util.FormattedLinkTest" --console=plain`
Expected: FAIL with `Unresolved reference 'findFormattedLinks'` (compile error).

- [ ] **Step 3: Implement `findFormattedLinks`**

Create `app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt`:

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.util.FormattedLinkTest" --console=plain`
Expected: BUILD SUCCESSFUL, all 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt \
        app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt
git commit -m "Add findFormattedLinks span detection utility"
```

---

### Task 3: `FormattedLink` — atomic edit sanitization

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt`
- Modify: `app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt` (inside the `FormattedLinkTest` class, after the existing tests):

```kotlin
    @Test
    fun `sanitize passes through an edit fully inside the question`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(1, 2) // delete "Q"

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 1, links = links
        )

        assertEquals(newText, result)
        assertEquals(1, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting the opening parenthesis`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(0, 1) // delete "("

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting the closing parenthesis`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val closingParenIndex = oldText.indexOf(')')
        val newText = oldText.removeRange(closingParenIndex, closingParenIndex + 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = closingParenIndex, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when deleting inside the url`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val urlCharIndex = oldText.indexOf("https") + 1
        val newText = oldText.removeRange(urlCharIndex, urlCharIndex + 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = urlCharIndex, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize collapses the link when inserting inside the url`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val urlCharIndex = oldText.indexOf("https") + 1
        val newText = oldText.substring(0, urlCharIndex) + "X" + oldText.substring(urlCharIndex)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = urlCharIndex + 1, links = links
        )

        assertEquals("", result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize passes through an insertion inside the question`() {
        val oldText = "(Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val insertIndex = oldText.indexOf(')')
        val newText = oldText.substring(0, insertIndex) + "!" + oldText.substring(insertIndex)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = insertIndex + 1, links = links
        )

        assertEquals(newText, result)
        assertEquals(insertIndex + 1, caret)
    }

    @Test
    fun `sanitize passes through an edit outside the span unaffected`() {
        val oldText = "prefix (Question)[https://example.com]"
        val links = findFormattedLinks(oldText, alwaysValid)
        val newText = oldText.removeRange(0, 1) // delete "p" from "prefix"

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = links
        )

        assertEquals(newText, result)
        assertEquals(0, caret)
    }

    @Test
    fun `sanitize passes through when there are no links`() {
        val oldText = "Just plain text"
        val newText = oldText.removeRange(0, 1)

        val (result, caret) = sanitizeFormattedLinkEdit(
            oldText, newText, proposedCaretPosition = 0, links = emptyList()
        )

        assertEquals(newText, result)
        assertEquals(0, caret)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.util.FormattedLinkTest" --console=plain`
Expected: FAIL with `Unresolved reference 'sanitizeFormattedLinkEdit'` (compile error).

- [ ] **Step 3: Implement `sanitizeFormattedLinkEdit`**

Add to `app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt`, after `findFormattedLinks`:

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.util.FormattedLinkTest" --console=plain`
Expected: BUILD SUCCESSFUL, all 12 tests pass (4 from Task 2 + 8 new).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt \
        app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt
git commit -m "Add sanitizeFormattedLinkEdit for atomic link editing"
```

---

### Task 4: Wire atomic editing into `VisitDetailViewModel`

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt:598-628`
- Modify: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`, near the other `VisitSubjectChanged`/`ConversationSelected` tests (or at the end of the class body, before `createViewModel`):

```kotlin
    @Test
    fun `onEvent with ConversationSelected and a url response inserts a formatted link`() {
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))
        val visit = viewModel.uiState.value.visitList.first()
        val conversation = VisitDetailViewModel.ConversationState(
            id = FIRST_CONVERSATION_ID,
            question = "What is God's Kingdom?",
            response = "https://www.jw.org/en/bible-teachings/kingdom/",
            questionAndResponse = "unused",
            show = true,
            conversationGroupId = null,
            orderIndex = 0
        )

        viewModel.onEvent(
            VisitDetailViewModel.UiEvent.ConversationSelected(
                visit = visit,
                conversation = conversation,
                caretPosition = 0
            )
        )

        assertEquals(
            "(What is God's Kingdom?)[https://www.jw.org/en/bible-teachings/kingdom/]",
            viewModel.uiState.value.visitList.first().subject
        )
    }

    @Test
    fun `onEvent with VisitSubjectChanged removes the whole link when deleting its closing bracket`() {
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))
        val visit = viewModel.uiState.value.visitList.first()
        val conversation = VisitDetailViewModel.ConversationState(
            id = FIRST_CONVERSATION_ID,
            question = "What is God's Kingdom?",
            response = "https://www.jw.org/en/bible-teachings/kingdom/",
            questionAndResponse = "unused",
            show = true,
            conversationGroupId = null,
            orderIndex = 0
        )
        viewModel.onEvent(
            VisitDetailViewModel.UiEvent.ConversationSelected(visit, conversation, caretPosition = 0)
        )
        val linkedVisit = viewModel.uiState.value.visitList.first()
        val linkedSubject = linkedVisit.subject
        val closingParenIndex = linkedSubject.indexOf(')')
        val textWithParenDeleted = linkedSubject.removeRange(closingParenIndex, closingParenIndex + 1)

        viewModel.onEvent(
            VisitDetailViewModel.UiEvent.VisitSubjectChanged(
                visit = linkedVisit,
                value = textWithParenDeleted,
                caretPosition = closingParenIndex
            )
        )

        val finalVisit = viewModel.uiState.value.visitList.first()
        assertEquals("", finalVisit.subject)
        assertEquals(0, finalVisit.caretPosition)
    }

    @Test
    fun `onEvent with VisitSubjectChanged allows editing text inside the link question`() {
        val viewModel = createViewModel()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))
        val visit = viewModel.uiState.value.visitList.first()
        val conversation = VisitDetailViewModel.ConversationState(
            id = FIRST_CONVERSATION_ID,
            question = "What is God's Kingdom?",
            response = "https://www.jw.org/en/bible-teachings/kingdom/",
            questionAndResponse = "unused",
            show = true,
            conversationGroupId = null,
            orderIndex = 0
        )
        viewModel.onEvent(
            VisitDetailViewModel.UiEvent.ConversationSelected(visit, conversation, caretPosition = 0)
        )
        val linkedVisit = viewModel.uiState.value.visitList.first()
        val linkedSubject = linkedVisit.subject
        val editedSubject = linkedSubject.replaceFirst(
            "What is God's Kingdom?",
            "What is the Kingdom?"
        )

        viewModel.onEvent(
            VisitDetailViewModel.UiEvent.VisitSubjectChanged(
                visit = linkedVisit,
                value = editedSubject,
                caretPosition = editedSubject.indexOf(')')
            )
        )

        assertEquals(editedSubject, viewModel.uiState.value.visitList.first().subject)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest" --console=plain`
Expected: FAIL. The `ConversationSelected` test already passes (that logic is pre-existing in the diff), but the two `VisitSubjectChanged` atomicity tests fail because `visitSubjectChanged` doesn't sanitize yet — deleting `)` currently just removes one character instead of collapsing the whole span.

- [ ] **Step 3: Wire sanitization into `visitSubjectChanged`**

Add two new imports to `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`, next to the other `com.msmobile.visitas.util` imports (around lines 19-23), alphabetically before `StringResource`:

```kotlin
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.LatLongParser
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.StringResource
```

becomes:

```kotlin
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.LatLongParser
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.StringResource
import com.msmobile.visitas.util.findFormattedLinks
import com.msmobile.visitas.util.sanitizeFormattedLinkEdit
```

Replace `visitSubjectChanged` (currently lines 598-628):

```kotlin
    private fun visitSubjectChanged(value: String, visit: VisitState, caretPosition: Int) {
        newState {
            val links = findFormattedLinks(visit.subject, urlValidator::isValid)
            val (sanitizedValue, sanitizedCaretPosition) = sanitizeFormattedLinkEdit(
                oldText = visit.subject,
                newText = value,
                proposedCaretPosition = caretPosition,
                links = links
            )
            val lines = sanitizedValue.split('\n')
            val lineIndex = lines.getLineIndex(sanitizedCaretPosition)
            val lineValue = lines.elementAtOrNull(lineIndex) ?: return@newState this
            val filteredConversationList = conversationList.filterBy(lineValue)
            val isConversionListExpanded = filteredConversationList.any { conversation ->
                conversation.show
            }
            val showClearSubject = sanitizedValue.isNotEmpty()
            val hasNextConversationSuggestion = visit.nextConversationSuggestion != null
            val showNextVisitSuggestion = hasNextConversationSuggestion && !showClearSubject
            val updatedList = visitList.toMutableList().apply {
                set(
                    this@apply.indexOfById(visit),
                    visit.copy(
                        editable = visit.editable.copy(subject = sanitizedValue),
                        isConversationListExpanded = isConversionListExpanded,
                        showClearSubject = showClearSubject,
                        showNextVisitSuggestion = showNextVisitSuggestion,
                        caretPosition = sanitizedCaretPosition
                    )
                )
            }
            copy(
                visitList = updatedList,
                conversationList = filteredConversationList,
                eventState = UiEventState.Idle,
            )
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest" --console=plain`
Expected: BUILD SUCCESSFUL, all tests pass including the 3 new ones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Enforce atomic link editing in visitSubjectChanged"
```

---

### Task 5: Render styled links in the subject field

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt:936-1049`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt`

- [ ] **Step 1: Add the required imports**

In `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt`, add after `import androidx.compose.ui.graphics.*` region — there's currently no `androidx.compose.ui.graphics.Color` import, add it near the top of the `androidx.compose.ui` import block (alphabetically, before `androidx.compose.ui.res` or similar — exact position doesn't matter for compilation, group it with other `androidx.compose.ui.*` imports):

```kotlin
import androidx.compose.ui.graphics.Color
```

Add these next to the existing `androidx.compose.ui.text.*` imports (around line 83-88, which currently read `TextRange`, `input.KeyboardCapitalization`, `input.KeyboardType`, `input.TextFieldValue`, `rememberTextMeasurer`, `style.TextOverflow`):

```kotlin
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
```

Add these next to the other `com.msmobile.visitas.util.*` imports (around lines 120-124, after `DetailScreenStyle` and before `borderPadding`):

```kotlin
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.UrlValidator
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.findFormattedLinks
import com.msmobile.visitas.util.floatingBarBottomPadding
```

- [ ] **Step 2: Add the `VisualTransformation` class**

In `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt`, add this private class immediately before `VisitSubjectDropdownList` (currently starting at line 936):

```kotlin
private class FormattedLinkVisualTransformation(
    private val linkColor: Color,
    private val isValidUrl: (String) -> Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val links = findFormattedLinks(text.text, isValidUrl)
        if (links.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val styled = buildAnnotatedString {
            append(text)
            links.forEach { span ->
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    span.start,
                    span.end
                )
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
```

- [ ] **Step 3: Apply it to the subject `TextField`**

In `VisitSubjectDropdownList` (currently lines 936-1049), add right after the existing `LaunchedEffect(visit.subject)` block and before `Column(modifier = modifier) {` (currently around line 962-964):

```kotlin
    val linkColor = MaterialTheme.colorScheme.primary
    val urlValidator = remember { UrlValidator() }
    val subjectVisualTransformation = remember(linkColor) {
        FormattedLinkVisualTransformation(linkColor, urlValidator::isValid)
    }

    Column(modifier = modifier) {
```

Add the `visualTransformation` parameter to the `TextField` call (currently lines 965-1013), right after `value = textFieldValueState,`:

```kotlin
        TextField(
            modifier = modifier.onFocusChanged { focusState ->
                onEvent(
                    VisitDetailViewModel.UiEvent.VisitSubjectFocusChanged(
                        focusState.hasFocus,
                        visit
                    )
                )
            },
            value = textFieldValueState,
            visualTransformation = subjectVisualTransformation,
            shape = MaterialTheme.shapes.textField.removeBottomCorner(),
```

- [ ] **Step 4: Add a screenshot-test preview config for the styled link**

In `app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt`, add a new entry to the `values` sequence, after the `"Multiple Visits"` config (around line 90):

```kotlin
        VisitDetailPreviewConfig(
            configName = "Formatted Link",
            mainActivityUiState = previewMainActivityUiState,
            householderId = UUID.randomUUID(),
            uiState = previewVisitDetailUiState.copy(
                visitList = listOf(
                    previewFirstVisitUiState.copy(
                        editable = previewFirstVisitUiState.editable.copy(
                            subject = "(What is God's Kingdom?)" +
                                "[https://www.jw.org/en/bible-teachings/kingdom/]"
                        ),
                        canBeRemoved = false
                    )
                )
            ),
            isDarkMode = false
        ),
```

- [ ] **Step 5: Build and run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL, all unit tests still pass (this task only touches Compose UI code and preview data, not logic covered by unit tests).

Run: `./gradlew.bat compileDebugKotlin --console=plain`
Expected: BUILD SUCCESSFUL — confirms the Compose changes compile.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt
git commit -m "Render formatted conversation links as styled text in the subject field"
```

---

### Task 6: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL, 0 failures.

- [ ] **Step 2: Run a full debug build**

Run: `./gradlew.bat assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update screenshot test golden images**

Run: `./gradlew.bat updateDebugScreenshotTest --console=plain`
Expected: BUILD SUCCESSFUL — regenerates reference PNGs including the new "Formatted Link" preview config added in Task 5. Review the generated image for `VisitDetailScreenPreviewTest` (Formatted Link) under `app/src/screenshotTest/reference/...` to confirm the link renders underlined/colored.

- [ ] **Step 4: Review the diff**

Run: `git status` and `git diff --stat`
Expected: Only the files listed across Tasks 1-5 plus new/updated screenshot reference images are modified. No unrelated files.

- [ ] **Step 5: Commit any updated screenshot references**

```bash
git add app/src/screenshotTest/reference
git commit -m "Update screenshot references for formatted link rendering"
```

(Skip this step if `git status` shows no changes under `app/src/screenshotTest/reference`.)

---

### Task 7: Open the pull request

**Files:** none (git/GitHub operations only)

- [ ] **Step 1: Push the branch**

```bash
git push -u origin feature/formatted-link-conversation
```

- [ ] **Step 2: Create the PR using the repo template**

```bash
gh pr create --title "Render formatted conversation links as clickable-styled text" --body "$(cat <<'EOF'
## 📝 Description
Replaces the `fooBar` stub with a real `UrlValidator` and adds atomic, styled rendering
for `(question)[url]` conversation-response links inserted into the visit subject field:
- `UrlValidator` (DI-injectable, `java.net.URI`-based) validates HTTP(S) responses.
- `FormattedLink.kt` detects `(question)[url]` spans and enforces edit atomicity — the
  question text stays freely editable, while the `[url]` + surrounding brackets can only
  be removed as a whole (deleting into them removes the entire link).
- The subject `TextField` renders detected links as colored/underlined text via a
  `VisualTransformation`, visible in `@PreviewTest` screenshot captures.

## 🐞 Type of Change

- [ ] Bug fix
- [x] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Refactoring
- [ ] CI/CD
- [ ] Other (please describe):

## ☑️ Checklist

- [x] Code compiles without errors
- [x] Tests pass locally
- [ ] UI changes tested on device/emulator
- [x] Follows existing code patterns and conventions
EOF
)"
```

- [ ] **Step 3: Report the PR URL to the user**

`gh pr create` prints the PR URL on success — relay it back to the user.
